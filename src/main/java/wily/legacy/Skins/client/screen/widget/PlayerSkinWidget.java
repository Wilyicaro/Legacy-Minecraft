package wily.legacy.Skins.client.screen.widget;

import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.gui.GuiDollRender;
import wily.legacy.Skins.client.gui.GuiSessionSkin;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSync;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float CAROUSEL_INTERP_MS = 250.0F;
    private static final float CAROUSEL_INTERP_SMOOTH_MS = 190.0F;
    private static final long CAROUSEL_FPS_FRAME_MS = 16L;
    private static final int CAROUSEL_KEYFRAMES = 15;
private static volatile boolean CLIP_ENABLED;
    private static volatile int CLIP_X1;
    private static volatile int CLIP_Y1;
    private static volatile int CLIP_X2;
    private static volatile int CLIP_Y2;
    private static volatile float CAROUSEL_YAW_DENOM = 240.0f;

    private static volatile boolean CENTER_NAME_PLATE;
    private static volatile int CENTER_NAME_PLATE_W;
    private static volatile int CENTER_NAME_PLATE_H;
    private static volatile int CENTER_NAME_PLATE_PAD_Y;
    private static volatile boolean CENTER_NAME_PLATE_FIXED_Y;
    private static volatile int CENTER_NAME_PLATE_Y;
    private static volatile boolean CENTER_NAME_PLATE_FIXED_CENTER_X;
    private static volatile int CENTER_NAME_PLATE_CENTER_X;

    private static volatile ResourceLocation CENTER_NAME_PLATE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/skin_box");

    private static volatile boolean CENTER_SELECTED_BADGE;
    private static volatile int CENTER_SELECTED_BADGE_W;
    private static volatile int CENTER_SELECTED_BADGE_H;
    private static volatile int CENTER_SELECTED_BADGE_GAP;
    private static volatile float CENTER_SELECTED_BADGE_ALPHA = 1f;
    private static volatile ResourceLocation CENTER_SELECTED_BADGE_SPRITE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected");

    public static void setCenterNamePlate(boolean enabled, int width, int height, int padY, int fixedY) {
        CENTER_NAME_PLATE = enabled;
        CENTER_NAME_PLATE_W = Math.max(1, width);
        CENTER_NAME_PLATE_H = Math.max(1, height);
        CENTER_NAME_PLATE_PAD_Y = Math.max(0, padY);
        CENTER_NAME_PLATE_FIXED_Y = fixedY >= 0;
        CENTER_NAME_PLATE_Y = fixedY;
    }

    public static void setCenterNamePlateCenterX(int centerX) {
        CENTER_NAME_PLATE_FIXED_CENTER_X = centerX >= 0;
        CENTER_NAME_PLATE_CENTER_X = centerX;
    }

    public static void setCenterNamePlateSprite(ResourceLocation sprite) {
        if (sprite != null) CENTER_NAME_PLATE_SPRITE = sprite;
    }

    public static void setCenterSelectedBadge(boolean enabled, int width, int height, int gap, float alpha, ResourceLocation sprite) {
        CENTER_SELECTED_BADGE = enabled;
        CENTER_SELECTED_BADGE_W = Math.max(1, width);
        CENTER_SELECTED_BADGE_H = Math.max(1, height);
        CENTER_SELECTED_BADGE_GAP = Math.max(0, gap);
        CENTER_SELECTED_BADGE_ALPHA = alpha < 0f ? 0f : Math.min(1f, alpha);
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

        if ("auto_select".equals(previewId)) {
            return applied.isBlank();
        }
        return previewId.equals(applied);
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

    public static void setCarouselYawDenom(float denom) {
        CAROUSEL_YAW_DENOM = denom <= 0f ? 240.0f : denom;
    }

    private String skinIdValue;
    public final Supplier<String> skinId;
    private String lastStableId;
    private ResourceLocation lastStableTexture;
    private final int originalWidth;
    private final int originalHeight;
    public int slotOffset;
    private int carouselCenterX = Integer.MIN_VALUE;
    private float rotationX;
    private float rotationY;
    private float targetRotationX = Float.NEGATIVE_INFINITY;
    private float targetRotationY = Float.NEGATIVE_INFINITY;
    private float targetPosX = Float.NEGATIVE_INFINITY;
    private float targetPosY = Float.NEGATIVE_INFINITY;
    private float prevPosX;
    private float prevPosY;
    private float prevRotationX;
    private float prevRotationY;
    private float prevScale;
    public float progress;
    private float scale = 1;
    private float targetScale = Float.NEGATIVE_INFINITY;
    private boolean wasHidden = true;
    private long start;
    private long lastAnimUpdate;
    private int lastStep = -1;
    private Integer snapX;
    private Integer snapY;
    private Float snapRotX;
    private Float snapRotY;
    private Float snapScale;
    private boolean crouchPose;    private boolean punchLoop;
    private int punchCooldown;

    private int pendingPoseMode = -1;
    private boolean pendingPunchLoop;

    public PlayerSkinWidget(int width, int height) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        this.originalWidth = width;
        this.originalHeight = height;
        this.skinId = () -> skinIdValue;
    }

    public boolean hasStablePreview() {
        return lastStableId != null && lastStableTexture != null;
    }

    public String getStablePreviewId() {
        return lastStableId;
    }

    public net.minecraft.resources.ResourceLocation getStablePreviewTexture() {
        return lastStableTexture;
    }

    public void seedStablePreviewIfMissing(String id, net.minecraft.resources.ResourceLocation texture) {
        if (!hasStablePreview() && id != null && texture != null) {
            this.lastStableId = id;
            this.lastStableTexture = texture;
        }
    }

    public void setSkinId(String id) {
        this.skinIdValue = (id == null || id.isBlank()) ? null : id;
    }

    public void setCarouselCenterX(int centerX) {
        this.carouselCenterX = centerX;
    }

    public void prewarm() {
        String id = skinId.get();
        if (id == null || id.isBlank()) return;
        if ("auto_select".equals(id)) return;
        SkinEntry entry = SkinPackLoader.getSkin(id);
        if (entry != null && entry.texture() != null) {
            try {
                Minecraft.getInstance().getTextureManager().getTexture(entry.texture());
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean isInterpolating() {
        return !(targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX);
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotationX;
            this.rotationY = targetRotationY;
            this.setX(Math.round(targetPosX));
            this.setY(Math.round(targetPosY));
            this.scale = targetScale;
            setWidth(Math.round(this.originalWidth * scale));
            setHeight(Math.round(this.originalHeight * scale));
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
            this.lastAnimUpdate = 0L;
            this.lastStep = -1;
            this.progress = 2;
            return;
        }
        this.progress = 0;
        this.start = System.currentTimeMillis();
        this.lastAnimUpdate = 0L;
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

    public void snapTo(int x, int y) {
        this.snapX = x;
        this.snapY = y;
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
                setWidth(Math.round(this.originalWidth * scale));
                setHeight(Math.round(this.originalHeight * scale));
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
            setWidth(Math.round(this.originalWidth * scale));
            setHeight(Math.round(this.originalHeight * scale));
        }
        this.targetScale = Float.NEGATIVE_INFINITY;
        this.lastStep = -1;

        if (pendingPoseMode != -1) {
            setPoseModeInternal(pendingPoseMode, pendingPunchLoop);
            pendingPoseMode = -1;
        }
    }

    private void interpolate(float progress) {
        if (targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX) return;
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
        setWidth(Math.round(this.originalWidth * scale));
        setHeight(Math.round(this.originalHeight * scale));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        long now = System.currentTimeMillis();
        final boolean smoothScroll = ConsoleSkinsClientSettings.isSmoothPreviewScroll();
        final float interpMs = smoothScroll ? CAROUSEL_INTERP_SMOOTH_MS : CAROUSEL_INTERP_MS;
        if (smoothScroll) {
            lastAnimUpdate = now;
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
        int left = this.getX();
        int top = this.getY();
        int right = left + this.getWidth();
        int bottom = top + this.getHeight();

        int renderTop = top;
        int renderBottom = bottom;
        if (CLIP_ENABLED) {
            if (right <= CLIP_X1 || left >= CLIP_X2 || bottom <= CLIP_Y1 || top >= CLIP_Y2) return;
        }
        if (Math.abs(this.slotOffset) > 4) return;
        String id = skinId.get();
        if (id == null) return;
        float yawOffset = this.rotationY;
if (isUpsideDownFacingFlip(id)) yawOffset = -yawOffset;
        float attackTime = 0.0F;
        if (punchLoop) {

            long ms = System.currentTimeMillis();

            long swing = 300L;

            long phase = ms % (swing + 5L);

            if (phase < swing) {

                attackTime = phase / (float) swing;

            } else {

                attackTime = 0.0F;

            }

        }
        int sizeCap = 165;
        if (CLIP_ENABLED) {
            try {
                guiGraphics.disableScissor();
            } catch (Throwable ignored) {
            }
            guiGraphics.enableScissor(CLIP_X1, CLIP_Y1, CLIP_X2, CLIP_Y2);
        }
        if ("auto_select".equals(id)) {
            var ps = GuiSessionSkin.getSessionPlayerSkin();
            if (ps != null) {
                GuiDollRender.renderDollInRect(guiGraphics, id, ps, yawOffset, crouchPose, attackTime, partialTick, left, renderTop, right, renderBottom, sizeCap);
            } else {
                SkinEntry entry = SkinPackLoader.getSkin(id);
                if (entry != null) {
                    GuiDollRender.renderDollInRect(guiGraphics, id, entry.texture(), yawOffset, crouchPose, attackTime, partialTick, left, renderTop, right, renderBottom, sizeCap);
                }
            }
        } else {
            SkinEntry entry = SkinPackLoader.getSkin(id);
            if (entry != null) {
                ResourceLocation tex = entry.texture();
                if (tex != null) {
                    lastStableId = id;
                    lastStableTexture = tex;
                    GuiDollRender.renderDollInRect(guiGraphics, id, tex, yawOffset, crouchPose, attackTime, partialTick, left, renderTop, right, renderBottom, sizeCap);
                }
            }
        }

        if (CENTER_NAME_PLATE && this.slotOffset == 0) {
            String label = null;
            if (!"auto_select".equals(id)) {
                SkinEntry e = SkinPackLoader.getSkin(id);
                if (e != null) {
                    label = SkinPackLoader.nameString(e.name(), id);
                }
            } else {
                label = "Current Skin";
            }
            if (label == null) label = "";

            int plateW = CENTER_NAME_PLATE_W;
            int plateH = CENTER_NAME_PLATE_H;
            int cx = CENTER_NAME_PLATE_FIXED_CENTER_X ? CENTER_NAME_PLATE_CENTER_X : (left + right) / 2;
            int plateX = cx - plateW / 2;
            int plateY = CENTER_NAME_PLATE_FIXED_Y ? CENTER_NAME_PLATE_Y : bottom + CENTER_NAME_PLATE_PAD_Y;
            if (CLIP_ENABLED) {
                int maxY = CLIP_Y2 - plateH - 1;
                if (plateY > maxY) plateY = maxY;
                if (plateY < CLIP_Y1) plateY = CLIP_Y1;
            }

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CENTER_NAME_PLATE_SPRITE, plateX, plateY, plateW, plateH);

            var font = Minecraft.getInstance().font;
            int maxPx = Math.max(1, plateW - 8);

            // Optional theme/subtitle line (Box meta) beneath the skin name.
            String theme = null;
            if (!"auto_select".equals(id)) {
                try {
                    SkinEntry e = SkinPackLoader.getSkin(id);
                    String ns = e != null && e.texture() != null ? e.texture().getNamespace() : SkinSync.ASSET_NS;
                    ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(ns, id);
                    theme = BoxModelManager.getThemeText(modelId);
                } catch (Throwable ignored) {
                }
            if (theme != null && (theme.isBlank() || theme.equals(label))) theme = null;
            }

            String showName = label;
            if (font.width(showName) > maxPx) {
                int ellW = font.width("…");
                showName = font.plainSubstrByWidth(showName, Math.max(0, maxPx - ellW)) + "…";
            }

            if (theme == null) {
                int textY = plateY + (plateH - font.lineHeight) / 2;
                guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal(showName), plateX + plateW / 2, textY, 0xFFFFFFFF);
            } else {
                String showTheme = theme;
                if (font.width(showTheme) > maxPx) {
                    int ellW = font.width("…");
                    showTheme = font.plainSubstrByWidth(showTheme, Math.max(0, maxPx - ellW)) + "…";
                }

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
        if (CLIP_ENABLED) {
            try {
                guiGraphics.disableScissor();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    private void onDragInternal(double deltaX, double deltaY) {
        if (isInterpolating() || !this.active) return;
        this.rotationX = Mth.clamp(this.rotationX - (float) deltaY * 2.5F, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        this.rotationY += (float) deltaX * ROTATION_SENSITIVITY;
        while (this.rotationY < 0) this.rotationY += 360;
        this.rotationY = (this.rotationY + 180) % 360 - 180;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.visible || !this.active || isInterpolating()) return;
        onDragInternal(dragX, dragY);
    }

    public void applyDrag(double dragX, double dragY) {
        if (!this.visible || !this.active || isInterpolating()) return;
        onDragInternal(dragX, dragY);
    }

    public void togglePose() {
        if (punchLoop) {
            punchLoop = false;
            punchCooldown = 0;
            return;
        }
        crouchPose = !crouchPose;
    }

    public void togglePunch() {
        if (crouchPose) {
            crouchPose = false;
            return;
        }
        punchLoop = !punchLoop;
        punchCooldown = 0;
    }

    public void resetPose() {
        rotationX = 0;
        rotationY = 0;
        crouchPose = false;
        punchLoop = false;
        punchCooldown = 0;
        pendingPoseMode = -1;
    }

    public void resetPoseState() {
        crouchPose = false;
        punchLoop = false;
        punchCooldown = 0;
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
        this.punchCooldown = 0;
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

    public void setRotation(float rotationX, float rotationY) {
        this.rotationX = Mth.clamp(rotationX, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        this.rotationY = rotationY;
        while (this.rotationY < 0) this.rotationY += 360;
        this.rotationY = (this.rotationY + 180) % 360 - 180;
        this.targetRotationX = Float.NEGATIVE_INFINITY;
        this.targetRotationY = Float.NEGATIVE_INFINITY;
    }

    private static boolean isUpsideDownFacingFlip(String id) {
        if (id == null || id.isBlank()) return false;
        if ("auto_select".equals(id)) return false;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, id)) return true;
        return false;
    }
}
