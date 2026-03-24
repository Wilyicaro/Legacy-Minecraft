package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import wily.legacy.util.LegacyComponents;

public final class CreateWorldLoadingTracker {
    private static final long RESET_HOLD_MS = 90L;
    private static final long FINDING_SEED_RESET_HOLD_MS = 58L;
    private static final long FINDING_SEED_CYCLE_MS = 250L;
    private static final long PREPARING_FAKE_MS = 320L;
    private static final long LOADING_CYCLE_MS = 320L;
    private static final long PREPARING_CHUNKS_MS = 160L;
    private static final long FINALIZING_MS = 240L;
    private static final int FINDING_SEED_CYCLES = 5;
    private static final float FINDING_SEED_END_PROGRESS = 0.9F;
    private static final float FINDING_SEED_PHASE_SPEED = 1.95F;
    private static final float PREPARING_PHASE_SPEED = 2.6F;
    private static final float LOADING_PHASE_SPEED = 2.75F;
    private static final float SYNTHETIC_PHASE_SPEED = 1.35F;
    private static final int MAX_LOADING_CYCLES = 3;
    private static final float LOADING_RESET_PROGRESS = 0.6F;
    private static final float[] LOADING_CYCLE_ENDS = new float[]{0.78F, 0.8F, 0.82F};
    private static final float PREFERRED_RANGE_WEIGHT = 0.72F;
    private static final float PREFERRED_MIN_END = 0.74F;
    private static final float PREFERRED_MAX_END = 0.8F;
    private static final float VARIED_MIN_END = 0.2F;
    private static final float VARIED_MAX_END = 0.85F;
    private static Phase phase = Phase.NONE;
    private static long phaseStartedMillis;

    private CreateWorldLoadingTracker() {
    }

    public static void start() {
        phase = Phase.FINDING_SEED;
        phaseStartedMillis = Util.getMillis();
    }

    public static void startLoadingOnly() {
        phase = Phase.PREPARING;
        phaseStartedMillis = Util.getMillis();
    }

    public static void reset() {
        phase = Phase.NONE;
        phaseStartedMillis = 0L;
    }

    public static boolean isActive() {
        return phase != Phase.NONE;
    }

    public static State preparing(Component header, Component stage, float rawProgress) {
        if (!isActive()) return new State(header, stage, rawProgress);
        State findingSeed = initialFindingSeed(header);
        if (findingSeed != null) return findingSeed;
        if (phase == Phase.PREPARING_CHUNKS) return synthetic(header, LegacyComponents.PREPARING_CHUNKS, PREPARING_CHUNKS_MS);
        if (phase == Phase.FINALIZING) return synthetic(header, LegacyComponents.FINALIZING, FINALIZING_MS, 1.0F);
        setPhase(Phase.PREPARING);
        Component[] normalized = normalizeHeader(header, stage);
        return new State(normalized[0], normalized[1], timed(PREPARING_FAKE_MS, variedEnd(phaseStartedMillis), PREPARING_PHASE_SPEED, rawProgress));
    }

    public static State loading(Component header, Component stage, float rawProgress) {
        if (!isActive()) return new State(header, stage, rawProgress);
        State findingSeed = initialFindingSeed(header);
        if (findingSeed != null) return findingSeed;
        if (phase == Phase.PREPARING_CHUNKS) return synthetic(header, LegacyComponents.PREPARING_CHUNKS, PREPARING_CHUNKS_MS);
        if (phase == Phase.FINALIZING) return synthetic(header, LegacyComponents.FINALIZING, FINALIZING_MS, 1.0F);
        setPhase(Phase.LOADING);
        return new State(header, stage == null ? LegacyComponents.LOADING_SPAWN_AREA : stage, loop(LOADING_CYCLE_MS, LOADING_PHASE_SPEED));
    }

    public static boolean holdClose() {
        if (!isActive()) return false;
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (phase != Phase.PREPARING_CHUNKS && phase != Phase.FINALIZING) {
            phase = Phase.PREPARING_CHUNKS;
            phaseStartedMillis = Util.getMillis();
            return true;
        }
        if (phase == Phase.PREPARING_CHUNKS) {
            if (elapsed < PREPARING_CHUNKS_MS) return true;
            phase = Phase.FINALIZING;
            phaseStartedMillis = Util.getMillis();
            return true;
        }
        if (elapsed < FINALIZING_MS) return true;
        reset();
        return false;
    }

