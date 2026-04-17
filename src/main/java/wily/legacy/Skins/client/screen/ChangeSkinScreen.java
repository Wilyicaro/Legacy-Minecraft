package wily.legacy.Skins.client.screen;
import com.mojang.blaze3d.platform.InputConstants;
import wily.legacy.Skins.client.changeskin.*;
import wily.legacy.Skins.client.preview.*;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.*;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;
public class ChangeSkinScreen extends AbstractChangeSkinScreen {
    private static final float NORMAL_CAROUSEL_BASE_SCALE = 0.935f;
    private static final float NORMAL_CAROUSEL_BASE_SPACING = 80f;
    private static final float NORMAL_DOLL_SCALE_BUMP = 1.05f;
    private static final float COMPACT_DOLL_SCALE_MULTIPLIER = 0.85f;
    private static final float COMPACT_CAROUSEL_SPACING_MULTIPLIER = 0.78f;
    private static final int PACK_LIST_VISIBLE_ROWS = 6;
    private static final int COMPACT_PACK_LIST_VISIBLE_ROWS = 5;
    private static final int PACK_LIST_FOOTER_RESERVE = 12;
    private static final float HD_MENU_SCALE = 1.10f;
    private static final ChangeSkinScreenLayout HD_LAYOUT = new ChangeSkinScreenLayout(
            false, hd(180), hd(290), hd(400),
            hd(24), hd(112), hd(34), hd(20), hd(7),
            hd(10), hd(5), hd(5), hd(5), hd(18), hd(10),
            1.485f, 0.65f, 1.045f, 0.60f,
            ChangeSkinLayoutMetrics.DEFAULT
    );
    private record NormalLayoutMetrics(
            int skinPanelInsetX, int skinPanelTop, int panelFillerInsetX, int panelFillerTop, int panelFillerBottomTrim, int panelFillerWidthTrim,
            int panelFillerHeight, int infoPanelInsetX, int infoPanelBottomTrim, int infoPanelWidthTrim, int infoPanelHeight, int packNameInsetX,
            int packNameTop, int packNameWidthTrim, int packNameHeight, int skinBoxInsetX, int skinBoxTop, int skinBoxWidthTrim, int skinBoxBottomTrim,
            int actionHolderSize, int actionHolderXOffset, int actionHolderBaseY, int actionHolderTopOffset, int actionHolderGap, int packFrameInsetX,
            int packFrameTop, int packFrameWidthTrim, int packFrameBottomTrim, int packListInsetX, int packListWidthTrim, int packListTop,
            int packListBottomInset, int previewListGap, int scrollArrowOffset, int infoCenterInsetX, int infoCenterWidthTrim, int skinNameBottomTrim,
            int themeTextWidthTrim, int themeTextGap, int themeBottomInset, int packTitleTop, int packMetaGap, int packTypeAdvance
    ) {
        static final NormalLayoutMetrics DEFAULT = new NormalLayoutMetrics(10, 7, 5, 16, 80, 14, 60, 1, 59, 55, 55, 5, 20, 18, 40, 5, 16, 14, 80, 24, 50, 60, 3, 30, 12, 137, 24, 147, 16, 32, 140, 20, 2, 8, 5, 18, 49, 26, 6, 12, 27, 8, 10);
    }
    private static final int BEACON_CHECK_VISIBLE_X = 3, BEACON_CHECK_VISIBLE_Y = 4,
            BEACON_CHECK_TEXTURE_SIZE = 28, BEACON_CHECK_VISIBLE_W = 24, BEACON_CHECK_VISIBLE_H = 20,
            SKIN_TICK_W = 12, SKIN_TICK_H = 10,
            PADLOCK_TEXTURE_SIZE = 32, HEART_TEXTURE_SIZE = 9, SELECTION_ICON_SIZE = 16, PACK_BUTTON_BASE_HEIGHT = 20;
    private static final float BEACON_CHECK_CENTER_X = 14.0f;
    private static final float BEACON_CHECK_CENTER_Y = 12.5f;
    private static final float TICK_HOLDER_OFFSET_X = 1.0f / 118.0f;
    private static final float TICK_HOLDER_OFFSET_Y = -3.0f / 117.0f;
    private static final float HEART_HOLDER_OFFSET_X = -0.5f / 118.0f;
    private static final float HEART_HOLDER_OFFSET_Y = -2.0f / 117.0f;
    private static final ResourceLocation
            SKIN_PANEL = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_panel"),
            PANEL_FILLER = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/panel_filler"),
            PACK_NAME_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/pack_name_box"),
            SKIN_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_box"),
            SIZEABLE_ICON_HOLDER = ResourceLocation.fromNamespaceAndPath("legacy", "container/sizeable_icon_holder"),
            BEACON_CHECK = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/beacon_check.png"),
            PADLOCK = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/padlock.png"),
            HEART_CONTAINER = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container"),
            HEART_FULL = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");
    private final HoldRepeat packHold = new HoldRepeat();
    private final NormalLayoutMetrics normalLayout = NormalLayoutMetrics.DEFAULT;
    private int resolvedPackRowHeight = PACK_BUTTON_BASE_HEIGHT;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    public ChangeSkinScreen(Screen parent) { super(parent); }

