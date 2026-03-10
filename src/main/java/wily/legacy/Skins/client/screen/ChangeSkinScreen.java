package wily.legacy.Skins.client.screen;

import java.util.Locale;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;

import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.FavoritesStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSync;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Skins.client.compat.legacy4j.ControlsCompat;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;


public class ChangeSkinScreen extends AbstractChangeSkinScreen {
    private static final int BEACON_CHECK_TEXTURE_SIZE = 28;
    private static final int BEACON_CHECK_VISIBLE_X = 3;
    private static final int BEACON_CHECK_VISIBLE_Y = 4;
    private static final int BEACON_CHECK_VISIBLE_W = 24;
    private static final int BEACON_CHECK_VISIBLE_H = 20;
    private static final int SELECTION_ICON_SIZE = 16;
    private static final int SELECTION_ICON_OFFSET_X = 1;
    private static final int SELECTION_ICON_OFFSET_Y = 1;

    
    private static final ResourceLocation SKIN_PANEL          = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_panel");
    private static final ResourceLocation PANEL_FILLER        = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/panel_filler");
    private static final ResourceLocation PACK_NAME_BOX       = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/pack_name_box");
    private static final ResourceLocation SKIN_BOX            = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_box");
    private static final ResourceLocation SIZEABLE_ICON_HOLDER = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/sizeable_icon_holder.png");
    private static final ResourceLocation BEACON_CHECK        = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/beacon_check.png");
    private static final ResourceLocation HEART_CONTAINER     = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container");
    private static final ResourceLocation HEART_FULL          = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");

    

    private boolean holdingPackStick;
    private int     holdingPackDir;
    private long    holdingPackStartAt, holdingPackNextAt;


    public ChangeSkinScreen(Screen parent) {
        super(parent);
    }


    @Override
    protected Panel createTooltipBox() {
        return new Panel(UIAccessor.of(this)) {
            @Override
            public void init(String name) {
                super.init(name);
                
                panel.x -= (tooltipWidth - 2) / 2;
                int groupWidth = panel.width + tooltipWidth - 2;
                int minX = sc(5), maxX = ChangeSkinScreen.this.width - groupWidth - sc(5);
                if (maxX < minX) maxX = minX;
                panel.x = Math.max(minX, Math.min(panel.x, maxX));
                int minY = sc(5), maxY = ChangeSkinScreen.this.height - panel.height - sc(5);
                if (maxY < minY) maxY = minY;
                panel.y = Math.max(minY, Math.min(panel.y, maxY));
                appearance(LegacySprites.POINTER_PANEL, tooltipWidth, panel.height - sc(10));
                pos(panel.x + panel.width - 2, panel.y + sc(18));
            }
            @Override public void init()                                                { init("tooltipBox"); }
            @Override public void render(GuiGraphics g, int i, int j, float f)         { LegacyRenderUtil.renderPointerPanel(g, getX(), getY(), getWidth(), getHeight()); }
        };
    }

    @Override
    protected boolean isInCarouselBounds(double mx, double my) {
        
        if (tooltipBox == null || panel == null) return false;
        int x  = tooltipBox.x, y = panel.y + sc(45);
        int x2 = x + tooltipBox.getWidth() - sc(23);
        int y2 = y + tooltipBox.getHeight() - sc(90) - sc(24);
        return inside(mx, my, x + sc(2), y, Math.max(1, x2 - x - sc(2)), Math.max(1, y2 - y));
    }

    @Override
    protected boolean insideScrollRegion(double mx, double my) {
        return inside(mx, my, tooltipBox.x, tooltipBox.y, tooltipBox.getWidth(), tooltipBox.getHeight());
    }


