package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import wily.legacy.util.LegacyComponents;

public final class CreateWorldLoadingTracker {
    private static final long RESET_HOLD_MS = 90L;
    private static final long FINDING_SEED_RESET_HOLD_MS = 58L;
    private static final long FINDING_SEED_CYCLE_MS = 250L;
    private static final float FINDING_SEED_END_PROGRESS = 0.82F;
    private static final int FINDING_SEED_CYCLES = 4;
    private static final long PREPARING_MS = 520L;
    private static final long LOADING_MS = 900L;
    private static final long PREPARING_CHUNKS_MS = 220L;
    private static final long FINALIZING_MS = 260L;
    private static final float LOADING_STAGE_END_PROGRESS = 0.84F;
    private static final float PREPARING_CHUNKS_END_PROGRESS = 0.82F;
    private static Phase phase = Phase.NONE;
    private static long phaseStartedMillis;
    private static float displayedProgress;
    private static boolean findingSeed;

    private CreateWorldLoadingTracker() {
    }

    public static void start() {
        start(false);
    }

    public static void start(boolean shouldFindSeed) {
        findingSeed = shouldFindSeed;
        phase = shouldFindSeed ? Phase.FINDING_SEED : Phase.PREPARING;
        phaseStartedMillis = Util.getMillis();
        displayedProgress = 0.0F;
    }

    public static void startLoadingOnly() {
        findingSeed = false;
        phase = Phase.PREPARING;
        phaseStartedMillis = Util.getMillis();
        displayedProgress = 0.0F;
    }

    public static void reset() {
        findingSeed = false;
        phase = Phase.NONE;
        phaseStartedMillis = 0L;
        displayedProgress = 0.0F;
    }

    public static boolean isActive() {
        return phase != Phase.NONE;
    }

    public static State preparing(Component header, Component stage, float rawProgress) {
        if (!isActive()) return new State(header, stage, quantize(rawProgress));
        State findingSeedState = findingSeedStage(header);
        if (findingSeedState != null) return findingSeedState;
        if (phase == Phase.PREPARING_CHUNKS) {
            return synthetic(header, LegacyComponents.PREPARING_CHUNKS, PREPARING_CHUNKS_MS, PREPARING_CHUNKS_END_PROGRESS);
        }
        if (phase == Phase.FINALIZING) {
            return synthetic(header, LegacyComponents.FINALIZING, FINALIZING_MS);
        }
        setPhase(Phase.PREPARING);
        Component[] normalized = normalizeHeader(header == null ? LegacyComponents.INITIALIZING : header, stage);
        float timeProgress = rangedProgress(PREPARING_MS);
        float rawTarget = quantize(rawProgress);
        return new State(normalized[0], normalized[1], advance(Math.max(timeProgress, rawTarget)));
    }

    public static State loading(Component header, Component stage, float rawProgress) {
        if (!isActive()) return new State(header, stage, quantize(rawProgress));
        State findingSeedState = findingSeedStage(header);
        if (findingSeedState != null) return findingSeedState;
        if (phase == Phase.PREPARING_CHUNKS) {
            return synthetic(header, LegacyComponents.PREPARING_CHUNKS, PREPARING_CHUNKS_MS, PREPARING_CHUNKS_END_PROGRESS);
        }
        if (phase == Phase.FINALIZING) {
            return synthetic(header, LegacyComponents.FINALIZING, FINALIZING_MS);
        }
        setPhase(Phase.LOADING);
        float timeProgress = rangedProgress(LOADING_MS, LOADING_STAGE_END_PROGRESS);
        float rawTarget = quantize(Mth.clamp(rawProgress, 0.0F, LOADING_STAGE_END_PROGRESS));
        return new State(header, stage == null ? LegacyComponents.LOADING_SPAWN_AREA : stage, advance(Math.max(timeProgress, rawTarget)));
    }

