package wily.legacy.Skins.client.preview;

import wily.legacy.Skins.client.gui.*;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.client.util.*;
import wily.legacy.Skins.skin.*;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F, ROTATION_X_LIMIT = 50.0F, CAROUSEL_INTERP_MS = 250.0F, CAROUSEL_INTERP_SMOOTH_MS = 190.0F;
    private static final int CAROUSEL_KEYFRAMES = 15;
    private static volatile boolean CLIP_ENABLED, CENTER_NAME_PLATE, CENTER_SELECTED_BADGE;
    private static volatile int CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2, CENTER_NAME_PLATE_W, CENTER_NAME_PLATE_H, CENTER_NAME_PLATE_PAD_Y,
            CENTER_NAME_PLATE_Y = -1, CENTER_NAME_PLATE_CENTER_X = -1, CENTER_SELECTED_BADGE_W, CENTER_SELECTED_BADGE_H, CENTER_SELECTED_BADGE_GAP;
    private static volatile ResourceLocation CENTER_NAME_PLATE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/skin_box");
    private static volatile ResourceLocation CENTER_SELECTED_BADGE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected");

    public static void setCenterNamePlate(boolean enabled, int width, int height, int padY, int fixedY) {
        CENTER_NAME_PLATE = enabled;
        CENTER_NAME_PLATE_W = Math.max(1, width);
        CENTER_NAME_PLATE_H = Math.max(1, height);
        CENTER_NAME_PLATE_PAD_Y = Math.max(0, padY);
        CENTER_NAME_PLATE_Y = fixedY;
    }

    public static void setCenterNamePlateCenterX(int centerX) { CENTER_NAME_PLATE_CENTER_X = centerX; }

    public static void setCenterNamePlateSprite(ResourceLocation sprite) { if (sprite != null) CENTER_NAME_PLATE_SPRITE = sprite; }

    public static void setCenterSelectedBadge(boolean enabled, int width, int height, int gap, ResourceLocation sprite) {
        CENTER_SELECTED_BADGE = enabled;
        CENTER_SELECTED_BADGE_W = Math.max(1, width);
        CENTER_SELECTED_BADGE_H = Math.max(1, height);
        CENTER_SELECTED_BADGE_GAP = Math.max(0, gap);
        if (sprite != null) CENTER_SELECTED_BADGE_SPRITE = sprite;
    }

    private static boolean isCurrentSkinSelected(String previewId) {
        if (previewId == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        UUID self = mc.player != null ? mc.player.getUUID() : (mc.getUser() != null ? mc.getUser().getProfileId() : null);
        if (self == null) return false;

        String applied = ClientSkinCache.get(self);
        if (applied == null) applied = SkinSync.getServerSkinId(self);
        if (applied == null) applied = "";

        if (SkinIdUtil.isAutoSelect(previewId)) { return applied.isBlank(); }
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

    private String skinIdValue, cachedEntryId;
    private int cachedEntryVersion = -1;
    public final Supplier<String> skinId;
    private SkinEntry cachedEntry;
    private final int originalWidth, originalHeight;
    public int slotOffset;
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

    public PlayerSkinWidget(int width, int height) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
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

    private SkinEntry getCachedEntry(String id) {
        if (SkinIdUtil.isBlankOrAutoSelect(id)) return null;
        int version = SkinPackLoader.getReloadVersion();
        if (id.equals(cachedEntryId) && cachedEntryVersion == version) return cachedEntry;
        cachedEntryId = id;
        cachedEntryVersion = version;
        cachedEntry = SkinPackLoader.getSkin(id);
        return cachedEntry;
    }

    public void prewarm() {
        String id = skinId.get();
        if (id == null || id.isBlank()) return;
        SkinPreviewWarmup.enqueue(id);
    }

    public boolean isInterpolating() { return targetRotationX != Float.NEGATIVE_INFINITY; }

    private void updateScaledSize() {
        setWidth(Math.round(originalWidth * scale));
        setHeight(Math.round(originalHeight * scale));
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotationX;
            this.rotationY = targetRotationY;
            this.setX(Math.round(targetPosX));
            this.setY(Math.round(targetPosY));
            this.scale = targetScale;
            updateScaledSize();
            if (this.visible) this.wasHidden = false;
            this.targetRotationX = Float.NEGATIVE_INFINITY;
            this.targetRotationY = Float.NEGATIVE_INFINITY;
            this.targetPosX = Float.NEGATIVE_INFINITY;
            this.targetPosY = Float.NEGATIVE_INFINITY;
            this.targetScale = Float.NEGATIVE_INFINITY;
            this.snapX = null;
            this.snapY = null;
            this.snapRotX = null;
            this.snapRotY = null;
            this.snapScale = null;
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
        this.progress = 2;
        if (progress >= 1) finishInterpolation();
    }

    private void finishInterpolation() {
        boolean snapped = false;
        if (this.targetRotationX != Float.NEGATIVE_INFINITY) {
            this.rotationX = this.targetRotationX;
            this.rotationY = this.targetRotationY;
        }
        this.targetRotationX = Float.NEGATIVE_INFINITY;
        this.targetRotationY = Float.NEGATIVE_INFINITY;
        if (snapX != null && snapY != null) {
            this.setX(snapX);
            this.setY(snapY);
            snapX = null;
            snapY = null;
            snapped = true;
            if (snapRotX != null && snapRotY != null) {
                this.rotationX = snapRotX;
                this.rotationY = snapRotY;
            }
            if (snapScale != null) {
                this.scale = snapScale;
                updateScaledSize();
            }
            snapRotX = null;
            snapRotY = null;
            snapScale = null;
        } else if (this.targetPosX != Float.NEGATIVE_INFINITY) {
            this.setX(Math.round(this.targetPosX));
            this.setY(Math.round(targetPosY));
        }
        this.targetPosX = Float.NEGATIVE_INFINITY;
        this.targetPosY = Float.NEGATIVE_INFINITY;
        if (!snapped && this.targetScale != Float.NEGATIVE_INFINITY) {
            this.scale = targetScale;
            updateScaledSize();
        }
        this.targetScale = Float.NEGATIVE_INFINITY;
        this.lastStep = -1;

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
        this.setX(Math.round(nPosX));
        this.setY(Math.round(nPosY));
        this.scale = nScale;
        updateScaledSize();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        boolean clipActive = false;
        try {
            long now = System.currentTimeMillis();
            final boolean smoothScroll = ConsoleSkinsClientSettings.isSmoothPreviewScroll();
            final float interpMs = smoothScroll ? CAROUSEL_INTERP_SMOOTH_MS : CAROUSEL_INTERP_MS;
            if (smoothScroll) {
                if (start == 0L) start = now;
                progress = (now - start) / interpMs;
                interpolate(progress);
            } else {
                if (start == 0L) {
                    start = now;
                    lastStep = -1;
                }
                float stepMs = interpMs / (float) CAROUSEL_KEYFRAMES;
                int step = (int) ((now - start) / stepMs);
                if (step != lastStep) {
                    lastStep = step;
                    progress = step / (float) CAROUSEL_KEYFRAMES;
                    interpolate(progress);
                }
            }

            int absOffset = Math.abs(this.slotOffset);
            if (absOffset >= 4 && progress > 1f) return;

            int left = this.getX();
            int top = this.getY();
            int right = left + this.getWidth();
            int bottom = top + this.getHeight();
            if (CLIP_ENABLED) { if (right <= CLIP_X1 || left >= CLIP_X2 || bottom <= CLIP_Y1 || top >= CLIP_Y2) return; }
            if (absOffset > 4) return;
            String id = skinId.get();
            if (id == null) return;
            float yawOffset = this.rotationY;
            if (isUpsideDownFacingFlip(id)) yawOffset = -yawOffset;
            float attackTime = 0.0F;
            if (punchLoop) {
                long swing = 300L;
                long phase = now % (swing + 5L);
                attackTime = phase < swing ? phase / (float) swing : 0.0F;
            }
            if (CLIP_ENABLED) {
                try {
                    guiGraphics.disableScissor();
                } catch (IllegalStateException ignored) { }
                guiGraphics.enableScissor(CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2);
                clipActive = true;
            }
            if (SkinIdUtil.isAutoSelect(id)) {
                var playerSkin = GuiSessionSkin.getSessionPlayerSkin();
                if (playerSkin != null) {
                    GuiDollRender.renderDollInRect(guiGraphics, id, playerSkin, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, 165);
                } else {
                    SkinEntry entry = SkinPackLoader.getSkin(id);
                    if (entry != null && entry.texture() != null) { GuiDollRender.renderDollInRect(guiGraphics, id, entry.texture(), yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, 165); }
                }
            } else {
                SkinEntry entry = getCachedEntry(id);
                if (entry != null && entry.texture() != null) { GuiDollRender.renderDollInRect(guiGraphics, id, entry.texture(), yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, 165); }
            }

            if (CENTER_NAME_PLATE && this.slotOffset == 0) {
                String label = null;
                if (!SkinIdUtil.isAutoSelect(id)) {
                    SkinEntry e = getCachedEntry(id);
                    if (e != null) { label = SkinPackLoader.nameString(e.name(), id); }
                } else { label = "Current Skin"; }
                if (label == null) label = "";

                int plateW = CENTER_NAME_PLATE_W;
                int plateH = CENTER_NAME_PLATE_H;
                int cx = CENTER_NAME_PLATE_CENTER_X >= 0 ? CENTER_NAME_PLATE_CENTER_X : (left + right) / 2;
                int plateX = cx - plateW / 2;
                int plateY = CENTER_NAME_PLATE_Y >= 0 ? CENTER_NAME_PLATE_Y : bottom + CENTER_NAME_PLATE_PAD_Y;
                if (CLIP_ENABLED) {
                    int maxY = CLIP_Y2 - plateH - 1;
                    if (plateY > maxY) plateY = maxY;
                    if (plateY < CLIP_Y1) plateY = CLIP_Y1;
                }

                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CENTER_NAME_PLATE_SPRITE, plateX, plateY, plateW, plateH);

                var font = Minecraft.getInstance().font;
                int maxPx = Math.max(1, plateW - 8);

                String theme = null;
                if (!SkinIdUtil.isAutoSelect(id)) {
                    SkinEntry themeEntry = getCachedEntry(id);
                    String ns = themeEntry != null && themeEntry.texture() != null ? themeEntry.texture().getNamespace() : SkinSync.ASSET_NS;
                    ResourceLocation texture = themeEntry != null && themeEntry.texture() != null
                            ? themeEntry.texture()
                            : ResourceLocation.fromNamespaceAndPath(ns, id);
                    theme = BoxModelManager.getThemeText(ClientSkinAssets.getModelIdFromTexture(texture));
                    if (theme != null && (theme.isBlank() || theme.equals(label))) theme = null;
                }

                String showName = font == null || label == null || label.isBlank()
                        ? ""
                        : font.width(label) <= maxPx
                            ? label
                            : font.plainSubstrByWidth(label, Math.max(0, maxPx - font.width("..."))) + "...";

                if (theme == null) {
                    int textY = plateY + (plateH - font.lineHeight) / 2;
                    guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal(showName), plateX + plateW / 2, textY, 0xFFFFFFFF);
                } else {
                    String showTheme = font == null || theme == null || theme.isBlank()
                            ? ""
                            : font.width(theme) <= maxPx
                                ? theme
                                : font.plainSubstrByWidth(theme, Math.max(0, maxPx - font.width("..."))) + "...";

                    int totalH = font.lineHeight * 2;
                    int baseY = plateY + (plateH - totalH) / 2;
                    guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal(showName), plateX + plateW / 2, baseY, 0xFFFFFFFF);
                    guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal(showTheme), plateX + plateW / 2, baseY + font.lineHeight, 0xFFFFFFFF);
                }

                if (CENTER_SELECTED_BADGE && isCurrentSkinSelected(id)) {
                    int badgeW = CENTER_SELECTED_BADGE_W;
                    int badgeH = CENTER_SELECTED_BADGE_H;
                    int badgeX = cx - badgeW / 2;
                    int badgeY = plateY - CENTER_SELECTED_BADGE_GAP - badgeH;
                    if (CLIP_ENABLED) {
                        int minY = CLIP_Y1;
                        int maxY = CLIP_Y2 - badgeH - 1;
                        if (badgeY < minY) badgeY = minY;
                        if (badgeY > maxY) badgeY = maxY;
                    }

                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CENTER_SELECTED_BADGE_SPRITE, badgeX, badgeY, badgeW, badgeH);

                    int by = badgeY + (badgeH - font.lineHeight) / 2;
                    guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal("Selected"), badgeX + badgeW / 2, by, 0xFFFFFFFF);
                }
            }
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

    private static boolean isUpsideDownFacingFlip(String id) {
        return !SkinIdUtil.isBlankOrAutoSelect(id)
                && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, id);
    }
}
