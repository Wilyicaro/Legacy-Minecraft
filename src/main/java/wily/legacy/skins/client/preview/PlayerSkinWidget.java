package wily.legacy.skins.client.preview;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.function.Supplier;

public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F, ROTATION_X_LIMIT = 50.0F, CAROUSEL_INTERP_MS = 250.0F, CAROUSEL_INTERP_SMOOTH_MS = 190.0F, DEFAULT_CAROUSEL_FPS = 30.0F;
    private static final long MOVE_HINT_MS = 170L;
    private static volatile boolean CLIP_ENABLED;
    private static volatile int CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2;
    public final Supplier<String> skinId;
    private final wily.legacy.skins.client.screen.ChangeSkinScreenSource source;
    private final int originalWidth, originalHeight;
    public int slotOffset;
    public int renderRadius = 4;
    public float progress;
    private String skinIdValue;
    private int sourceSlotOffset;
    private float rotationX, rotationY, prevPosX, prevPosY, prevRotationX, prevRotationY, prevScale;
    private float targetRotationX = Float.NEGATIVE_INFINITY, targetRotationY = Float.NEGATIVE_INFINITY, targetPosX = Float.NEGATIVE_INFINITY,
            targetPosY = Float.NEGATIVE_INFINITY;
    private float scale = 1;
    private float targetScale = Float.NEGATIVE_INFINITY;
    private boolean wasHidden = true;
    private long start;
    private int lastStep = -1;
    private Integer snapX, snapY;
    private Float snapRotX, snapRotY, snapScale;
    private boolean crouchPose, punchLoop;
    private int pendingPoseMode = -1;
    private boolean pendingPunchLoop;
    private int moveHintDir;
    private long moveHintStart;

    public PlayerSkinWidget(wily.legacy.skins.client.screen.ChangeSkinScreenSource source, int width, int height) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        this.source = source;
        this.originalWidth = width;
        this.originalHeight = height;
        this.skinId = () -> skinIdValue;
    }

    public static void setCarouselClip(int x1, int y1, int x2, int y2) {
        CLIP_ENABLED = true;
        CLIP_X1 = x1;
        CLIP_Y1 = y1;
        CLIP_X2 = x2;
        CLIP_Y2 = y2;
    }

    public static void clearCarouselClip() {
        CLIP_ENABLED = false;
    }

    public static String clipText(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text;
        return font.width(value) <= maxWidth ? value : font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private static boolean isUpsideDownFacingFlip(String id) {
        return !SkinIdUtil.isBlankOrAutoSelect(id)
                && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, id);
    }

    public void setSkinId(String id) {
        this.skinIdValue = (id == null || id.isBlank()) ? null : id;
    }

    public void setSourceSlotOffset(int offset) {
        this.sourceSlotOffset = offset;
    }

    public void prewarm() {
        String id = skinId.get();
        if (id == null || id.isBlank()) return;
        source.prewarmPreview(id);
    }

    public boolean isInterpolating() {
        return targetRotationX != Float.NEGATIVE_INFINITY;
    }

    private void updateScaledSize() {
        setWidth(Math.round(originalWidth * scale));
        setHeight(Math.round(originalHeight * scale));
    }

    private void setTransform(float x, float y, float scale) {
        setX(Math.round(x));
        setY(Math.round(y));
        this.scale = scale;
        updateScaledSize();
    }

    private void clearInterpolationTargets() {
        targetRotationX = Float.NEGATIVE_INFINITY;
        targetRotationY = Float.NEGATIVE_INFINITY;
        targetPosX = Float.NEGATIVE_INFINITY;
        targetPosY = Float.NEGATIVE_INFINITY;
        targetScale = Float.NEGATIVE_INFINITY;
        snapX = null;
        snapY = null;
        snapRotX = null;
        snapRotY = null;
        snapScale = null;
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotationX;
            this.rotationY = targetRotationY;
            setTransform(targetPosX, targetPosY, targetScale);
            if (this.visible) this.wasHidden = false;
            clearInterpolationTargets();
            this.start = 0L;
            this.lastStep = -1;
            this.progress = 2;
            return;
        }
        this.progress = 0;
        this.start = System.currentTimeMillis();
        this.lastStep = -1;
        this.prevRotationX = rotationX;
        this.prevRotationY = rotationY;
        this.targetRotationX = targetRotationX;
        this.targetRotationY = targetRotationY;
        this.prevPosX = getX();
        this.prevPosY = getY();
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.prevScale = scale;
        this.targetScale = targetScale;
        this.snapX = null;
        this.snapY = null;
        this.snapRotX = null;
        this.snapRotY = null;
        this.snapScale = null;
    }

    public void snapTo(int x, int y, float rotX, float rotY, float scale) {
        this.snapX = x;
        this.snapY = y;
        this.snapRotX = rotX;
        this.snapRotY = rotY;
        this.snapScale = scale;
    }

    public void visible() {
        this.visible = true;
    }

    public void invisible() {
        this.wasHidden = true;
        this.visible = false;
        clearMoveHint();
        this.progress = 2;
        if (progress >= 1) finishInterpolation();
    }

    private void finishInterpolation() {
        boolean snapped = false;
        if (this.targetRotationX != Float.NEGATIVE_INFINITY) {
            this.rotationX = this.targetRotationX;
            this.rotationY = this.targetRotationY;
        }
        if (snapX != null && snapY != null) {
            setTransform(snapX, snapY, snapScale == null ? scale : snapScale);
            snapX = null;
            snapY = null;
            snapped = true;
            if (snapRotX != null && snapRotY != null) {
                this.rotationX = snapRotX;
                this.rotationY = snapRotY;
            }
            snapRotX = null;
            snapRotY = null;
            snapScale = null;
        } else if (this.targetPosX != Float.NEGATIVE_INFINITY) {
            setTransform(targetPosX, targetPosY, targetScale == Float.NEGATIVE_INFINITY ? scale : targetScale);
        }
        if (!snapped && this.targetScale != Float.NEGATIVE_INFINITY && targetPosX == Float.NEGATIVE_INFINITY) {
            this.scale = targetScale;
            updateScaledSize();
        }
        clearInterpolationTargets();
        this.lastStep = -1;
        this.progress = 2;
        if (pendingPoseMode != -1) {
            setPoseModeInternal(pendingPoseMode, pendingPunchLoop);
            pendingPoseMode = -1;
        }
    }

    private void interpolate(float progress) {
        if (!isInterpolating()) return;
        if (progress >= 1) {
            finishInterpolation();
            return;
        }
        float delta = progress;
        float nRotX = prevRotationX * (1 - delta) + targetRotationX * delta;
        float nRotY = prevRotationY * (1 - delta) + targetRotationY * delta;
        float nPosX = prevPosX * (1 - delta) + targetPosX * delta;
        float nPosY = prevPosY * (1 - delta) + targetPosY * delta;
        float nScale = prevScale * (1 - delta) + targetScale * delta;
        this.rotationX = nRotX;
        this.rotationY = nRotY;
        setTransform(nPosX, nPosY, nScale);
    }

    private int getDefaultCarouselStepCount(float interpMs) {
        return Math.max(1, Mth.ceil(interpMs * DEFAULT_CAROUSEL_FPS / 1000.0F));
    }

    private int getDefaultCarouselStep(long elapsed, float interpMs) {
        int stepCount = getDefaultCarouselStepCount(interpMs);
        if (elapsed >= interpMs) return stepCount;
        float frameMs = 1000.0F / DEFAULT_CAROUSEL_FPS;
        return Math.min(stepCount - 1, Math.max(0, (int) (elapsed / frameMs)));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        boolean clipActive = false;
        try {
            long now = System.currentTimeMillis();
            if (isInterpolating()) {
                long elapsed;
                float interpMs = LegacyOptions.smoothPreviewScroll.get() ? CAROUSEL_INTERP_SMOOTH_MS : CAROUSEL_INTERP_MS;
                if (LegacyOptions.smoothPreviewScroll.get()) {
                    if (start == 0L) start = now;
                    elapsed = Math.max(0L, now - start);
                    progress = elapsed / interpMs;
                    interpolate(progress);
                } else {
                    if (start == 0L) {
                        start = now;
                        lastStep = -1;
                    }
                    elapsed = Math.max(0L, now - start);
                    int stepCount = getDefaultCarouselStepCount(interpMs);
                    int step = getDefaultCarouselStep(elapsed, interpMs);
                    if (step != lastStep) {
                        lastStep = step;
                        progress = step / (float) stepCount;
                        interpolate(progress);
                    }
                }
            } else progress = 2;
            int absOffset = Math.abs(slotOffset);
            if (absOffset > 4) return;
            if (absOffset > renderRadius) {
                if (!isInterpolating() || Math.abs(sourceSlotOffset) > renderRadius) return;
            }
            int moveHintX = slotOffset == 0 ? resolveMoveHintOffset(now) : 0;
            int left = getX() + moveHintX;
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            if (CLIP_ENABLED && (right <= CLIP_X1 || left >= CLIP_X2 || bottom <= CLIP_Y1 || top >= CLIP_Y2)) return;
            String id = skinId.get();
            if (id == null) return;
            float attackTime = 0f;
            if (punchLoop) {
                long swing = 300L;
                long phase = now % (swing + 5L);
                attackTime = phase < swing ? phase / (float) swing : 0f;
            }
            float yawOffset = isUpsideDownFacingFlip(id) ? -rotationY : rotationY;
            if (CLIP_ENABLED) {
                try {
                    GuiGraphicsExtractor.disableScissor();
                } catch (IllegalStateException ignored) {
                }
                GuiGraphicsExtractor.enableScissor(CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2);
                clipActive = true;
            }
            source.renderPreview(GuiGraphicsExtractor, id, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom);
        } catch (RuntimeException ignored) {
        } finally {
            if (clipActive) {
                try {
                    GuiGraphicsExtractor.disableScissor();
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        applyDrag(dragX, dragY);
    }

    public void applyDrag(double dragX, double dragY) {
        if (!this.visible || !this.active || isInterpolating()) return;
        this.rotationX = Mth.clamp(this.rotationX - (float) dragY * 2.5F, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        this.rotationY += (float) dragX * ROTATION_SENSITIVITY;
        while (this.rotationY < 0) this.rotationY += 360;
        this.rotationY = (this.rotationY + 180) % 360 - 180;
    }

    public void togglePose() {
        if (punchLoop) punchLoop = false;
        else crouchPose = !crouchPose;
    }

    public void togglePunch() {
        if (crouchPose) crouchPose = false;
        else punchLoop = !punchLoop;
    }

    public void resetPose() {
        recenterView();
        resetPoseState();
    }

    public void resetPoseState() {
        crouchPose = false;
        punchLoop = false;
        pendingPoseMode = -1;
    }

    public int getPoseMode() {
        return crouchPose ? 1 : 0;
    }

    public boolean isPunchLoop() {
        return punchLoop;
    }

    public void setPoseMode(int mode, boolean punchLoop, boolean queueIfInterpolating) {
        if (queueIfInterpolating && isInterpolating()) {
            pendingPoseMode = mode;
            pendingPunchLoop = punchLoop;
            return;
        }
        setPoseModeInternal(mode, punchLoop);
    }

    private void setPoseModeInternal(int mode, boolean punchLoop) {
        this.punchLoop = punchLoop;
        this.crouchPose = mode == 1;
    }

    public void recenterView() {
        rotationX = 0;
        rotationY = 0;
    }

    public float getRotationX() {
        return rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public void hintMove(int dir) {
        if (dir == 0) return;
        moveHintDir = Integer.signum(dir);
        moveHintStart = System.currentTimeMillis();
    }

    private int resolveMoveHintOffset(long now) {
        if (moveHintDir == 0) return 0;
        float progress = (now - moveHintStart) / (float) MOVE_HINT_MS;
        if (progress >= 1f) {
            clearMoveHint();
            return 0;
        }
        float distance = Math.max(5f, getWidth() * 0.055f);
        return Math.round(Mth.sin(progress * Mth.PI) * distance * moveHintDir);
    }

    private void clearMoveHint() {
        moveHintDir = 0;
        moveHintStart = 0L;
    }
}