    public ChangeSkinScreen(Screen parent, ChangeSkinScreenSource source) { super(parent, source); }
    private static int hd(int value) { return Math.max(1, Math.round(value * HD_MENU_SCALE)); }
    @Override
    protected ChangeSkinScreenLayout resolveRuntimeLayout() { return isCompact480() ? ChangeSkinScreenLayout.DEFAULT : HD_LAYOUT; }
    private void refreshNormalSdFit() {
        if (isCompact480()) {
            uiScale = Math.min(1f, uiScale * 1.10f);
        } else { uiScale = Math.min(1f, uiScale * 1.14f); }
        tooltipWidth = Math.max(1, Math.round(layoutProfile.baseTooltipWidth() * uiScale));
    }
    private void applyNormalCarouselTuning(boolean relayout) {
        if (playerSkinWidgetList == null) return;
        float scaleMultiplier = getLayoutMetrics().centerScale() / NORMAL_CAROUSEL_BASE_SCALE;
        float spacingMultiplier = getLayoutMetrics().carouselOffset() / NORMAL_CAROUSEL_BASE_SPACING;
        if (scaleMultiplier <= 0f) scaleMultiplier = 1f;
        if (spacingMultiplier <= 0f) spacingMultiplier = 1f;
        scaleMultiplier *= 1.10f * NORMAL_DOLL_SCALE_BUMP;
        if (isCompact480()) scaleMultiplier *= COMPACT_DOLL_SCALE_MULTIPLIER;
        spacingMultiplier *= 1.16f;
        if (isCompact480()) spacingMultiplier *= COMPACT_CAROUSEL_SPACING_MULTIPLIER;
        playerSkinWidgetList.setRenderRadius(2);
        playerSkinWidgetList.setCarouselTuning(scaleMultiplier, spacingMultiplier);
        if (relayout) playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }

    private int visiblePackRows() { return isCompact480() ? COMPACT_PACK_LIST_VISIBLE_ROWS : PACK_LIST_VISIBLE_ROWS; }

    private int resolvePackRowHeight() {
        int scaledHeight = Math.max(10, Math.round(PACK_BUTTON_BASE_HEIGHT * uiScale));
        int availableHeight = resolvePackListAvailableHeight();
        int fittedHeight = Math.max(10, (availableHeight - PACK_LIST_FOOTER_RESERVE) / visiblePackRows());
        if (isCompact480()) return fittedHeight;
        return Math.min(fittedHeight, Math.max(18, scaledHeight));
    }

    private int resolvePackListAvailableHeight() {
        int minY = Math.max(panel.y + sc(normalLayout.packListTop()), previewBoxY() + previewBoxSize() + sc(normalLayout.previewListGap()));
        return Math.max(1, panel.y + panel.height - sc(normalLayout.packListBottomInset()) - minY);
    }

    private int adjustPackListHeight(int height) {
        int visibleRowsHeight = resolvedPackRowHeight * visiblePackRows();
        return Math.max(1, Math.min(height, visibleRowsHeight + PACK_LIST_FOOTER_RESERVE));
    }
    private int centerTextX() {
        if (playerSkinWidgetList != null) return playerSkinWidgetList.getCenterAnchorX();
        return tooltipBox.x - sc(normalLayout.infoCenterInsetX()) + (tooltipBox.getWidth() - sc(normalLayout.infoCenterWidthTrim())) / 2;
    }

