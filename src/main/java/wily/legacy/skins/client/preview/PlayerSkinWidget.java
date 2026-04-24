package wily.legacy.skins.client.preview;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.client.util.*;
import wily.legacy.skins.skin.*;

import java.util.function.Supplier;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import wily.legacy.client.LegacyOptions;
public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F, ROTATION_X_LIMIT = 50.0F, CAROUSEL_INTERP_MS = 250.0F, CAROUSEL_INTERP_SMOOTH_MS = 190.0F, DEFAULT_CAROUSEL_FPS = 30.0F;
    private static final long MOVE_HINT_MS = 170L;
    private static volatile boolean CLIP_ENABLED, CENTER_NAME_PLATE, CENTER_NAME_PLATE_HIGHLIGHT, CENTER_NAME_PLATE_READY = true, CENTER_SELECTED_BADGE;
    private static volatile int CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2, CENTER_NAME_PLATE_W, CENTER_NAME_PLATE_H, CENTER_NAME_PLATE_PAD_Y,
            CENTER_NAME_PLATE_Y = -1, CENTER_NAME_PLATE_CENTER_X = -1, CENTER_NAME_PLATE_HIGHLIGHT_PAD, CENTER_NAME_PLATE_HIGHLIGHT_THICKNESS = 1,
            CENTER_NAME_PLATE_HIGHLIGHT_COLOR = 0xFFEBEB0F, CENTER_SELECTED_BADGE_W, CENTER_SELECTED_BADGE_H, CENTER_SELECTED_BADGE_GAP;
    private static volatile ResourceLocation CENTER_NAME_PLATE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/skin_box");
    private static volatile ResourceLocation CENTER_SELECTED_BADGE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected");
    private static volatile String CENTER_NAME_PLATE_DISPLAY_ID, CENTER_NAME_PLATE_PENDING_ID;
    private static volatile boolean CENTER_NAME_PLATE_WAITING;
    private final wily.legacy.skins.client.screen.ChangeSkinScreenSource source;
    public static void setCenterNamePlate(boolean enabled, int width, int height, int padY, int fixedY) {
        CENTER_NAME_PLATE = enabled;
        CENTER_NAME_PLATE_W = Math.max(1, width);
        CENTER_NAME_PLATE_H = Math.max(1, height);
        CENTER_NAME_PLATE_PAD_Y = Math.max(0, padY);
        CENTER_NAME_PLATE_Y = fixedY;
        if (!enabled) {
            CENTER_NAME_PLATE_DISPLAY_ID = null;
            CENTER_NAME_PLATE_PENDING_ID = null;
            CENTER_NAME_PLATE_WAITING = false;
        }
    }
    public static void setCenterNamePlateCenterX(int centerX) { CENTER_NAME_PLATE_CENTER_X = centerX; }

    public static void setCenterNamePlateSprite(ResourceLocation sprite) { if (sprite != null) CENTER_NAME_PLATE_SPRITE = sprite; }

    public static void setCenterNamePlateReady(boolean ready) {
        CENTER_NAME_PLATE_READY = ready;
        if (!ready) {
            CENTER_NAME_PLATE_WAITING = true;
            return;
        }
        if (CENTER_NAME_PLATE_WAITING) {
            if (CENTER_NAME_PLATE_PENDING_ID != null && !CENTER_NAME_PLATE_PENDING_ID.isBlank()) {
                CENTER_NAME_PLATE_DISPLAY_ID = CENTER_NAME_PLATE_PENDING_ID;
            }
            CENTER_NAME_PLATE_WAITING = false;
        }
    }

    public static void setCenterNamePlateHighlight(boolean enabled, int pad, int thickness, int color) {
        CENTER_NAME_PLATE_HIGHLIGHT = enabled;
        CENTER_NAME_PLATE_HIGHLIGHT_PAD = Math.max(0, pad);
        CENTER_NAME_PLATE_HIGHLIGHT_THICKNESS = Math.max(1, thickness);
        CENTER_NAME_PLATE_HIGHLIGHT_COLOR = color;
    }

    public static void setCenterSelectedBadge(boolean enabled, int width, int height, int gap, ResourceLocation sprite) {
        CENTER_SELECTED_BADGE = enabled;
        CENTER_SELECTED_BADGE_W = Math.max(1, width);
        CENTER_SELECTED_BADGE_H = Math.max(1, height);
        CENTER_SELECTED_BADGE_GAP = Math.max(0, gap);
        if (sprite != null) CENTER_SELECTED_BADGE_SPRITE = sprite;
    }
    private boolean isCurrentSkinSelected(String previewId) {
        if (previewId == null) return false;
        String applied = source.currentAppliedSkinId();
        if (SkinIdUtil.isAutoSelect(previewId)) return applied == null || applied.isBlank();
        return previewId.equals(applied);
    }
    public static void setCarouselClip(int x1, int y1, int x2, int y2) {
        CLIP_ENABLED = true;
        CLIP_X1 = x1;
        CLIP_Y1 = y1;
        CLIP_X2 = x2;
        CLIP_Y2 = y2;
    }
    public static void clearCarouselClip() { CLIP_ENABLED = false; }

    public static String clipText(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text;
        return font.width(value) <= maxWidth ? value : font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("..."))) + "...";
    }
    private String skinIdValue, cachedEntryId;
    private int cachedEntryVersion = -1;
    public final Supplier<String> skinId;
    private SkinEntry cachedEntry;
    private final int originalWidth, originalHeight;
    public int slotOffset;
    public int renderRadius = 4;
    private int sourceSlotOffset;
    private float rotationX, rotationY, prevPosX, prevPosY, prevRotationX, prevRotationY, prevScale;
    private float targetRotationX = Float.NEGATIVE_INFINITY, targetRotationY = Float.NEGATIVE_INFINITY, targetPosX = Float.NEGATIVE_INFINITY,
            targetPosY = Float.NEGATIVE_INFINITY;
    public float progress;
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
    public void setSkinId(String id) {
        this.skinIdValue = (id == null || id.isBlank()) ? null : id;
        if (this.skinIdValue == null || !this.skinIdValue.equals(cachedEntryId)) {
            cachedEntryId = null;
            cachedEntry = null;
            cachedEntryVersion = -1;
        }
    }
    public void setSourceSlotOffset(int offset) { this.sourceSlotOffset = offset; }
    private SkinEntry getCachedEntry(String id) {
        if (SkinIdUtil.isBlankOrAutoSelect(id)) return null;
        int version = source.version();
        if (id.equals(cachedEntryId) && cachedEntryVersion == version) return cachedEntry;
        cachedEntryId = id;
        cachedEntryVersion = version;
        cachedEntry = source.skin(id);
        return cachedEntry;
    }
    public void prewarm() {
        String id = skinId.get();
        if (id == null || id.isBlank()) return;
        source.prewarmPreview(id);
    }
    public boolean isInterpolating() { return targetRotationX != Float.NEGATIVE_INFINITY; }
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
    public void visible() { this.visible = true; }
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
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
                    guiGraphics.disableScissor();
                } catch (IllegalStateException ignored) { }
                guiGraphics.enableScissor(CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2);
                clipActive = true;
            }
            renderDoll(guiGraphics, partialTick, id, yawOffset, attackTime, left, top, right, bottom);
            if (CENTER_NAME_PLATE && slotOffset == 0) renderNamePlate(guiGraphics, id, left, right, bottom);
        } catch (RuntimeException ignored) {
        } finally {
            if (clipActive) {
                try {
                    guiGraphics.disableScissor();
                } catch (IllegalStateException ignored) { }
            }
        }
    }
    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { this.defaultButtonNarrationText(narrationElementOutput); }
    @Override
    public void playDownSound(SoundManager soundManager) { }
    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) { applyDrag(dragX, dragY); }
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
    public int getPoseMode() { return crouchPose ? 1 : 0; }
    public boolean isPunchLoop() { return punchLoop; }
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
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
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
    private static boolean isUpsideDownFacingFlip(String id) {
        return !SkinIdUtil.isBlankOrAutoSelect(id)
                && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, id);
    }
    private String resolveNamePlateId(String id) {
        if (id != null && !id.isBlank()) {
            CENTER_NAME_PLATE_PENDING_ID = id;
        }
        if (CENTER_NAME_PLATE_READY || CENTER_NAME_PLATE_DISPLAY_ID == null || CENTER_NAME_PLATE_DISPLAY_ID.isBlank()) {
            CENTER_NAME_PLATE_DISPLAY_ID = CENTER_NAME_PLATE_PENDING_ID;
        }
        return CENTER_NAME_PLATE_DISPLAY_ID;
    }
    private void renderDoll(GuiGraphics guiGraphics, float partialTick, String id, float yawOffset, float attackTime, int left, int top, int right, int bottom) {
        source.renderPreview(guiGraphics, id, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom);
    }
    private void renderNamePlate(GuiGraphics guiGraphics, String id, int left, int right, int bottom) {
        String displayId = resolveNamePlateId(id);
        if (displayId == null || displayId.isBlank()) return;
        String label = SkinIdUtil.isAutoSelect(displayId) ? "Current Skin" : nameLabel(displayId);
        if (label == null) label = "";
        int plateW = CENTER_NAME_PLATE_W;
        int plateH = CENTER_NAME_PLATE_H;
        int cx = CENTER_NAME_PLATE_CENTER_X >= 0 ? CENTER_NAME_PLATE_CENTER_X : (left + right) / 2;
        int plateX = cx - plateW / 2;
        int plateY = CENTER_NAME_PLATE_Y >= 0 ? CENTER_NAME_PLATE_Y : bottom + CENTER_NAME_PLATE_PAD_Y;
        if (CLIP_ENABLED) plateY = Math.max(CLIP_Y1, Math.min(plateY, CLIP_Y2 - plateH - 1));
        if (CENTER_NAME_PLATE_HIGHLIGHT) renderNamePlateHighlight(guiGraphics, plateX, plateY, plateW, plateH);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CENTER_NAME_PLATE_SPRITE, plateX, plateY, plateW, plateH);
        var font = Minecraft.getInstance().font;
        int maxPx = Math.max(1, plateW - 8);
        String theme = themeLabel(displayId, label);
        String showName = clipText(font, label, maxPx);
        if (theme == null) guiGraphics.drawCenteredString(font, Component.literal(showName), plateX + plateW / 2, plateY + (plateH - font.lineHeight) / 2, 0xFFFFFFFF);
        else {
            int baseY = plateY + (plateH - font.lineHeight * 2) / 2;
            guiGraphics.drawCenteredString(font, Component.literal(showName), plateX + plateW / 2, baseY, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(font, Component.literal(clipText(font, theme, maxPx)), plateX + plateW / 2, baseY + font.lineHeight, 0xFFFFFFFF);
        }
        if (!CENTER_SELECTED_BADGE || !CENTER_NAME_PLATE_READY || !isCurrentSkinSelected(displayId)) return;
        int badgeW = CENTER_SELECTED_BADGE_W;
        int badgeH = CENTER_SELECTED_BADGE_H;
        int badgeX = cx - badgeW / 2;
        int badgeY = plateY - CENTER_SELECTED_BADGE_GAP - badgeH;
        if (CLIP_ENABLED) badgeY = Math.max(CLIP_Y1, Math.min(badgeY, CLIP_Y2 - badgeH - 1));
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CENTER_SELECTED_BADGE_SPRITE, badgeX, badgeY, badgeW, badgeH);
        guiGraphics.drawCenteredString(font, Component.literal("Selected"), badgeX + badgeW / 2, badgeY + (badgeH - font.lineHeight) / 2 + 2, 0xFFFFFFFF);
    }
    private void renderNamePlateHighlight(GuiGraphics guiGraphics, int plateX, int plateY, int plateW, int plateH) {
        int pad = CENTER_NAME_PLATE_HIGHLIGHT_PAD;
        int x = plateX - pad;
        int y = plateY - pad;
        int w = plateW + pad * 2;
        int h = plateH + pad * 2;
        int thickness = Math.min(CENTER_NAME_PLATE_HIGHLIGHT_THICKNESS, Math.max(1, Math.min(w, h) / 2));
        guiGraphics.fill(x, y, x + w, y + thickness, CENTER_NAME_PLATE_HIGHLIGHT_COLOR);
        guiGraphics.fill(x, y + h - thickness, x + w, y + h, CENTER_NAME_PLATE_HIGHLIGHT_COLOR);
        guiGraphics.fill(x, y + thickness, x + thickness, y + h - thickness, CENTER_NAME_PLATE_HIGHLIGHT_COLOR);
        guiGraphics.fill(x + w - thickness, y + thickness, x + w, y + h - thickness, CENTER_NAME_PLATE_HIGHLIGHT_COLOR);
    }
    private String nameLabel(String id) {
        SkinEntry entry = getCachedEntry(id);
        return entry == null ? null : source.skinName(entry);
    }
    private String themeLabel(String id, String label) {
        if (SkinIdUtil.isAutoSelect(id)) return null;
        SkinEntry entry = getCachedEntry(id);
        ResourceLocation modelId = entry == null ? null : entry.modelId();
        if (modelId == null) {
            String ns = entry != null && entry.texture() != null ? entry.texture().getNamespace() : SkinSync.ASSET_NS;
            ResourceLocation texture = entry != null && entry.texture() != null ? entry.texture() : ResourceLocation.fromNamespaceAndPath(ns, id);
            modelId = ClientSkinAssets.getModelIdFromTexture(texture);
        }
        String theme = BoxModelManager.getThemeText(modelId);
        return theme == null || theme.isBlank() || theme.equals(label) ? null : theme;
    }
}