    private static State synthetic(Component header, Component stage, long durationMs) {
        return new State(header == null ? LegacyComponents.INITIALIZING : header, stage, timed(durationMs, variedEnd(phaseStartedMillis), SYNTHETIC_PHASE_SPEED, 0.0F));
    }

    private static State synthetic(Component header, Component stage, long durationMs, float endProgress) {
        return new State(header == null ? LegacyComponents.INITIALIZING : header, stage, timed(durationMs, endProgress, SYNTHETIC_PHASE_SPEED, 0.0F));
    }

    private static State initialFindingSeed(Component header) {
        if (phase != Phase.FINDING_SEED) return null;
        long elapsed = Util.getMillis() - phaseStartedMillis;
        long cycleLength = FINDING_SEED_RESET_HOLD_MS + FINDING_SEED_CYCLE_MS;
        long cycle = elapsed / cycleLength;
        if (cycle >= FINDING_SEED_CYCLES) {
            phase = Phase.PREPARING;
            phaseStartedMillis = Util.getMillis();
            return null;
        }
        long cycleElapsed = elapsed % cycleLength;
        float progress = cycleElapsed < FINDING_SEED_RESET_HOLD_MS
                ? 0.0F
                : accelerate((cycleElapsed - FINDING_SEED_RESET_HOLD_MS) / (float) FINDING_SEED_CYCLE_MS, FINDING_SEED_PHASE_SPEED) * FINDING_SEED_END_PROGRESS;
        return new State(header == null ? LegacyComponents.INITIALIZING : header, Component.translatable("legacy.finding_seed"), progress);
    }

    private static void setPhase(Phase phase) {
        if (CreateWorldLoadingTracker.phase == phase || CreateWorldLoadingTracker.phase == Phase.FINALIZING) return;
        CreateWorldLoadingTracker.phase = phase;
        phaseStartedMillis = Util.getMillis();
    }

    private static float timed(long durationMs, float endProgress, float speed, float rawProgress) {
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (elapsed < RESET_HOLD_MS) return 0.0F;
        float completion = Mth.clamp((elapsed - RESET_HOLD_MS) / (float) durationMs, 0.0F, 1.0F);
        return accelerate(Math.max(rawProgress, completion), speed) * endProgress;
    }

    private static float loop(long cycleMs, float speed) {
        long elapsed = Util.getMillis() - phaseStartedMillis;
        long cycleLength = RESET_HOLD_MS + cycleMs;
        long cycle = elapsed / cycleLength;
        if (cycle >= MAX_LOADING_CYCLES) return LOADING_CYCLE_ENDS[LOADING_CYCLE_ENDS.length - 1];
        long cycleElapsed = elapsed % cycleLength;
        float cycleStart = LOADING_RESET_PROGRESS;
        float cycleEnd = LOADING_CYCLE_ENDS[(int) cycle];
        if (cycleElapsed < RESET_HOLD_MS) return cycleStart;
        float completion = (cycleElapsed - RESET_HOLD_MS) / (float) cycleMs;
        return Mth.lerp(accelerate(completion, speed), cycleStart, cycleEnd);
    }

    private static float variedEnd(long salt) {
        long value = salt * 1103515245L + 12345L;
        float selector = (value & 1023L) / 1023.0F;
        value = value * 1103515245L + 12345L;
        float sample = (value & 1023L) / 1023.0F;
        return selector < PREFERRED_RANGE_WEIGHT
                ? Mth.lerp(sample, PREFERRED_MIN_END, PREFERRED_MAX_END)
                : Mth.lerp(sample, VARIED_MIN_END, VARIED_MAX_END);
    }

    private static Component[] normalizeHeader(Component header, Component stage) {
        Component normalizedHeader = header == null ? LegacyComponents.INITIALIZING : header;
        Component normalizedStage = stage;
        if (normalizedStage == null && !normalizedHeader.equals(LegacyComponents.INITIALIZING)) {
            normalizedStage = normalizedHeader;
            normalizedHeader = LegacyComponents.INITIALIZING;
        }
        return new Component[]{normalizedHeader, normalizedStage};
    }

    private static float accelerate(float rawProgress, float speed) {
        float boosted = Mth.clamp(rawProgress * speed, 0.0F, 1.0F);
        return 1.0F - (1.0F - boosted) * (1.0F - boosted);
    }

    public record State(Component header, Component stage, float progress) {
    }

    private enum Phase {
        NONE,
        FINDING_SEED,
        PREPARING,
        LOADING,
        PREPARING_CHUNKS,
        FINALIZING
    }
}
