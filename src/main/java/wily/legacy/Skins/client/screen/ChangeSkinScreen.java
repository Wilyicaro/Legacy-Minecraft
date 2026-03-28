package wily.legacy.Skins.client.screen;

import java.util.Locale;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;

import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinLayoutMetrics;
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
    private static final float NORMAL_CAROUSEL_BASE_SCALE = 0.935f;
    private static final float NORMAL_CAROUSEL_BASE_SPACING = 80f;
    private static final int PACK_LIST_VISIBLE_ROWS = 6;
    private static final int PACK_LIST_FOOTER_RESERVE = 12;
    private static final ChangeSkinScreenLayout HD_LAYOUT = new ChangeSkinScreenLayout(
            false,
            180, 290, 400,
            24, 112,
            34, 20, 7,
            10, 5, 5,
            5, 18, 10,
            1.485f, 0.65f,
            1.045f, 0.60f,
            ChangeSkinLayoutMetrics.DEFAULT
    );

    private record NormalLayoutMetrics(
            int skinPanelInsetX,
            int skinPanelTop,
            int panelFillerInsetX,
            int panelFillerTop,
            int panelFillerBottomTrim,
            int panelFillerWidthTrim,
            int panelFillerHeight,
            int infoPanelInsetX,
            int infoPanelBottomTrim,
            int infoPanelWidthTrim,
            int infoPanelHeight,
            int packNameInsetX,
            int packNameTop,
            int packNameWidthTrim,
            int packNameHeight,
            int skinBoxInsetX,
            int skinBoxTop,
            int skinBoxWidthTrim,
            int skinBoxBottomTrim,
            int actionHolderSize,
            int actionHolderXOffset,
            int actionHolderBaseY,
            int actionHolderTopOffset,
            int actionHolderGap,
            int packFrameInsetX,
            int packFrameTop,
            int packFrameWidthTrim,
            int packFrameBottomTrim,
            int packListInsetX,
            int packListWidthTrim,
            int packListTop,
            int packListBottomInset,
            int previewListGap,
            int scrollArrowOffset,
            int infoCenterInsetX,
            int infoCenterWidthTrim,
            int skinNameBottomTrim,
            int themeTextWidthTrim,
            int themeTextGap,
            int themeBottomInset,
            int packTitleTop,
            int packMetaGap,
            int packTypeAdvance
    ) {
        static final NormalLayoutMetrics DEFAULT = new NormalLayoutMetrics(
                10, 7,
                5, 16, 80, 14, 60,
                1, 59, 55, 55,
                5, 20, 18, 40,
                5, 16, 14, 80,
                24, 50, 60, 3, 30,
                7, 129, 14, 140,
                11, 22, 136, 8, 2, 8,
                5, 18, 49, 26, 6, 12,
                27, 8, 10
        );

        static final NormalLayoutMetrics SD_480 = new NormalLayoutMetrics(
                7, 6,
                4, 8, 48, 10, 34,
                1, 38, 34, 40,
                4, 11, 12, 26,
                4, 10, 10, 50,
                18, 34, 40, 2, 18,
                5, 92, 10, 102,
                8, 12, 94, 8, 1, 6,
                4, 10, 32, 18, 4, 10,
                18, 6, 8
        );
    }

    private static final int BEACON_CHECK_TEXTURE_SIZE = 28;
    private static final int BEACON_CHECK_VISIBLE_X = 3;
    private static final int BEACON_CHECK_VISIBLE_Y = 4;
    private static final int BEACON_CHECK_VISIBLE_W = 24;
    private static final int BEACON_CHECK_VISIBLE_H = 20;
    private static final int SELECTION_ICON_SIZE = 16;
    private static final int ACTION_ICON_PADDING = 2;
    private static final int ACTION_HOLDER_BORDER = 1;
    private static final int PACK_BUTTON_BASE_HEIGHT = 20;

    
    private static final ResourceLocation SKIN_PANEL          = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_panel");
    private static final ResourceLocation PANEL_FILLER        = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/panel_filler");
    private static final ResourceLocation PACK_NAME_BOX       = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/pack_name_box");
    private static final ResourceLocation SKIN_BOX            = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_box");
    private static final ResourceLocation SIZEABLE_ICON_HOLDER = ResourceLocation.fromNamespaceAndPath("legacy", "container/sizeable_icon_holder");
    private static final ResourceLocation BEACON_CHECK        = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/beacon_check.png");
    private static final ResourceLocation HEART_CONTAINER     = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container");
    private static final ResourceLocation HEART_FULL          = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");

    

    private boolean holdingPackStick;
    private int     holdingPackDir;
    private long    holdingPackStartAt, holdingPackNextAt;
    private NormalLayoutMetrics normalLayout = NormalLayoutMetrics.DEFAULT;
    private int resolvedPackRowHeight = PACK_BUTTON_BASE_HEIGHT;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;


    public ChangeSkinScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected ChangeSkinScreenLayout resolveRuntimeLayout() {
        return isCompact480() ? ChangeSkinScreenLayout.DEFAULT : HD_LAYOUT;
    }

    @Override
    protected ChangeSkinScreenLayout resolveFitReferenceLayout() {
        return isCompact480() ? ChangeSkinScreenLayout.DEFAULT : HD_LAYOUT;
    }

    private void refreshNormalLayout() {
        normalLayout = NormalLayoutMetrics.DEFAULT;
    }

    private void refreshNormalSdFit() {
        if (isCompact480()) {
            uiScale = Math.min(1f, uiScale * 1.10f);
        } else {
            uiScale = Math.min(1f, uiScale * 1.14f);
        }
        tooltipWidth = Math.max(1, Math.round(layoutProfile.baseTooltipWidth() * uiScale));
    }

    private void applyNormalCarouselTuning(boolean relayout) {
        if (playerSkinWidgetList == null) return;

        float scaleMultiplier = getLayoutMetrics().centerScale() / NORMAL_CAROUSEL_BASE_SCALE;
        float spacingMultiplier = getLayoutMetrics().carouselOffset() / NORMAL_CAROUSEL_BASE_SPACING;
        if (scaleMultiplier <= 0f) scaleMultiplier = 1f;
        if (spacingMultiplier <= 0f) spacingMultiplier = 1f;

        playerSkinWidgetList.setCarouselTuning(scaleMultiplier, spacingMultiplier);
        if (relayout) playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }

    private int resolvePackRowHeight() {
        int scaledHeight = Math.max(10, Math.round(PACK_BUTTON_BASE_HEIGHT * uiScale));
        int availableHeight = resolvePackListAvailableHeight();
        int fittedHeight = Math.max(10, (availableHeight - PACK_LIST_FOOTER_RESERVE) / PACK_LIST_VISIBLE_ROWS);
        if (isCompact480()) return Math.min(scaledHeight, fittedHeight);
        return Math.min(fittedHeight, Math.max(18, scaledHeight));
    }

    private int resolvePackListAvailableHeight() {
        int minY = Math.max(panel.y + sc(normalLayout.packListTop()), previewBoxY() + previewBoxSize() + sc(normalLayout.previewListGap()));
        return Math.max(1, panel.y + panel.height - sc(normalLayout.packListBottomInset()) - minY);
    }

    private int adjustPackListHeight(int height) {
        if (!isCompact480()) return height;
        int visibleRowsHeight = resolvedPackRowHeight * PACK_LIST_VISIBLE_ROWS;
        return Math.max(1, Math.min(height, visibleRowsHeight + PACK_LIST_FOOTER_RESERVE));
    }

    @Override
    protected int previewBoxX() {
        int size = previewBoxSize();
        return panel.x + Math.max(sc(7), (panel.width - size) / 2);
    }

    @Override
    protected void onWidgetListCreated(PlayerSkinWidgetList list) {
        applyNormalCarouselTuning(false);
    }

    @Override
    protected void onAfterSkinPackChanged() {
        applyNormalCarouselTuning(true);
    }


    @Override
    protected Panel createTooltipBox() {
        return new Panel(UIAccessor.of(this)) {
            @Override
            public void init(String name) {
                super.init(name);
                int groupWidth = panel.width + tooltipWidth - 2;
                int desiredGroupX = (ChangeSkinScreen.this.width - groupWidth) / 2;
                panel.x = desiredGroupX;
                int minX = sc(layoutProfile.tooltipGroupMargin()), maxX = ChangeSkinScreen.this.width - groupWidth - sc(layoutProfile.tooltipGroupMargin());
                if (maxX < minX) maxX = minX;
                panel.x = Math.max(minX, Math.min(panel.x, maxX));
                int minY = sc(layoutProfile.tooltipGroupMargin());
                int groupBottomTrim = Math.max(0, sc(layoutProfile.tooltipYOffset() - layoutProfile.tooltipHeightInset()));
                int groupHeight = panel.height + groupBottomTrim;
                int desiredGroupY = (ChangeSkinScreen.this.height - groupHeight) / 2;
                panel.y = desiredGroupY;
                int maxY = ChangeSkinScreen.this.height - controlTooltipFooterReserve() - groupHeight - sc(layoutProfile.tooltipGroupMargin());
                if (maxY < minY) maxY = minY;
                panel.y = Math.max(minY, Math.min(panel.y, maxY));
                appearance(LegacySprites.POINTER_PANEL, tooltipWidth, panel.height - sc(layoutProfile.tooltipHeightInset()));
                pos(panel.x + panel.width - 2, panel.y + sc(layoutProfile.tooltipYOffset()));
            }
            @Override public void init()                                                { init("tooltipBox"); }
            @Override public void render(GuiGraphics g, int i, int j, float f)         { LegacyRenderUtil.renderPointerPanel(g, getX(), getY(), getWidth(), getHeight()); }
        };
    }

    @Override
    protected boolean isInCarouselBounds(double mx, double my) {
        if (tooltipBox == null || panel == null) return false;
        int clipLeft = carouselClipLeft();
        int clipTop = tooltipContentTop();
        int clipRight = carouselClipRight();
        int clipBottom = tooltipContentBottom() - sc(getLayoutMetrics().carouselClipBottomTrim());
        return inside(mx, my, clipLeft, clipTop, Math.max(1, clipRight - clipLeft), Math.max(1, clipBottom - clipTop));
    }

    @Override
    protected boolean insideScrollRegion(double mx, double my) {
        return inside(mx, my, tooltipBox.x, tooltipBox.y, tooltipBox.getWidth(), tooltipBox.getHeight());
    }

    private int carouselClipLeft() {
        int clipLeft = tooltipBox.x + sc(getLayoutMetrics().carouselClipInset());
        if (!isCompact480()) clipLeft += sc(3);
        return clipLeft;
    }

    private int carouselClipRight() {
        int clipRight = tooltipContentRight();
        if (!isCompact480()) clipRight -= sc(3);
        return clipRight;
    }


    @Override
    public void renderableVListInit() {
        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL,
                        panel.x + sc(normalLayout.packFrameInsetX()), panel.y + sc(normalLayout.packFrameTop()), panel.width - sc(normalLayout.packFrameWidthTrim()), panel.height - sc(normalLayout.packFrameBottomTrim())));

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

        int x        = panel.x + sc(normalLayout.packListInsetX());
        int w        = Math.max(1, panel.width - sc(normalLayout.packListWidthTrim()));
        int minY     = previewBoxY() + previewBoxSize() + sc(normalLayout.previewListGap());
        int y        = Math.max(panel.y + sc(normalLayout.packListTop()), minY);
        int h        = adjustPackListHeight(Math.max(1, panel.y + panel.height - sc(normalLayout.packListBottomInset()) - y));
        getRenderableVList().verticalScrollArrowSize(isCompact480() ? 13 : 16, isCompact480() ? 7 : 9);
        getRenderableVList().verticalScrollArrowOffset(isCompact480() ? 0 : -5, isCompact480() ? 8 : -2);
        getRenderableVList().scrollArrowYOffset(-normalLayout.scrollArrowOffset() - sc(normalLayout.scrollArrowOffset()));
        getRenderableVList().init("consoleskins.packList", x, y, w, h);
    }

    @Override
    protected void panelInit() {
        boolean windowResized = width != lastLayoutWidth || height != lastLayoutHeight;
        refreshSharedLayout();
        refreshNormalLayout();
        refreshNormalSdFit();
        renderableVList.layoutSpacing(l -> 0);
        if (windowResized) {
            getRenderableVList().resetScroll();
        }
        addRenderableOnly(panel);
        panel.init();
        applyResolvedPanelBounds();
        resolvedPackRowHeight = resolvePackRowHeight();
        packList.applyResolvedButtonHeight(resolvedPackRowHeight);
        tooltipBox.init("tooltipBox");
        lastLayoutWidth = width;
        lastLayoutHeight = height;

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
        float drawX = iconX + (holderSize - drawW) / 2.0f;
        float drawY = iconY + (holderSize - drawH) / 2.0f;
        g.pose().pushMatrix();
        g.pose().translate(drawX - BEACON_CHECK_VISIBLE_X * scaleX, drawY - BEACON_CHECK_VISIBLE_Y * scaleY);
        g.pose().scale(scaleX, scaleY);
        g.blit(RenderPipelines.GUI_TEXTURED, BEACON_CHECK, 0, 0, 0, 0, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE);
        g.pose().popMatrix();
    }

    private void renderFavoriteHeart(GuiGraphics g, int iconX, int iconY, int holderSize) {
        int heartSize = Math.max(1, holderSize - 8);
        int heartX = iconX + Math.round((holderSize - heartSize) / 2.0f);
        int heartY = iconY + Math.round((holderSize - heartSize) / 2.0f);
        blitSprite(g, HEART_CONTAINER, heartX, heartY, heartSize, heartSize);
        blitSprite(g, HEART_FULL, heartX, heartY, heartSize, heartSize);
    }

    

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (handleCarouselMouseClicked(e, bl)) return true;

        
        double mx = e.x(), my = e.y();
        int holder = Math.max(1, sc(normalLayout.actionHolderSize()));
        int iconX  = tooltipBox.x + tooltipBox.getWidth() - sc(normalLayout.actionHolderXOffset());
        int iconY  = panel.y + tooltipBox.getHeight() - sc(normalLayout.actionHolderBaseY());
        if (inside(mx, my, iconX, iconY + sc(normalLayout.actionHolderTopOffset()), holder, holder)) { selectSkin(); return true; }
        if (inside(mx, my, iconX, iconY + sc(normalLayout.actionHolderGap()), holder, holder)) { favorite(); return true; }

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
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);

        blitSprite(g, SKIN_PANEL, tooltipBox.x - sc(normalLayout.skinPanelInsetX()), panel.y + sc(normalLayout.skinPanelTop()), Math.max(1, tooltipBox.getWidth()), Math.max(1, tooltipBox.getHeight() - sc(2)));
        blitSprite(g, PANEL_FILLER, tooltipBox.x - sc(normalLayout.panelFillerInsetX()), panel.y + sc(normalLayout.panelFillerTop()) + tooltipBox.getHeight() - sc(normalLayout.panelFillerBottomTrim()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.panelFillerWidthTrim())), Math.max(1, sc(normalLayout.panelFillerHeight())));
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, tooltipBox.x - sc(normalLayout.infoPanelInsetX()), panel.y + tooltipBox.getHeight() - sc(normalLayout.infoPanelBottomTrim()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.infoPanelWidthTrim())), Math.max(1, sc(normalLayout.infoPanelHeight())));
        blitSprite(g, PACK_NAME_BOX, tooltipBox.x - sc(normalLayout.packNameInsetX()), panel.y + sc(normalLayout.packNameTop()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.packNameWidthTrim())), Math.max(1, sc(normalLayout.packNameHeight())));
        blitSprite(g, SKIN_BOX, tooltipBox.x - sc(normalLayout.skinBoxInsetX()), panel.y + sc(normalLayout.skinBoxTop()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.skinBoxWidthTrim())), Math.max(1, tooltipBox.getHeight() - sc(normalLayout.skinBoxBottomTrim())));

        int holder    = Math.max(1, sc(normalLayout.actionHolderSize()));
        int iconX     = tooltipBox.x + tooltipBox.getWidth() - sc(normalLayout.actionHolderXOffset());
        int iconBaseY = panel.y + tooltipBox.getHeight() - sc(normalLayout.actionHolderBaseY());
        blitSprite(g, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder, holder);
        blitSprite(g, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(normalLayout.actionHolderGap()), holder, holder);

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String selected     = playerSkinWidgetList.element3.skinId.get();
            UUID   self         = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String current      = self == null ? null : ClientSkinCache.get(self);
            boolean isAuto      = "auto_select".equals(selected);
            boolean isAutoActive = current == null || current.isBlank();

            
            
            
            if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
                renderSelectedSkinMarker(g, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
            }

            if (selected != null && FavoritesStore.isFavorite(selected)) {
                renderFavoriteHeart(g, iconX, iconBaseY + sc(normalLayout.actionHolderGap()), holder);
            }
        }

        
        
        
        int clipLeft = carouselClipLeft();
        int clipTop = tooltipContentTop();
        int clipRight = carouselClipRight();
        int clipBottom = tooltipContentBottom() - sc(getLayoutMetrics().carouselClipBottomTrim());
        PlayerSkinWidget.setCarouselClip(clipLeft, clipTop, clipRight, clipBottom);
        PlayerSkinWidget.setCarouselYawDenom(Math.max(1f, 240f * uiScale));

        
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String    skinId = playerSkinWidgetList.element3.skinId.get();
            SkinEntry entry  = skinId == null ? null : SkinPackLoader.getSkin(skinId);
            String    name   = entry == null ? String.valueOf(skinId) : entry.name();
            int mid       = tooltipBox.x - sc(normalLayout.infoCenterInsetX()) + (tooltipBox.getWidth() - sc(normalLayout.infoCenterWidthTrim())) / 2;
            int skinNameY = panel.y + tooltipBox.getHeight() - sc(normalLayout.skinNameBottomTrim());
            drawBigCentered(g, Component.literal(name), mid, skinNameY, 0xFFFFFFFF);

            ResourceLocation modelId = null;
            try {
                String ns = entry != null && entry.texture() != null ? entry.texture().getNamespace() : SkinSync.ASSET_NS;
                modelId = ResourceLocation.fromNamespaceAndPath(ns, skinId);
            } catch (Throwable ignored) {}

            String theme = modelId == null ? null : BoxModelManager.getThemeText(modelId);
            if (theme != null && !theme.isBlank() && !theme.equals(name)) {
                float scale = bigTextScale();
                int maxUnscaled = (int) ((tooltipBox.getWidth() - sc(normalLayout.themeTextWidthTrim())) / scale);
                String show = theme;
                show = show.replace("\u00E2\u20AC\u00A6", "...");
                if (minecraft.font.width(show) > maxUnscaled) { int ellW = minecraft.font.width("…"); show = minecraft.font.plainSubstrByWidth(show, Math.max(0, maxUnscaled - ellW)) + "…"; }
                show = show.replace("\u00E2\u20AC\u00A6", "...");
                int themeY = skinNameY + (int) (minecraft.font.lineHeight * scale) + sc(normalLayout.themeTextGap());
                drawBigCentered(g, Component.literal(show), mid, Math.min(themeY, panel.y + tooltipBox.getHeight() - sc(normalLayout.themeBottomInset())), 0xFFFFFFFF);
            }
        }

        
        SkinPack pack = getFocusedPack();
        int packMid = tooltipBox.x - sc(normalLayout.infoCenterInsetX()) + (tooltipBox.getWidth() - sc(normalLayout.infoCenterWidthTrim())) / 2;
        int packMetaY = panel.y + sc(normalLayout.packTitleTop()) + (int) (minecraft.font.lineHeight * bigTextScale()) + sc(normalLayout.packMetaGap());
        if (pack != null) {
            drawBigCentered(g, Component.literal(SkinPackLoader.nameString(pack.name(), pack.id())), packMid, panel.y + sc(normalLayout.packTitleTop()), 0xFFFFFFFF);
            String t = pack.type();
            if (t != null && !t.isBlank()) {
                String k = t.toLowerCase(Locale.ROOT);
                Component label = null;
                if (k.equals("skin"))   label = Component.translatable("legacy.skinpack.type.skin");
                if (k.equals("mashup")) label = Component.translatable("legacy.skinpack.type.mashup");
                if (label != null) {
                    drawSmallCentered(g, label, packMid, packMetaY, 0xCCFFFFFF);
                    packMetaY += Math.max(sc(normalLayout.packTypeAdvance()), (int) (minecraft.font.lineHeight * smallTextScale()));
                }
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
        addPreviewControlTooltips(r);
        if (showExpandedControlTooltips()) {
            r.add(() -> ControlType.getActiveType().isKbm()
                    ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_W), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_A), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_S), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_D)})
                    : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.literal("Navigate"));
            if (canUsePackFilter()) {
                r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_T) : ControllerBinding.BACK.bindingState.getIcon(), this::currentPackFilterLabel);
            }
            r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> Component.literal("Advanced Options"));
        }
    }

    

    @Override
    public void removed() {
        stopHoldingPackStick();
        super.removed();
    }
}
