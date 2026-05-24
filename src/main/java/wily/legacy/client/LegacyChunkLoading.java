package wily.legacy.client;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyChunkLoading {
    private static final int STEP_MILLIS = 75;
    private static final int FEATURE_DELAY_MILLIS = 900;
    private static final int SNOW_DELAY_MILLIS = 1200;
    private static final int BASE_SECTIONS_PER_STEP = 2;
    private static final int MID_SECTIONS_PER_STEP = 3;
    private static final int MAX_SECTIONS_PER_STEP = 4;
    private static final int MID_BACKLOG = 96;
    private static final int HIGH_BACKLOG = 256;
    private static final int MAX_CATCH_UP_STEPS = 4;
    private static final int IMMEDIATE_DISTANCE = 20;
    private static final int EXTRA_TRIM_DISTANCE = 6;
    private static final LongOpenHashSet revealed = new LongOpenHashSet();
    private static final LongOpenHashSet pending = new LongOpenHashSet();
    private static final Long2LongOpenHashMap featureReadyAt = new Long2LongOpenHashMap();
    private static final LongOpenHashSet hiddenFeatureSections = new LongOpenHashSet();
    private static final Map<BlockStateModel, BlockStateModel> hiddenFeatureModels = new ConcurrentHashMap<>();
    private static ClientLevel level;
    private static long lastStep = Long.MIN_VALUE;

    private LegacyChunkLoading() {
    }

    public static synchronized boolean filter(List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (!LegacyOptions.slowChunkLoading.get() || minecraft.level == null || cameraEntity == null) {
            reset();
            return false;
        }

        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }

        Vec3 center = cameraEntity.position();
        long centerSection = SectionPos.asLong(cameraEntity.blockPosition());

        collect(visible, center, centerSection);
        collect(nearby, center, centerSection);

        long step = Util.getMillis() / STEP_MILLIS;
        if (step != lastStep) {
            int steps = stepCount(step);
            lastStep = step;
            trim(centerSection, minecraft.options.renderDistance().get() + EXTRA_TRIM_DISTANCE);
            reveal(centerSection, steps);
        }

        refreshFeatures(minecraft);
        visible.removeIf(section -> !isRevealed(section));
        nearby.removeIf(section -> !isRevealed(section));
        return !pending.isEmpty() || !featureReadyAt.isEmpty();
    }

    public static synchronized boolean filterSection(long key, int originX, int originY, int originZ, double cameraX, double cameraY, double cameraZ) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (!LegacyOptions.slowChunkLoading.get() || minecraft.level == null || cameraEntity == null) {
            reset();
            return true;
        }

        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }

        collect(key, originX, originY, originZ, cameraX, cameraY, cameraZ);

        long step = Util.getMillis() / STEP_MILLIS;
        if (step != lastStep) {
            int steps = stepCount(step);
            lastStep = step;
            long centerSection = SectionPos.asLong(cameraEntity.blockPosition());
            trim(centerSection, minecraft.options.renderDistance().get() + EXTRA_TRIM_DISTANCE);
            reveal(centerSection, steps);
        }

        refreshFeatures(minecraft);
        return revealed.contains(key);
    }

    public static synchronized BlockStateModel getFeatureModel(BlockPos pos, BlockState state, BlockStateModel model) {
        if (!LegacyOptions.slowChunkLoading.get() || !isFeatureState(state)) {
            return model;
        }

        long section = SectionPos.asLong(pos);
        long readyAt = featureReadyAt.get(section);
        if (readyAt == 0) {
            return model;
        }

        if (Util.getMillis() >= readyAt) {
            finishFeatureDelay(section);
            return model;
        }

        hiddenFeatureSections.add(section);
        return hiddenFeatureModels.computeIfAbsent(model, HiddenFeatureModel::new);
    }

    public static synchronized BlockState getFeatureState(BlockPos pos, BlockState state) {
        if (!LegacyOptions.slowChunkLoading.get() || !isFeatureState(state)) {
            return state;
        }

        long section = SectionPos.asLong(pos);
        long readyAt = featureReadyAt.get(section);
        if (readyAt == 0) {
            return state;
        }

        if (Util.getMillis() >= readyAt) {
            finishFeatureDelay(section);
            return state;
        }

        hiddenFeatureSections.add(section);
        return Blocks.AIR.defaultBlockState();
    }

    public static synchronized boolean hasPendingFeatures(BlockPos pos) {
        if (!LegacyOptions.slowChunkLoading.get()) {
            return false;
        }

        long readyAt = featureReadyAt.get(SectionPos.asLong(pos));
        return readyAt != 0 && Util.getMillis() < readyAt;
    }

    public static synchronized void reset() {
        level = null;
        lastStep = Long.MIN_VALUE;
        revealed.clear();
        pending.clear();
        featureReadyAt.clear();
        hiddenFeatureSections.clear();
        hiddenFeatureModels.clear();
    }

    private static void collect(List<SectionRenderDispatcher.RenderSection> sections, Vec3 center, long centerSection) {
        for (SectionRenderDispatcher.RenderSection section : sections) {
            long key = key(section);
            if (revealed.contains(key)) {
                continue;
            }
            if (section.getBoundingBox().distanceToSqr(center) <= IMMEDIATE_DISTANCE * IMMEDIATE_DISTANCE) {
                pending.remove(key);
                reveal(key);
            } else {
                pending.add(key);
            }
        }
    }

    private static int stepCount(long step) {
        if (lastStep == Long.MIN_VALUE) {
            return 1;
        }
        long steps = Math.max(1, step - lastStep);
        return (int) Math.min(steps, MAX_CATCH_UP_STEPS);
    }

    private static void reveal(long centerSection, int steps) {
        int count = Math.min(pending.size(), revealBudget() * steps);
        if (count <= 0) {
            return;
        }

        long[] nearest = new long[count];
        int[] distances = new int[count];
        Arrays.fill(nearest, Long.MIN_VALUE);
        Arrays.fill(distances, Integer.MAX_VALUE);

        LongIterator iterator = pending.iterator();

        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            int distance = weightedDistance(key, centerSection);
            int slot = farthestSlot(distances);
            if (distance < distances[slot]) {
                nearest[slot] = key;
                distances[slot] = distance;
            }
        }

        for (long key : nearest) {
            if (key != Long.MIN_VALUE) {
                pending.remove(key);
                reveal(key);
                featureReadyAt.put(key, Util.getMillis() + delayMillis(key));
            }
        }
    }

    private static void collect(long key, int originX, int originY, int originZ, double cameraX, double cameraY, double cameraZ) {
        if (revealed.contains(key)) {
            return;
        }
        if (distanceToSectionSqr(originX, originY, originZ, cameraX, cameraY, cameraZ) <= IMMEDIATE_DISTANCE * IMMEDIATE_DISTANCE) {
            pending.remove(key);
            reveal(key);
        } else {
            pending.add(key);
        }
    }

    private static void reveal(long section) {
        if (revealed.add(section)) {
            markDirty(section);
        }
    }

    private static void markDirty(long section) {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable dirty = () -> {
            if (minecraft.level != null) {
                minecraft.levelRenderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
            }
        };
        if (minecraft.isSameThread()) {
            dirty.run();
        } else {
            minecraft.execute(dirty);
        }
    }

    private static void finishFeatureDelay(long section) {
        featureReadyAt.remove(section);
        hiddenFeatureSections.remove(section);
        markDirtyAround(section);
    }

    private static int delayMillis(long section) {
        ClientLevel currentLevel = Minecraft.getInstance().level;
        if (currentLevel == null) {
            return FEATURE_DELAY_MILLIS;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = SectionPos.sectionToBlockCoord(SectionPos.x(section));
        int minY = SectionPos.sectionToBlockCoord(SectionPos.y(section));
        int minZ = SectionPos.sectionToBlockCoord(SectionPos.z(section));
        for (int y = minY; y < minY + 16; y++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int x = minX; x < minX + 16; x++) {
                    if (currentLevel.getBlockState(pos.set(x, y, z)).is(Blocks.SNOW)) {
                        return SNOW_DELAY_MILLIS;
                    }
                }
            }
        }
        return FEATURE_DELAY_MILLIS;
    }

    private static void refreshFeatures(Minecraft minecraft) {
        if (featureReadyAt.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        LongIterator iterator = featureReadyAt.keySet().iterator();
        while (iterator.hasNext()) {
            long section = iterator.nextLong();
            long readyAt = featureReadyAt.get(section);
            if (now < readyAt) {
                continue;
            }

            iterator.remove();
            hiddenFeatureSections.remove(section);
            markDirtyAround(section);
        }
    }

    private static void markDirtyAround(long section) {
        int x = SectionPos.x(section);
        int y = SectionPos.y(section);
        int z = SectionPos.z(section);
        for (int dy = -1; dy <= 1; dy++) {
            markDirty(SectionPos.asLong(x, y + dy, z));
        }
        markDirty(SectionPos.asLong(x - 1, y, z));
        markDirty(SectionPos.asLong(x + 1, y, z));
        markDirty(SectionPos.asLong(x, y, z - 1));
        markDirty(SectionPos.asLong(x, y, z + 1));
    }

    private static int revealBudget() {
        int size = pending.size();
        if (size >= HIGH_BACKLOG) {
            return MAX_SECTIONS_PER_STEP;
        }
        if (size >= MID_BACKLOG) {
            return MID_SECTIONS_PER_STEP;
        }
        return BASE_SECTIONS_PER_STEP;
    }

    private static int farthestSlot(int[] distances) {
        int slot = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] > distances[slot]) {
                slot = i;
            }
        }
        return slot;
    }

    private static void trim(long centerSection, int distance) {
        int maxDistance = distance * distance;
        trim(pending, centerSection, maxDistance);
        trim(revealed, centerSection, maxDistance);
    }

    private static void trim(LongOpenHashSet chunks, long centerSection, int maxDistance) {
        LongIterator iterator = chunks.iterator();
        while (iterator.hasNext()) {
            long section = iterator.nextLong();
            if (horizontalDistance(section, centerSection) > maxDistance) {
                iterator.remove();
                featureReadyAt.remove(section);
                hiddenFeatureSections.remove(section);
            }
        }
    }

    private static boolean isFeatureState(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)) {
            return true;
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES)) {
            return true;
        }
        if (state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.STONE_BRICKS) || state.is(BlockTags.WALLS)) {
            return true;
        }

        Block block = state.getBlock();
        return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.LARGE_FERN
                || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.MUSHROOM_STEM
                || block == Blocks.SNOW
                || block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE || block == Blocks.BOOKSHELF || block == Blocks.CHEST
                || block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.LANTERN || block == Blocks.RAIL || block == Blocks.COBWEB || block == Blocks.IRON_BARS || block == Blocks.GLASS_PANE;
    }

    private static boolean isRevealed(SectionRenderDispatcher.RenderSection section) {
        return revealed.contains(key(section));
    }

    private static long key(SectionRenderDispatcher.RenderSection section) {
        return section.getSectionNode();
    }

    private static int horizontalDistance(long key, long centerSection) {
        int x = SectionPos.x(key) - SectionPos.x(centerSection);
        int z = SectionPos.z(key) - SectionPos.z(centerSection);
        return x * x + z * z;
    }

    private static int weightedDistance(long key, long centerSection) {
        int x = SectionPos.x(key) - SectionPos.x(centerSection);
        int y = SectionPos.y(key) - SectionPos.y(centerSection);
        int z = SectionPos.z(key) - SectionPos.z(centerSection);
        return x * x + y * y * 4 + z * z;
    }

    private static double distanceToSectionSqr(int originX, int originY, int originZ, double x, double y, double z) {
        double dx = Math.max(Math.max(originX - x, 0.0), x - (originX + 16));
        double dy = Math.max(Math.max(originY - y, 0.0), y - (originY + 16));
        double dz = Math.max(Math.max(originZ - z, 0.0), z - (originZ + 16));
        return dx * dx + dy * dy + dz * dz;
    }

    private record HiddenFeatureModel(BlockStateModel source) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource randomSource, List<BlockStateModelPart> parts) {
        }

        @Override
        public Material.Baked particleMaterial() {
            return source.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return source.materialFlags();
        }
    }
}
