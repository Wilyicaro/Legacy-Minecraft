package wily.legacy.client;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//? if <1.20.2 {
/*import it.unimi.dsi.fastutil.objects.ObjectArrayList;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? if <1.20.2 {
/*import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
*///?}
//? if >=1.20.2 {
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4JClient;
//? if <1.20.2 {
/*import wily.legacy.mixin.base.LevelRendererRenderChunkInfoAccessor;
*///?}
import wily.legacy.util.LegacyTags;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyChunkLoading {
    private static final int STEP_MILLIS = 75;
    private static final int FEATURE_DELAY_MILLIS = 1300;
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
    private static final LongOpenHashSet freshFeatureChunks = new LongOpenHashSet();
    private static final Map<Long, Long> featureReadyAt = new ConcurrentHashMap<>();
    private static final Set<Long> hiddenFeatureSections = ConcurrentHashMap.newKeySet();
    private static ClientLevel level;
    private static long lastStep = Long.MIN_VALUE;

    private LegacyChunkLoading() {
    }

    public static boolean isEnabled() {
        return LegacyOptions.slowChunkLoading.get() && !FactoryAPI.isModLoaded("sodium");
    }

    //? if >=1.20.2 {
    public static synchronized boolean filter(List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (!isEnabled() || minecraft.level == null) {
            reset();
            return false;
        }
        if (cameraEntity == null) {
            return false;
        }

        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }

        Vec3 center = cameraEntity.position();
        long centerSection = sectionKey(cameraEntity.blockPosition());

        collect(visible, center);
        collect(nearby, center);

        long step = Util.getMillis() / STEP_MILLIS;
        if (step != lastStep) {
            int steps = stepCount(step);
            lastStep = step;
            trim(centerSection, minecraft.options.renderDistance().get() + EXTRA_TRIM_DISTANCE);
            reveal(centerSection, steps);
        }

        refreshFeatures();
        visible.removeIf(section -> !isRevealed(section));
        if (nearby != null) nearby.removeIf(section -> !isRevealed(section));
        return !pending.isEmpty() || !featureReadyAt.isEmpty();
    }
    //?}

    //? if <1.20.2 {
    /*public static synchronized boolean filterLegacy(ObjectArrayList<?> visible) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (!isEnabled() || minecraft.level == null) {
            reset();
            return false;
        }
        if (cameraEntity == null) {
            return false;
        }

        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }

        Vec3 center = cameraEntity.position();
        long centerSection = sectionKey(cameraEntity.blockPosition());

        collectLegacy(visible, center);

        long step = Util.getMillis() / STEP_MILLIS;
        if (step != lastStep) {
            int steps = stepCount(step);
            lastStep = step;
            trim(centerSection, minecraft.options.renderDistance().get() + EXTRA_TRIM_DISTANCE);
            reveal(centerSection, steps);
        }

        refreshFeatures();
        visible.removeIf(info -> !isRevealed(((LevelRendererRenderChunkInfoAccessor) info).legacy$getChunk()));
        return !pending.isEmpty() || !featureReadyAt.isEmpty();
    }
    *///?}

    public static BlockState getFeatureState(BlockPos pos, BlockState state) {
        ClientLevel currentLevel = Minecraft.getInstance().level;
        if (!isEnabled() || currentLevel == null) {
            return state;
        }

        if (isHiddenFeatureSupport(pos, state, currentLevel)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        if (!isFeatureState(state)) {
            return state;
        }

        long section = sectionKey(pos);
        long readyAt = readyAt(section);
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

    public static boolean hasPendingFeatures(BlockPos pos) {
        if (!isEnabled()) {
            return false;
        }

        long section = sectionKey(pos);
        if (!hiddenFeatureSections.contains(section)) {
            return false;
        }

        long readyAt = readyAt(section);
        return readyAt != 0 && Util.getMillis() < readyAt;
    }

    public static synchronized boolean isSectionVisible(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isEnabled() || minecraft.level == null) {
            return true;
        }
        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }
        return revealed.contains(sectionKey(pos));
    }

    public static synchronized void markFreshChunk(int x, int z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isEnabled() || minecraft.level == null) {
            return;
        }
        if (level != minecraft.level) {
            reset();
            level = minecraft.level;
        }
        freshFeatureChunks.add(ChunkPos.asLong(x, z));
    }

    public static synchronized void reset() {
        level = null;
        lastStep = Long.MIN_VALUE;
        revealed.clear();
        pending.clear();
        freshFeatureChunks.clear();
        featureReadyAt.clear();
        hiddenFeatureSections.clear();
    }

    private static void clearSectionDelay(long section, boolean dirty) {
        if (!hasDelayedFeatures(section) && !pending.contains(section)) {
            return;
        }

        boolean changed = pending.remove(section);
        revealed.add(section);
        changed |= featureReadyAt.remove(section) != null;
        changed |= hiddenFeatureSections.remove(section);
        if (dirty && changed) {
            markDirtyAround(section);
        }
    }

    //? if >=1.20.2 {
    private static void collect(List<SectionRenderDispatcher.RenderSection> sections, Vec3 center) {
        if (sections == null) {
            return;
        }
        for (SectionRenderDispatcher.RenderSection section : sections) {
            long key = key(section);
            boolean immediate = section.getBoundingBox().distanceToSqr(center) <= IMMEDIATE_DISTANCE * IMMEDIATE_DISTANCE;
            if (revealed.contains(key)) {
                if (immediate && hasDelayedFeatures(key)) {
                    clearSectionDelay(key, true);
                }
                continue;
            }
            if (immediate) {
                pending.remove(key);
                reveal(key);
            } else {
                pending.add(key);
            }
        }
    }
    //?}

    //? if <1.20.2 {
    /*private static void collectLegacy(ObjectArrayList<?> chunks, Vec3 center) {
        for (Object info : chunks) {
            ChunkRenderDispatcher.RenderChunk chunk = ((LevelRendererRenderChunkInfoAccessor) info).legacy$getChunk();
            long key = key(chunk);
            boolean immediate = chunk.getBoundingBox().distanceToSqr(center) <= IMMEDIATE_DISTANCE * IMMEDIATE_DISTANCE;
            if (revealed.contains(key)) {
                if (immediate && hasDelayedFeatures(key)) {
                    clearSectionDelay(key, true);
                }
                continue;
            }
            if (immediate) {
                pending.remove(key);
                reveal(key);
            } else {
                pending.add(key);
            }
        }
    }
    *///?}

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
                if (shouldDelayFeatures(key)) {
                    featureReadyAt.put(key, Util.getMillis() + delayMillis(key));
                }
            }
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
        if (featureReadyAt.remove(section) != null) {
            hiddenFeatureSections.remove(section);
            markDirtyAround(section);
        }
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

    private static void refreshFeatures() {
        if (featureReadyAt.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        for (long section : featureReadyAt.keySet()) {
            long readyAt = readyAt(section);
            if (now < readyAt) {
                continue;
            }

            if (featureReadyAt.remove(section, readyAt)) {
                hiddenFeatureSections.remove(section);
                markDirtyAround(section);
            }
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
                freshFeatureChunks.remove(chunkKey(section));
            }
        }
    }

    private static boolean shouldDelayFeatures(long section) {
        return !hasFreshChunkSignals() || freshFeatureChunks.contains(chunkKey(section));
    }

    private static boolean hasFreshChunkSignals() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hasSingleplayerServer() || Legacy4JClient.hasModOnServer();
    }

    private static long chunkKey(long section) {
        return ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section));
    }

    private static boolean isFeatureState(BlockState state) {
        return state.is(LegacyTags.SLOW_CHUNK_FEATURES);
    }

    private static boolean isHiddenFeatureSupport(BlockPos pos, BlockState state, ClientLevel currentLevel) {
        if (!canRenderAsGrassUnderHiddenFeature(state) || featureReadyAt.isEmpty()) {
            return false;
        }

        if ((state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRASS_BLOCK)) && hasPendingFeatureDelay(sectionKey(pos))) {
            return true;
        }

        BlockPos above = pos.above();
        return hasPendingFeatureDelay(sectionKey(above)) && isFeatureState(currentLevel.getBlockState(above));
    }

    private static boolean canRenderAsGrassUnderHiddenFeature(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRASS_BLOCK);
    }

    private static boolean hasPendingFeatureDelay(long section) {
        long readyAt = readyAt(section);
        return readyAt != 0 && Util.getMillis() < readyAt;
    }

    private static boolean hasDelayedFeatures(long section) {
        return readyAt(section) != 0 || hiddenFeatureSections.contains(section);
    }

    private static long readyAt(long section) {
        Long readyAt = featureReadyAt.get(section);
        return readyAt == null ? 0 : readyAt;
    }

    //? if >=1.20.2 {
    private static boolean isRevealed(SectionRenderDispatcher.RenderSection section) {
        return revealed.contains(key(section));
    }

    private static long key(SectionRenderDispatcher.RenderSection section) {
        //? if <1.21.5 {
        BlockPos origin = section.getOrigin();
        return sectionKey(origin);
        //?} else {
        /*return section.getSectionNode();
        *///?}
    }
    //?}

    //? if <1.20.2 {
    /*private static boolean isRevealed(ChunkRenderDispatcher.RenderChunk chunk) {
        return revealed.contains(key(chunk));
    }

    private static long key(ChunkRenderDispatcher.RenderChunk chunk) {
        return sectionKey(chunk.getOrigin());
    }
    *///?}

    private static long sectionKey(BlockPos pos) {
        return SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
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
}