    public static boolean holdClose() {
        if (!isActive()) return false;
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (phase == Phase.FINDING_SEED) {
            if (elapsed < FINDING_SEED_RESET_HOLD_MS + FINDING_SEED_CYCLE_MS) return true;
            setPhase(Phase.PREPARING_CHUNKS);
            return true;
        }
        if (phase != Phase.PREPARING_CHUNKS && phase != Phase.FINALIZING) {
            setPhase(Phase.PREPARING_CHUNKS);
            return true;
        }
        if (phase == Phase.PREPARING_CHUNKS) {
            if (elapsed < PREPARING_CHUNKS_MS) return true;
            setPhase(Phase.FINALIZING);
            return true;
        }
        if (elapsed < FINALIZING_MS) return true;
        reset();
        return false;
    }

    private static State synthetic(Component header, Component stage, long durationMs) {
        return new State(header == null ? LegacyComponents.INITIALIZING : header, stage, advance(rangedProgress(durationMs)));
    }

    private static State synthetic(Component header, Component stage, long durationMs, float endProgress) {
        return new State(header == null ? LegacyComponents.INITIALIZING : header, stage, advance(rangedProgress(durationMs, endProgress)));
    }

    private static State findingSeedStage(Component header) {
        if (!findingSeed || phase != Phase.FINDING_SEED) return null;
        long cycleLength = FINDING_SEED_RESET_HOLD_MS + FINDING_SEED_CYCLE_MS;
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (elapsed >= cycleLength * FINDING_SEED_CYCLES) {
            findingSeed = false;
            setPhase(Phase.PREPARING);
            return null;
        }
        return new State(header == null ? LegacyComponents.INITIALIZING : header, Component.translatable("legacy.finding_seed"), loopingFindingSeedProgress());
    }

    private static void setPhase(Phase phase) {
        if (CreateWorldLoadingTracker.phase == phase || CreateWorldLoadingTracker.phase == Phase.FINALIZING) return;
        CreateWorldLoadingTracker.phase = phase;
        phaseStartedMillis = Util.getMillis();
        displayedProgress = 0.0F;
    }

    private static float rangedProgress(long durationMs) {
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (elapsed <= RESET_HOLD_MS) return -1.0F;
        float completion = Mth.clamp((elapsed - RESET_HOLD_MS) / (float) durationMs, 0.0F, 1.0F);
        return quantize(smoothstep(completion));
    }

    private static float rangedProgress(long durationMs, float endProgress) {
        long elapsed = Util.getMillis() - phaseStartedMillis;
        if (elapsed <= RESET_HOLD_MS) return -1.0F;
        float completion = Mth.clamp((elapsed - RESET_HOLD_MS) / (float) durationMs, 0.0F, 1.0F);
        return quantize(smoothstep(completion) * endProgress);
    }

    private static float advance(float targetProgress) {
        if (targetProgress < 0.0F) {
            return -1.0F;
        }
        displayedProgress = Math.max(displayedProgress, quantize(targetProgress));
        return displayedProgress;
    }

    private static float loopingFindingSeedProgress() {
        long elapsed = Util.getMillis() - phaseStartedMillis;
        long cycleLength = FINDING_SEED_RESET_HOLD_MS + FINDING_SEED_CYCLE_MS;
        long cycleElapsed = elapsed % cycleLength;
        if (cycleElapsed < FINDING_SEED_RESET_HOLD_MS) {
            displayedProgress = 0.0F;
            return 0.0F;
        }
        float completion = (cycleElapsed - FINDING_SEED_RESET_HOLD_MS) / (float) FINDING_SEED_CYCLE_MS;
        displayedProgress = quantize(smoothstep(Mth.clamp(completion, 0.0F, 1.0F)) * FINDING_SEED_END_PROGRESS);
        return displayedProgress;
    }

    private static float quantize(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        float stepped = (float) Math.floor(clamped * 100.0F) / 100.0F;
        if (clamped > 0.0F && stepped <= 0.0F) {
            return 0.01F;
        }
        return stepped;
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

    private static float smoothstep(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
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
