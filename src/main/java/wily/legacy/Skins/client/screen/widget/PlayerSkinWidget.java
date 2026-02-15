package wily.legacy.Skins.client.screen.widget;

import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.gui.GuiDollRender;
import wily.legacy.Skins.client.gui.GuiSessionSkin;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float CAROUSEL_INTERP_MS = 320.0F;
    private static final float CAROUSEL_INTERP_SMOOTH_MS = 220.0F;
    private static final long CAROUSEL_FPS_FRAME_MS = 33L;
    private static volatile boolean CLIP_ENABLED;
    private static volatile int CLIP_X1;
    private static volatile int CLIP_Y1;
    private static volatile int CLIP_X2;
    private static volatile int CLIP_Y2;

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
    private Integer snapX;
    private Integer snapY;
    private boolean crouchPose;
    private boolean punchLoop;
    private int punchCooldown;

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
        if (id.startsWith("cpm:") && entry != null && entry.texture() != null) {
            GuiCpmPreviewCache.prewarmMenuPreview(id, entry.texture());
        }
    }

    public boolean isInterpolating() {
        return !(targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX);
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotationX;
            this.rotationY = targetRotationY;
            this.setX((int) targetPosX);
            this.setY((int) targetPosY);
            this.scale = targetScale;
            setWidth((int) (this.originalWidth * scale));
            setHeight((int) (this.originalHeight * scale));
            if (this.visible) this.wasHidden = false;
            this.targetRotationX = Float.NEGATIVE_INFINITY;
            this.targetRotationY = Float.NEGATIVE_INFINITY;
            this.targetPosX = Float.NEGATIVE_INFINITY;
            this.targetPosY = Float.NEGATIVE_INFINITY;
            this.targetScale = Float.NEGATIVE_INFINITY;
            this.snapX = null;
            this.snapY = null;
            this.start = 0L;
            this.lastAnimUpdate = 0L;
            this.progress = 2;
            return;
        }
        this.progress = 0;
        this.start = System.currentTimeMillis();
        this.lastAnimUpdate = 0L;
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
    }

    public void snapTo(int x, int y) {
        this.snapX = x;
        this.snapY = y;
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
        } else if (this.targetPosX != Float.NEGATIVE_INFINITY) {
            this.setX((int) this.targetPosX);
            this.setY((int) targetPosY);
        }
        this.targetPosX = Float.NEGATIVE_INFINITY;
        this.targetPosY = Float.NEGATIVE_INFINITY;
        if (this.targetScale != Float.NEGATIVE_INFINITY) {
            this.scale = targetScale;
            setWidth((int) (this.originalWidth * scale));
            setHeight((int) (this.originalHeight * scale));
        }
        this.targetScale = Float.NEGATIVE_INFINITY;
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
        this.setX((int) nPosX);
        this.setY((int) nPosY);
        this.scale = nScale;
        setWidth((int) (this.originalWidth * scale));
        setHeight((int) (this.originalHeight * scale));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        long now = System.currentTimeMillis();
        final float interpMs = ConsoleSkinsClientSettings.isSmoothPreviewScroll() ? CAROUSEL_INTERP_SMOOTH_MS : CAROUSEL_INTERP_MS;
        if (ConsoleSkinsClientSettings.isSmoothPreviewScroll()) {
            lastAnimUpdate = now;
            if (start == 0L) start = now;
            progress = (now - start) / interpMs;
            interpolate(progress);
        } else if (now - lastAnimUpdate >= CAROUSEL_FPS_FRAME_MS) {
            lastAnimUpdate = now;
            if (start == 0L) start = now;
            progress = (now - start) / interpMs;
            interpolate(progress);
        }
        int left = this.getX();
        int top = this.getY();
        int right = left + this.getWidth();
        int bottom = top + this.getHeight();
        if (CLIP_ENABLED) {
            if (right <= CLIP_X1 || left >= CLIP_X2 || bottom <= CLIP_Y1 || top >= CLIP_Y2) return;
        }
        if (Math.abs(this.slotOffset) > 3) return;
        String id = skinId.get();
        if (id == null) return;
        float yawOffset;
        if (this.active || this.carouselCenterX == Integer.MIN_VALUE) {
            yawOffset = this.rotationY;
        } else {
            float cx = this.getX() + (this.getWidth() * 0.5f);
            float dx = cx - (float) this.carouselCenterX;
            float t = Mth.clamp(dx / 240.0f, -1.0f, 1.0f);
            yawOffset = t * 55.0f;
        }
        float attackTime = 0.0F;
        if (punchLoop) {
            long ms = System.currentTimeMillis();
            float t = (ms % 400L) / 400.0F;
            attackTime = t < 0.5F ? (t * 2.0F) : ((1.0F - t) * 2.0F);
        }
        int sizeCap = 175;
        if (CLIP_ENABLED) {
            try {
                guiGraphics.disableScissor();
            } catch (Throwable ignored) {
            }
            int clipY1 = CLIP_Y1;
            if (id.startsWith("cpm:")) {
                clipY1 = Math.max(0, clipY1 - GuiDollRender.CPM_TOP_CLIP_HEADROOM_PX);
            }
            guiGraphics.enableScissor(CLIP_X1, clipY1, CLIP_X2, CLIP_Y2);
        }
        if ("auto_select".equals(id)) {
            var ps = GuiSessionSkin.getSessionPlayerSkin();
            if (ps != null) {
                GuiDollRender.renderDollInRect(guiGraphics, id, ps, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, sizeCap);
            } else {
                SkinEntry entry = SkinPackLoader.getSkin(id);
                if (entry != null) {
                    GuiDollRender.renderDollInRect(guiGraphics, id, entry.texture(), yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, sizeCap);
                }
            }
        } else {
            SkinEntry entry = SkinPackLoader.getSkin(id);
            if (entry != null) {
                ResourceLocation tex = entry.texture();
                String renderId = id;
                ResourceLocation renderTex = tex;
                boolean isCpm = id.startsWith("cpm:") && tex != null;
                if (isCpm) {
                    GuiCpmPreviewCache.prewarmMenuPreview(id, tex);
                    boolean resolved = GuiCpmPreviewCache.isResolved(id, tex);
                    if (!resolved && lastStableId != null && lastStableTexture != null) {
                        renderId = lastStableId;
                        renderTex = lastStableTexture;
                    } else if (resolved) {
                        lastStableId = id;
                        lastStableTexture = tex;
                    }
                } else {
                    if (tex != null) {
                        lastStableId = id;
                        lastStableTexture = tex;
                    }
                }
                if (renderTex != null) {
                    GuiDollRender.renderDollInRect(guiGraphics, renderId, renderTex, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, sizeCap);
                }
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
        crouchPose = !crouchPose;
    }

    public void togglePunch() {
        this.punchLoop = !this.punchLoop;
        this.punchCooldown = 0;
    }

    public void resetPose() {
        rotationX = 0;
        rotationY = 0;
        crouchPose = false;
        punchLoop = false;
        punchCooldown = 0;
    }

    public void recenterView() {
        rotationX = 0;
        rotationY = 0;
    }
}