    private int compactPreviewBackgroundInset() { return isCompact480() ? sc(8) : 0; }

    private float mainTextScale() { return bigTextScale(); }

    private float packTypeTextScale() { return Math.min(1.0f, smallTextScale()); }

    private Component packLabel(SkinPack pack) {
        if (isReorderingCustomPack()) return Component.translatable("legacy.menu.reorder_custom_skin_pack");
        if (isEditingCustomPack()) return Component.translatable("legacy.menu.edit_custom_skin_pack_skins");
        return packSubtitle(pack);
    }
    @Override
    protected int previewBoxX() {
        int size = previewBoxSize();
        return panel.x + Math.max(sc(7), (panel.width - size) / 2);
    }
    @Override
    protected void onWidgetListCreated(PlayerSkinWidgetList list) { applyNormalCarouselTuning(false); }
    @Override
    protected void onAfterSkinPackChanged() { applyNormalCarouselTuning(true); }
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
    protected boolean insideScrollRegion(double mx, double my) { return inside(mx, my, tooltipBox.x, tooltipBox.y, tooltipBox.getWidth(), tooltipBox.getHeight()); }
    private int carouselClipLeft() {
        int clipLeft = tooltipBox.x + sc(getLayoutMetrics().carouselClipInset());
        if (isCompact480()) clipLeft += sc(6);
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
        int frameX = panel.x + sc(normalLayout.packFrameInsetX());
        int frameY = panel.y + sc(normalLayout.packFrameTop());
        int frameW = Math.max(1, panel.width - sc(normalLayout.packFrameWidthTrim()));
        int frameH = Math.max(1, panel.height - sc(normalLayout.packFrameBottomTrim()));
        int arrowWidth = isCompact480() ? 11 : 14;
        int arrowHeight = isCompact480() ? 5 : 7;
        int arrowOffsetX = isCompact480() ? 0 : -2;
        int arrowOffsetY = isCompact480() ? 8 : -2;
        int arrowTop = frameY + frameH - sc(6) - arrowHeight;
        int x = frameX + sc(4);
        int w = Math.max(1, frameW - sc(8));
        packList.refreshPackIdsIfNeeded();
        packList.setReorderMode(isReorderingCustomPack());
        int visibleRows = visiblePackRows();
        int maxListBottom = arrowTop - sc(4);
        int rowHeight = Math.max(10, resolvedPackRowHeight);
        int h = rowHeight * visibleRows + PACK_LIST_FOOTER_RESERVE;
        int y = maxListBottom - rowHeight * visibleRows;
        int packFrameRenderX = frameX - sc(2);
        int packFrameRenderY = isCompact480() ? y - sc(3) : y - sc(4);
        int packFrameRenderW = frameW + sc(4);
        int packFrameRenderH = frameY + frameH - packFrameRenderY;
        int packIconTop = panel.y + sc(normalLayout.skinPanelTop());
        int packIconBottom = packFrameRenderY;
        int packIconAvailableHeight = Math.max(1, packIconBottom - packIconTop);
        int packIconSize = Math.max(1, Math.round(Math.min(panel.width - sc(12), packIconAvailableHeight) * 0.8925f));
        int packIconY = packIconTop + Math.max(0, (packIconAvailableHeight - packIconSize) / 2) - sc(3);
        int packIconX = panel.x + Math.max(0, (panel.width - packIconSize) / 2);
        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, packIconX, packIconY, packIconSize, packIconSize));
        addRenderableOnly((g, i, j, f) -> {
            SkinPack pack = packList.getFocusedPack();
            ResourceLocation icon = pack == null ? null : pack.icon();
            if (icon == null) return;
            int innerInset = 2;
            int innerX = packIconX + innerInset;
            int innerY = packIconY + innerInset;
            int innerSize = Math.max(1, packIconSize - innerInset * 2);
            int[] d = packIconDims(icon);
            float scale = Math.min(innerSize / (float) d[0], innerSize / (float) d[1]);
            scale += 1f / Math.max(d[0], d[1]);
            float scaleY = scale + 0.25f / d[1];
            float cx = innerX + innerSize / 2f;
            float cy = innerY + innerSize / 2f - 0.125f;
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx, cy);
            pose.scale(scale, scaleY);
            pose.translate(-d[0] / 2f, -d[1] / 2f);
            g.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0, 0, d[0], d[1], d[0], d[1]);
            pose.popMatrix();
        });

        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, packFrameRenderX, packFrameRenderY, packFrameRenderW, packFrameRenderH));
        packList.applyResolvedButtonHeight(rowHeight);
        getRenderableVList().renderables.clear();
        if (packList.getPackCount() == 0) {
            getRenderableVList().addRenderable(new ChangeSkinPackList.PackButton(packList, -1, packList.getWrappedLabelForIndex(0), packList.getButtonHeight()));
        } else for (int i = 0; i < packList.getPackCount(); i++) {
            getRenderableVList().addRenderable(new ChangeSkinPackList.PackButton(packList, i, packList.getLabelForIndex(i), packList.getButtonHeight()));
        }
        getRenderableVList().verticalScrollArrowSize(arrowWidth, arrowHeight);
        getRenderableVList().verticalScrollArrowOffset(arrowOffsetX, arrowOffsetY);
        getRenderableVList().scrollArrowYOffset(arrowTop - (y + h) - arrowOffsetY);
        getRenderableVList().init("consoleskins.packList", x, y, w, h);
    }
    @Override
    protected void panelInit() {
        boolean windowResized = width != lastLayoutWidth || height != lastLayoutHeight;
        refreshSharedLayout();
        refreshNormalSdFit();
        renderableVList.layoutSpacing(l -> 0);
        if (windowResized) { getRenderableVList().resetScroll(); }
        addRenderableOnly(panel);
        panel.init();
        applyResolvedPanelBounds();
        resolvedPackRowHeight = resolvePackRowHeight();
        packList.applyResolvedButtonHeight(resolvedPackRowHeight);
        tooltipBox.init("tooltipBox");
        lastLayoutWidth = width;
        lastLayoutHeight = height;
    }
    @Override
    protected boolean handlePackListStepNavigation(int key) {
        return handlePackListStepNavigation(key, true, true, true, false);
    }
    @Override
    protected void focusPackListItem(Object item) {
        RenderableVList vList = getRenderableVList();
        if (vList == null || item == null) return;
        if (item instanceof ChangeSkinPackList.PackButton btn) {
            if (btn.getPackIndex() < 0) return;
            vList.focusRenderable(btn);
            return;
        }
        super.focusPackListItem(item);
    }
    private ChangeSkinPackList.PackButton pickPackButton(double mx, double my) {
        for (var renderable : getRenderableVList().renderables) {
            if (!(renderable instanceof ChangeSkinPackList.PackButton button)) continue;
            if (!button.visible || button.getPackIndex() < 0) continue;
            if (inside(mx, my, button.getX(), button.getY(), button.getWidth(), button.getHeight())) return button;
        }
        return null;
    }
    private void syncPackFocus() {
        if (isReorderingCustomPack()) {
            String packId = customPacks.reorderingPackId();
            if (packId != null) packList.focusPackId(packId, false);
        }
        ChangeSkinPackList.PackButton target = findPackButton(packList.getFocusedPackIndex());
        if (target == null) return;
        var f = getFocused();
        if ((f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton) && f != target)
            focusPackListItem(target);
    }
    private void startHoldingPackStick(int dir) {
        packHold.start(dir);
        if (isReorderingCustomPack()) customPacks.moveReorderingPack(packHold.dir());
        else if (focusRelativePack(packHold.dir(), true)) applyQueuedPackChange();
    }
    private void stopHoldingPackStick() { packHold.stop(); }
    private void pumpHoldingPackStick() {
        if (!packHold.ready()) return;
        if (isReorderingCustomPack()) customPacks.moveReorderingPack(packHold.dir());
        else if (focusRelativePack(packHold.dir(), true)) applyQueuedPackChange();
        packHold.step();
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (handleCarouselMouseClicked(e, bl)) return true;
        double mx = e.x(), my = e.y();
        if (isReorderingCustomPack() && e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            ChangeSkinPackList.PackButton packButton = pickPackButton(mx, my);
            if (packButton != null) {
                customPacks.moveReorderingPackTo(packButton.getPackIndex());
                return true;
            }
        }
        int holder = Math.max(1, sc(normalLayout.actionHolderSize()));
        int iconX  = tooltipBox.x + tooltipBox.getWidth() - sc(normalLayout.actionHolderXOffset());
        int iconY  = panel.y + tooltipBox.getHeight() - sc(normalLayout.actionHolderBaseY());
        if (inside(mx, my, iconX, iconY + sc(normalLayout.actionHolderTopOffset()), holder, holder)) { selectSkin(); return true; }
        if (inside(mx, my, iconX, iconY + sc(normalLayout.actionHolderGap()), holder, holder)) { favoriteSkin(); return true; }
        boolean handled = super.mouseClicked(e, bl);
        if (handled) applyQueuedPackChange();
        return handled;
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
            else if (ay >= triggerY) { int dir = sy < 0 ? -1 : 1; if (!packHold.active() || packHold.dir() != dir) startHoldingPackStick(dir); }
            pumpHoldingPackStick();
            if (Math.abs(sx) <= sideLimit && ay >= releaseY) { state.block(); return; }
        }
        super.bindingStateTick(state);
    }
    @Override
    public void tick() {
        super.tick();
        syncPackFocus();
        if (tickScreenTail()) return;
        pumpHoldingPackStick();
    }
    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);
        int packNameX = tooltipBox.x - sc(normalLayout.packNameInsetX());
        int packNameY = panel.y + sc(normalLayout.packNameTop());
        int packNameW = Math.max(1, tooltipBox.getWidth() - sc(normalLayout.packNameWidthTrim()));
        int packNameH = Math.max(1, sc(normalLayout.packNameHeight()));
        int packNameRenderH = packNameH + Math.max(1, Math.round(packNameH * 0.10f)) + sc(6);
        int compactPreviewInset = compactPreviewBackgroundInset();
        blitSprite(g, SKIN_PANEL, tooltipBox.x - sc(normalLayout.skinPanelInsetX()) - compactPreviewInset, panel.y + sc(normalLayout.skinPanelTop()), Math.max(1, tooltipBox.getWidth() + compactPreviewInset), Math.max(1, tooltipBox.getHeight() - sc(2)));
        blitSprite(g, PANEL_FILLER, tooltipBox.x - sc(normalLayout.panelFillerInsetX()), panel.y + sc(normalLayout.panelFillerTop()) + tooltipBox.getHeight() - sc(normalLayout.panelFillerBottomTrim()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.panelFillerWidthTrim())), Math.max(1, sc(normalLayout.panelFillerHeight())));
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, tooltipBox.x - sc(normalLayout.infoPanelInsetX()), panel.y + tooltipBox.getHeight() - sc(normalLayout.infoPanelBottomTrim()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.infoPanelWidthTrim())), Math.max(1, sc(normalLayout.infoPanelHeight())));
        blitSprite(g, PACK_NAME_BOX, packNameX, packNameY, packNameW, packNameRenderH);
        g.fill(packNameX, packNameY, packNameX + packNameW, packNameY + packNameRenderH, 0x66000000);
        blitSprite(g, SKIN_BOX, tooltipBox.x - sc(normalLayout.skinBoxInsetX()) - compactPreviewInset, panel.y + sc(normalLayout.skinBoxTop()), Math.max(1, tooltipBox.getWidth() - sc(normalLayout.skinBoxWidthTrim()) + compactPreviewInset), Math.max(1, tooltipBox.getHeight() - sc(normalLayout.skinBoxBottomTrim())));
        int holder    = Math.max(1, sc(normalLayout.actionHolderSize()));
        int iconX     = tooltipBox.x + tooltipBox.getWidth() - sc(normalLayout.actionHolderXOffset());
        int iconBaseY = panel.y + tooltipBox.getHeight() - sc(normalLayout.actionHolderBaseY());
        blitSprite(g, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder, holder);
        blitSprite(g, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(normalLayout.actionHolderGap()), holder, holder);
        PlayerSkinWidget center = getCenterWidget();
        if (center != null) {
            String selected     = center.skinId.get();
            String current      = currentAppliedSkinId();
            boolean isAuto      = SkinIdUtil.isAutoSelect(selected);
            boolean isAutoActive = current == null || current.isBlank();
            boolean isImport = customPacks.isImportSkinSelection(selected);
            boolean reordering = isReorderingCustomPack();
            boolean editing = isEditingCustomPack();
            boolean locked = customPacks.isLockedSkinSelection(selected);

            if (reordering) {
                drawActionSprite(g, LegacySprites.BEACON_CONFIRM, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
            } else if (editing) {
                if (isImport || locked) drawPadlock(g, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
                else if (selected != null) drawActionSprite(g, LegacySprites.BEACON_CONFIRM, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
                if (!isImport && !locked && selected != null) drawActionSprite(g, LegacySprites.ERROR_CROSS, iconX, iconBaseY + sc(normalLayout.actionHolderGap()), holder);
            } else if (isImport) drawPadlock(g, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
            else if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
                drawTick(g, iconX, iconBaseY + sc(normalLayout.actionHolderTopOffset()), holder);
            }
            if (!editing && !isImport && isSkinFavorite(selected)) {
                int iconY = iconBaseY + sc(normalLayout.actionHolderGap());
                float heartSize = Math.max(1f, holder - 8f);
                float heartScaleX = heartSize / HEART_TEXTURE_SIZE;
                float heartScaleY = (heartSize + 1.0f) / HEART_TEXTURE_SIZE;
                float centerX = iconX + holder / 2.0f + holder * HEART_HOLDER_OFFSET_X;
                float centerY = iconY + holder / 2.0f + holder * HEART_HOLDER_OFFSET_Y;
                float heartX = centerX - (HEART_TEXTURE_SIZE * heartScaleX) / 2.0f;
                float heartY = centerY - (HEART_TEXTURE_SIZE * heartScaleY) / 2.0f - 0.5f;
                g.pose().pushMatrix();
                g.pose().translate(heartX, heartY);
                g.pose().scale(heartScaleX, heartScaleY);
                blitSprite(g, HEART_CONTAINER, 0, 0, HEART_TEXTURE_SIZE, HEART_TEXTURE_SIZE);
                blitSprite(g, HEART_FULL, 0, 0, HEART_TEXTURE_SIZE, HEART_TEXTURE_SIZE);
                g.pose().popMatrix();
            }
        }
        int clipLeft = carouselClipLeft();
        int clipTop = tooltipContentTop();
        int clipRight = carouselClipRight();
        int clipBottom = tooltipContentBottom() - sc(getLayoutMetrics().carouselClipBottomTrim());
        PlayerSkinWidget.setCarouselClip(clipLeft, clipTop, clipRight, clipBottom);
        center = getCenterWidget();
        if (center != null) {
            String    skinId = center.skinId.get();
            SkinEntry entry  = skinId == null ? null : source.skin(skinId);
            String    name   = SkinIdUtil.isAutoSelect(skinId) ? "Auto Selected" : entry == null ? String.valueOf(skinId) : source.skinName(entry);
            int mid       = centerTextX();
            int skinNameY = panel.y + tooltipBox.getHeight() - sc(normalLayout.skinNameBottomTrim());
            float mainTextScale = mainTextScale();
            int maxNameWidth = Math.max(1, (int) ((tooltipBox.getWidth() - sc(normalLayout.themeTextWidthTrim())) / mainTextScale));
            drawScaledCentered(g, Component.literal(PlayerSkinWidget.clipText(minecraft.font, name, maxNameWidth)), mid, skinNameY, LegacyRenderUtil.getDefaultTextColor(true), mainTextScale, true);

            ResourceLocation modelId = entry == null ? null : entry.modelId();
            if (modelId == null && entry != null && entry.texture() != null) modelId = ClientSkinAssets.getModelIdFromTexture(entry.texture());

            String theme = modelId == null ? null : BoxModelManager.getThemeText(modelId);
            if (theme != null && !theme.isBlank() && !theme.equals(name)) {
                int maxThemeWidth = Math.max(1, (int) ((tooltipBox.getWidth() - sc(normalLayout.themeTextWidthTrim())) / mainTextScale));
                String show = PlayerSkinWidget.clipText(minecraft.font, theme, maxThemeWidth);
                int themeY = skinNameY + (int) (minecraft.font.lineHeight * mainTextScale) + sc(normalLayout.themeTextGap());
                drawScaledCentered(g, Component.literal(show), mid, Math.min(themeY, panel.y + tooltipBox.getHeight() - sc(normalLayout.themeBottomInset())), LegacyRenderUtil.getDefaultTextColor(true), mainTextScale, true);
            }
        }
        SkinPack pack = packList.getFocusedPack();
        int packMid = centerTextX();
        int packTitleY = panel.y + sc(normalLayout.packTitleTop());
        float mainTextScale = mainTextScale();
        int packMetaY = packTitleY + (int) (minecraft.font.lineHeight * mainTextScale) + sc(normalLayout.packMetaGap());
        if (pack != null) {
            int maxPackWidth = Math.max(1, (int) ((tooltipBox.getWidth() - sc(normalLayout.packNameWidthTrim())) / mainTextScale));
            String packName = PlayerSkinWidget.clipText(minecraft.font, source.packName(pack), maxPackWidth);
            drawScaledCentered(g, Component.literal(packName), packMid, packTitleY, LegacyRenderUtil.getDefaultTextColor(true), mainTextScale, true);
            Component label = packLabel(pack);
            if (label != null) drawScaledCentered(g, label, packMid, packMetaY, ColorUtil.withAlpha(LegacyRenderUtil.getDefaultTextColor(true), 0.8f), packTypeTextScale(), true);
        }
    }
    @Override
    public void addControlTooltips(ControlTooltip.Renderer r) {
        addCommonControlTooltips(
                r,
                ControlTooltip.POINTER_MOVEMENT::get,
                () -> Component.literal("Navigate")
        );
    }
    @Override
    public void removed() {
        stopHoldingPackStick();
        super.removed();
    }
    private void drawTick(GuiGraphics guiGraphics, int iconX, int iconY, int holder) {
        float targetSize = Math.max(1f, sc(SELECTION_ICON_SIZE) * 0.8f + 2.0f);
        float scale = Math.min(targetSize / BEACON_CHECK_VISIBLE_W, targetSize / BEACON_CHECK_VISIBLE_H);
        float centerX = iconX + holder / 2.0f + holder * TICK_HOLDER_OFFSET_X;
        float centerY = iconY + holder / 2.0f + holder * TICK_HOLDER_OFFSET_Y;
        float visibleCenterX = centerX + (BEACON_CHECK_VISIBLE_X + BEACON_CHECK_VISIBLE_W / 2.0f - BEACON_CHECK_CENTER_X) * scale;
        float visibleCenterY = centerY + (BEACON_CHECK_VISIBLE_Y + BEACON_CHECK_VISIBLE_H / 2.0f - BEACON_CHECK_CENTER_Y) * scale;
        int left = Math.round(visibleCenterX - SKIN_TICK_W / 2.0f);
        int top = Math.round(visibleCenterY - SKIN_TICK_H / 2.0f);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BEACON_CHECK, left, top,
                (float) BEACON_CHECK_VISIBLE_X, (float) BEACON_CHECK_VISIBLE_Y,
                SKIN_TICK_W, SKIN_TICK_H,
                BEACON_CHECK_VISIBLE_W, BEACON_CHECK_VISIBLE_H,
                BEACON_CHECK_TEXTURE_SIZE, BEACON_CHECK_TEXTURE_SIZE);
    }
    private void drawPadlock(GuiGraphics guiGraphics, int iconX, int iconY, int holder) {
        float size = Math.max(1f, holder - 8f);
        float scale = size / PADLOCK_TEXTURE_SIZE;
        float left = iconX + (holder - PADLOCK_TEXTURE_SIZE * scale) / 2.0f;
        float top = iconY + (holder - PADLOCK_TEXTURE_SIZE * scale) / 2.0f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(left, top);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PADLOCK, 0, 0, 0, 0, PADLOCK_TEXTURE_SIZE, PADLOCK_TEXTURE_SIZE, PADLOCK_TEXTURE_SIZE, PADLOCK_TEXTURE_SIZE);
        guiGraphics.pose().popMatrix();
    }
    private void drawActionSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int iconX, int iconY, int holder) {
        int size = Math.max(1, holder - 8);
        int left = iconX + (holder - size) / 2;
        int top = iconY + (holder - size) / 2;
        blitSprite(guiGraphics, sprite, left, top, size, size);
    }
}