    @Override
    public void renderableVListInit() {
        
        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL,
                        panel.x + sc(7), panel.y + sc(129), panel.width - sc(14), panel.height - sc(140)));

        
        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL,
                        previewBoxX(), previewBoxY(), previewBoxSize(), previewBoxSize()));

        
        addRenderableOnly((g, i, j, f) -> {
            ResourceLocation icon = getFocusedPackIcon();
            if (icon == null) return;
            int inner = Math.max(1, previewBoxSize() - 2);
            int[] d   = packIconDims(icon);
            float scale = Math.min(inner / (float) d[0], inner / (float) d[1]);
            float cx = previewBoxX() + 1 + inner / 2f, cy = previewBoxY() + 1 + inner / 2f;
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx, cy); pose.scale(scale, scale); pose.translate(-d[0] / 2f, -d[1] / 2f);
            g.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0, 0, d[0], d[1], d[0], d[1]);
            pose.popMatrix();
        });

        packList.refreshPackIdsIfNeeded();
        packList.populateInto(getRenderableVList());

        
        int x        = panel.x + sc(11);
        int w        = Math.max(1, panel.width - sc(22));
        int minY     = previewBoxY() + previewBoxSize() + sc(2);
        int y        = Math.max(panel.y + sc(136), minY);
        int h        = Math.max(1, panel.y + panel.height - sc(8) - y);
        getRenderableVList().scrollArrowYOffset(-8 - sc(8));
        getRenderableVList().init("consoleskins.packList", x, y, w, h);
    }

    @Override
    protected void panelInit() {
        uiScale      = computeScale(width, height);
        tooltipWidth = Math.max(1, Math.round(BASE_TOOLTIP_WIDTH * uiScale));
        renderableVList.layoutSpacing(l -> 0);
        packList.applyUiScale(uiScale);
        addRenderableOnly(panel);
        panel.init();
        tooltipBox.init("tooltipBox");

        if (firstOpen) {
            
            UUID self = minecraft.player != null ? minecraft.player.getUUID()
                      : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String selectedId = self != null ? ClientSkinCache.get(self) : null;
            String focusId;
            if (selectedId == null || selectedId.isBlank()) {
                focusId = SkinPackLoader.getPreferredDefaultPackId();
            } else {
                String src = SkinPackLoader.getSourcePackId(selectedId);
                focusId = src != null ? src : SkinPackLoader.getPreferredDefaultPackId();
            }
            if (focusId == null) { String openId = SkinPackLoader.getLastUsedCustomPackId(); if (openId != null) focusId = openId; }
            if (focusId != null) packList.focusPackId(focusId, false);
        }
    }

    @Override
    protected boolean handlePackListStepNavigation(int key) {
        boolean kbm  = ControlType.getActiveType().isKbm();
        boolean up   = (kbm && key == InputConstants.KEY_W) || key == InputConstants.KEY_UP;
        boolean down = (kbm && key == InputConstants.KEY_S) || key == InputConstants.KEY_DOWN;
        if (!(up || down)) return false;

        if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            var f = getFocused();
            if (!(f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton)) return false;
        }

        int count = packList.getPackCount();
        if (count <= 1) return true;

        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1; else if (target >= count) target = 0;

        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return true;

        packList.setFocusedPackIndex(target, true);
        focusPackListItem(btn);
        return true;
    }

    
    @Override
    protected void focusPackListItem(Object item) {
        RenderableVList vList = getRenderableVList();
        if (vList == null || item == null) return;

        if (item instanceof ChangeSkinPackList.PackButton btn) {
            if (btn.getPackIndex() < 0) return;
            if (children().contains(btn)) { setFocused(btn); return; }

            
            int target = btn.getPackIndex(), min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (var c : children()) {
                if (c instanceof ChangeSkinPackList.PackButton pb && pb.getPackIndex() >= 0) {
                    if (pb.getPackIndex() < min) min = pb.getPackIndex();
                    if (pb.getPackIndex() > max) max = pb.getPackIndex();
                }
            }

            boolean down = (min == Integer.MAX_VALUE) || (target > max) || (target >= min && target <= max);
            if (target < min && min != Integer.MAX_VALUE) down = false;

            
            int guard = Math.max(8, packList.getPackCount() + 4);
            for (int t = 0; t < guard && !children().contains(btn); t++) vList.mouseScrolled(down);
            if (children().contains(btn)) setFocused(btn);
            return;
        }
        super.focusPackListItem(item);
    }

    
    private void syncPackFocus() {
        ChangeSkinPackList.PackButton target = packList.getButtonForIndex(packList.getFocusedPackIndex());
        if (target == null) return;
        var f = getFocused();
        if ((f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton) && f != target)
            focusPackListItem(target);
    }

    private void stepPack(boolean up) {
        int count = packList != null ? packList.getPackCount() : 0;
        if (count <= 1) return;
        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1; else if (target >= count) target = 0;
        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return;
        packList.setFocusedPackIndex(target, true);
        focusPackListItem(btn);
    }

    

    private void startHoldingPackStick(int dir) {
        holdingPackStick = true; holdingPackDir = dir < 0 ? -1 : 1;
        long now = net.minecraft.Util.getMillis();
        holdingPackStartAt = now; holdingPackNextAt = now + 220L;
        stepPack(holdingPackDir < 0);
    }

    private void stopHoldingPackStick() {
        holdingPackStick = false; holdingPackDir = 0; holdingPackStartAt = holdingPackNextAt = 0L;
    }

    private void pumpHoldingPackStick() {
        if (!holdingPackStick) return;
        long now = net.minecraft.Util.getMillis();
        if (now < holdingPackNextAt) return;
        stepPack(holdingPackDir < 0);
        holdingPackNextAt = now + (now - holdingPackStartAt < 700L ? 120L : 80L);
    }

    
    private void renderSelectedSkinMarker(GuiGraphics g, int iconX, int iconY, int holderSize) {
        float drawW = Math.max(1, sc(SELECTION_ICON_SIZE));
        float drawH = Math.max(1, Math.round(drawW * (BEACON_CHECK_VISIBLE_H / (float) BEACON_CHECK_VISIBLE_W)));
        float scaleX = drawW / BEACON_CHECK_VISIBLE_W;
        float scaleY = drawH / BEACON_CHECK_VISIBLE_H;
        float drawX = iconX + (holderSize - drawW) / 2.0f + sc(SELECTION_ICON_OFFSET_X);
        float drawY = iconY + (holderSize - drawH) / 2.0f + sc(SELECTION_ICON_OFFSET_Y);
        g.pose().pushMatrix();
        g.pose().translate(drawX - BEACON_CHECK_VISIBLE_X * scaleX, drawY - BEACON_CHECK_VISIBLE_Y * scaleY);
        g.pose().scale(scaleX, scaleY);
        g.blit(RenderPipelines.GUI_TEXTURED, BEACON_CHECK, 0, 0, 0, 0, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE);
        g.pose().popMatrix();
    }

    

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (handleCarouselMouseClicked(e, bl)) return true;

        
        double mx = e.x(), my = e.y();
        int holder = Math.max(1, sc(24));
        int iconX  = tooltipBox.x + tooltipBox.getWidth() - sc(50);
        int iconY  = panel.y + tooltipBox.getHeight() - sc(60);
        if (inside(mx, my, iconX, iconY + sc(3),  holder, holder)) { selectSkin(); return true; }
        if (inside(mx, my, iconX, iconY + sc(30), holder, holder)) { favorite();   return true; }

        return super.mouseClicked(e, bl);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (handleSharedBindingState(state)) return;

        
        if (!ControlType.getActiveType().isKbm()
                && state != null && state.is(ControllerBinding.LEFT_STICK)
                && state instanceof BindingState.Axis stick) {
            final double triggerY = 0.65d, releaseY = 0.3d, sideLimit = 0.45d;
            double sx = stick.x, sy = stick.y, ay = Math.abs(sy);
            if (Math.abs(sx) > sideLimit || ay < releaseY) stopHoldingPackStick();
            else if (ay >= triggerY) { int dir = sy < 0 ? -1 : 1; if (!holdingPackStick || holdingPackDir != dir) startHoldingPackStick(dir); }
            pumpHoldingPackStick();
            if (Math.abs(sx) <= sideLimit && ay >= releaseY) { state.block(); return; }
        }

        super.bindingStateTick(state);
    }

    @Override
    public void tick() {
        super.tick();
        syncPackFocus();
        if (sharedTick()) return; 
        pumpQueuedCarousel();
        pumpHoldingOuterCarousel();
        pumpHoldingPackStick();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false);

        blitSprite(g, SKIN_PANEL,   tooltipBox.x - sc(10), panel.y + sc(7),  Math.max(1, tooltipBox.getWidth()), Math.max(1, tooltipBox.getHeight() - sc(2)));
        blitSprite(g, PANEL_FILLER, tooltipBox.x - sc(5),  panel.y + sc(16) + tooltipBox.getHeight() - sc(80), Math.max(1, tooltipBox.getWidth() - sc(14)), Math.max(1, sc(60)));
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, tooltipBox.x - sc(1), panel.y + tooltipBox.getHeight() - sc(59), Math.max(1, tooltipBox.getWidth() - sc(55)), Math.max(1, sc(55)));
        blitSprite(g, PACK_NAME_BOX, tooltipBox.x - sc(5), panel.y + sc(20), Math.max(1, tooltipBox.getWidth() - sc(18)), Math.max(1, sc(40)));
        blitSprite(g, SKIN_BOX,     tooltipBox.x - sc(5),  panel.y + sc(16), Math.max(1, tooltipBox.getWidth() - sc(14)), Math.max(1, tooltipBox.getHeight() - sc(80)));

        int holder    = Math.max(1, sc(24));
        int iconX     = tooltipBox.x + tooltipBox.getWidth() - sc(50);
        int iconBaseY = panel.y + tooltipBox.getHeight() - sc(60);
        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(3),  0, 0, holder, holder, 24, 24);
        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(30), 0, 0, holder, holder, 24, 24);

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String selected     = playerSkinWidgetList.element3.skinId.get();
            UUID   self         = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String current      = self == null ? null : ClientSkinCache.get(self);
            boolean isAuto      = "auto_select".equals(selected);
            boolean isAutoActive = current == null || current.isBlank();

            
            
            
            if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
                renderSelectedSkinMarker(g, iconX, iconBaseY + sc(3), holder);
            }

            if (selected != null && FavoritesStore.isFavorite(selected)) {
                int hx = iconX + sc(4);
                int hy = iconBaseY + sc(30) + sc(4);
                int hs = Math.max(1, sc(16));
                blitSprite(g, HEART_CONTAINER, hx, hy, hs, hs);
                blitSprite(g, HEART_FULL,      hx, hy, hs, hs);
            }
        }

        
        
        
        int cx = tooltipBox.x, cy = panel.y + sc(45);
        PlayerSkinWidget.setCarouselClip(cx + sc(2), cy, cx + tooltipBox.getWidth() - sc(23), cy + tooltipBox.getHeight() - sc(90) - sc(24));
        PlayerSkinWidget.setCarouselYawDenom(Math.max(1f, 240f * uiScale));

        
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String    skinId = playerSkinWidgetList.element3.skinId.get();
            SkinEntry entry  = skinId == null ? null : SkinPackLoader.getSkin(skinId);
            String    name   = entry == null ? String.valueOf(skinId) : entry.name();
            int mid       = tooltipBox.x - sc(5) + (tooltipBox.getWidth() - sc(18)) / 2;
            int skinNameY = panel.y + tooltipBox.getHeight() - sc(49);
            drawBigCentered(g, Component.literal(name), mid, skinNameY, 0xFFFFFFFF);

            ResourceLocation modelId = null;
            try {
                String ns = entry != null && entry.texture() != null ? entry.texture().getNamespace() : SkinSync.ASSET_NS;
                modelId = ResourceLocation.fromNamespaceAndPath(ns, skinId);
            } catch (Throwable ignored) {}

            String theme = modelId == null ? null : BoxModelManager.getThemeText(modelId);
            if (theme != null && !theme.isBlank() && !theme.equals(name)) {
                float scale = Math.max(0.65f, 1.485f * uiScale);
                int maxUnscaled = (int) ((tooltipBox.getWidth() - sc(26)) / scale);
                String show = theme;
                if (minecraft.font.width(show) > maxUnscaled) { int ellW = minecraft.font.width("…"); show = minecraft.font.plainSubstrByWidth(show, Math.max(0, maxUnscaled - ellW)) + "…"; }
                int themeY = skinNameY + (int) (minecraft.font.lineHeight * scale) + sc(6);
                drawBigCentered(g, Component.literal(show), mid, Math.min(themeY, panel.y + tooltipBox.getHeight() - sc(12)), 0xFFFFFFFF);
            }
        }

        
        SkinPack pack = getFocusedPack();
        if (pack != null) {
            int mid = tooltipBox.x - sc(5) + (tooltipBox.getWidth() - sc(18)) / 2;
            drawBigCentered(g, Component.literal(SkinPackLoader.nameString(pack.name(), pack.id())), mid, panel.y + sc(27), 0xFFFFFFFF);
            String t = pack.type();
            if (t != null && !t.isBlank()) {
                String k = t.toLowerCase(Locale.ROOT);
                Component label = null;
                if (k.equals("skin"))   label = Component.translatable("legacy.skinpack.type.skin");
                if (k.equals("mashup")) label = Component.translatable("legacy.skinpack.type.mashup");
                if (label != null) drawSmallCentered(g, label, mid, panel.y + sc(27) + (int) (minecraft.font.lineHeight * (1.55f * uiScale)) + sc(8), 0xCCFFFFFF);
            }
        }
    }

    

    @Override
    public void addControlTooltips(ControlTooltip.Renderer r) {
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),  () -> Component.literal("Select"));
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.cancel"));
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_F) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), () -> {
            String id = playerSkinWidgetList != null && playerSkinWidgetList.element3 != null ? playerSkinWidgetList.element3.skinId.get() : null;
            return id != null && FavoritesStore.isFavorite(id) ? Component.literal("Remove Favorite") : Component.literal("Add Favorite");
        });
        r.add(() -> ControlType.getActiveType().isKbm()
                ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_W), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_A), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_S), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_D)})
                : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.literal("Navigate"));
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> Component.literal("Advanced Options"));
    }

    

    @Override
    public void removed() {
        stopHoldingPackStick();
        super.removed();
    }
}
