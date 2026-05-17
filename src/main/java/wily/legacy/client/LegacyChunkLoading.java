package wily.legacy.client;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public final class LegacyChunkLoading {
    private static final int STEP_MILLIS = 75;
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
    private static ClientLevel level;
    private static long lastStep = Long.MIN_VALUE;

    private LegacyChunkLoading() {
    }

    public static boolean filter(List<SectionRenderDispatcher.RenderSection> visible, List<SectionRenderDispatcher.RenderSection> nearby) {
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

        visible.removeIf(section -> !isRevealed(section));
        nearby.removeIf(section -> !isRevealed(section));
        return !pending.isEmpty();
    }

    public static void reset() {
        level = null;
        lastStep = Long.MIN_VALUE;
        revealed.clear();
        pending.clear();
    }

    private static void collect(List<SectionRenderDispatcher.RenderSection> sections, Vec3 center, long centerSection) {
        for (SectionRenderDispatcher.RenderSection section : sections) {
            long key = key(section);
            if (revealed.contains(key)) {
                continue;
            }
            if (section.getBoundingBox().distanceToSqr(center) <= IMMEDIATE_DISTANCE * IMMEDIATE_DISTANCE) {
                pending.remove(key);
                revealed.add(key);
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
                revealed.add(key);
            }
        }
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
            if (horizontalDistance(iterator.nextLong(), centerSection) > maxDistance) {
                iterator.remove();
            }
        }
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
}
