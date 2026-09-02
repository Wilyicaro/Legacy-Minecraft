package wily.legacy.skins.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.skins.client.changeskin.ChangeSkinPackList;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.skins.client.preview.PlayerSkinWidgetList;
import wily.legacy.skins.skin.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

public class ChangeSkinScreen extends AbstractChangeSkinScreen {
    private static final int PACK_LIST_FOOTER_RESERVE = 12;
    private static final int PACK_BUTTON_BORDER_OVERLAP = 1;
    private static final int PADLOCK_TEXTURE_SIZE = 32;
    private static final int HEART_TEXTURE_SIZE = 9;
    private static final int PACK_BUTTON_BASE_HEIGHT = 20;

    private static final ReferenceSize FRAME_TEXTURE_SIZE = new ReferenceSize(1125, 818);
    private static final int PREVIEW_OPENING_RIGHT = 1095;
    private static final int PREVIEW_OPENING_TOP = 30;
    private static final int PREVIEW_OPENING_BOTTOM = 632;
    private static final int FOOTER_OPENING_TOP = 641;
    private static final int FOOTER_OPENING_BOTTOM = 809;

    private static final ReferenceSize LAYOUT_REFERENCE_SIZE = new ReferenceSize(1130, 822);
    private static final int DETAILS_PANEL_X_OFFSET = -5;
    private static final int DETAILS_PANEL_WIDTH = 1026;
    private static final int DETAILS_PANEL_HEIGHT = 151;
    private static final int ACTION_HOLDER_GAP = 7;
    private static final int ACTION_HOLDER_RIGHT_INSET = 28;

    private static final int FRAME_TOP_LIFT = 1;
    private static final int FRAME_BOTTOM_LIFT = 1;
    private static final int DETAILS_PANEL_VERTICAL_INSET = 2;

    private static final float PACK_HEADER_HEIGHT_OVERSCAN_RATIO = 0.10f;
    private static final int PACK_HEADER_BOTTOM_PADDING = 6;
    private static final int SKIN_PREVIEW_FILL_COLOR = 0x46000000;
    private static final int PACK_HEADER_FILL_COLOR = 0xAF000000;
    private static final int DETAILS_PANEL_FILL_COLOR = 0xFFA6A6A6;
    private static final float HEART_HOLDER_OFFSET_X = -0.5f / 118.0f;
    private static final float HEART_HOLDER_OFFSET_Y = -2.0f / 117.0f;
    private static final Identifier
            CHANGE_SKIN_FRAME = Identifier.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/change_skin_frame"),
            PADLOCK_TEXTURE = Identifier.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/padlock.png"),
            HEART_CONTAINER = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container"),
            HEART_FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");

    private static final int FRAME_TOP = 7;
    private static final int PACK_HEADER_TOP = 20;
    private static final int PACK_TEXT_WIDTH_TRIM = 18;
    private static final int PACK_HEADER_HEIGHT = 40;
    private static final int PACK_FRAME_INSET_X = 12;
    private static final int PACK_FRAME_TOP = 137;
    private static final int PACK_FRAME_WIDTH_TRIM = 24;
    private static final int PACK_FRAME_BOTTOM_TRIM = 147;
    private static final int PACK_LIST_TOP = 140;
    private static final int PACK_LIST_BOTTOM_INSET = 20;
    private static final int PREVIEW_LIST_GAP = 2;
    private static final int TEXT_CENTER_INSET_X = 5;
    private static final int TEXT_CENTER_WIDTH_TRIM = 18;
    private static final int SKIN_NAME_BOTTOM_TRIM = 49;
    private static final int SELECTED_SKIN_TEXT_Y_OFFSET = 5;
    private static final int THEME_TEXT_WIDTH_TRIM = 26;
    private static final int THEME_TEXT_GAP = 6;
    private static final int THEME_BOTTOM_INSET = 12;
    private static final int PACK_TITLE_TOP = 27;
    private static final int PACK_META_GAP = 8;
    private final HoldRepeat packHold = new HoldRepeat();
    private int resolvedPackRowHeight = PACK_BUTTON_BASE_HEIGHT;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;

    public ChangeSkinScreen(Screen parent) {
        super(parent);
    }

    public ChangeSkinScreen(Screen parent, ChangeSkinScreenSource source) {
        super(parent, source);
    }

    private void applyScreenScaleAdjustment() {
        uiScale = Math.min(1f, uiScale * accessor.getFloat("layout.scaleMultiplier", 1.10f));
        tooltipWidth = Math.max(1, Math.round(layoutProfile.baseTooltipWidth() * uiScale));
    }

    private void applyCarouselTuning(boolean relayout) {
        if (playerSkinWidgetList == null) return;
        playerSkinWidgetList.setRenderRadius(accessor.getInteger("carousel.renderRadius", 2));
        playerSkinWidgetList.setCarouselLayout(resolveCarouselLayout());
        playerSkinWidgetList.setCarouselTuning(
                accessor.getFloat("carousel.scaleMultiplier", 1.155f),
                accessor.getFloat("carousel.spacingMultiplier", 1.16f)
        );
        if (relayout) playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }

    private PlayerSkinWidgetList.CarouselLayout resolveCarouselLayout() {
        PlayerSkinWidgetList.CarouselLayout fallback = PlayerSkinWidgetList.CarouselLayout.DEFAULT;
        return new PlayerSkinWidgetList.CarouselLayout(
                accessor.getFloat("carousel.baseYOffset", fallback.baseYOffset()),
                carouselSlots("leftScale", fallback.leftScale()),
                carouselSlots("rightScale", fallback.rightScale()),
                carouselSlots("leftDistance", fallback.leftDistance()),
                carouselSlots("rightDistance", fallback.rightDistance()),
                carouselSlots("yOffset", fallback.yOffsets())
        );
    }

    private float[] carouselSlots(String name, float[] fallback) {
        float[] values = new float[fallback.length];
        for (int i = 0; i < values.length; i++)
            values[i] = accessor.getFloat("carousel.slots." + name + "." + i, fallback[i]);
        return values;
    }

    private int visiblePackRows() {
        return accessor.getInteger("packList.visibleRows", 6);
    }

    private int resolvePackRowHeight() {
        int scaledHeight = Math.max(10, Math.round(PACK_BUTTON_BASE_HEIGHT * uiScale));
        int availableHeight = resolvePackListAvailableHeight();
        int fittedHeight = Math.max(10, (availableHeight - PACK_LIST_FOOTER_RESERVE) / visiblePackRows());
        if (accessor.getBoolean("packList.fitRows", false)) return fittedHeight;
        return Math.min(fittedHeight, Math.max(18, scaledHeight));
    }

    private int resolvePackListAvailableHeight() {
        int minY = Math.max(
                panel.y + sc(PACK_LIST_TOP),
                previewBoxY() + previewBoxSize() + sc(PREVIEW_LIST_GAP)
        );
        return Math.max(1, panel.y + panel.height - sc(PACK_LIST_BOTTOM_INSET) - minY);
    }

    private int adjustPackListHeight(int height) {
        int visibleRowsHeight = resolvedPackRowHeight * visiblePackRows();
        return Math.max(1, Math.min(height, visibleRowsHeight + PACK_LIST_FOOTER_RESERVE));
    }

    private int centerTextX() {
        if (playerSkinWidgetList != null && playerSkinWidgetList.getCenter() != null)
            return playerSkinWidgetList.getCenterAnchorX();
        return tooltipBox.x - sc(TEXT_CENTER_INSET_X)
                + (tooltipBox.getWidth() - sc(TEXT_CENTER_WIDTH_TRIM)) / 2;
    }

    private Bounds changeSkinFrameBounds() {
        int frameInset = sc(accessor.getInteger("frame.insetX", 10));
        int extraInset = sc(accessor.getInteger("frame.extraInsetX", 0));
        int yInset = sc(FRAME_TOP) - FRAME_TOP_LIFT;
        return new Bounds(
                tooltipBox.x - frameInset - extraInset,
                panel.y + yInset,
                Math.max(1, tooltipBox.getWidth() + extraInset),
                Math.max(1, panel.height - yInset - sc(3) - FRAME_BOTTOM_LIFT)
        );
    }

    private FrameFooterLayout resolveFrameFooter(Bounds frame) {
        int footerTop = frame.y() + FRAME_TEXTURE_SIZE.scaleY(FOOTER_OPENING_TOP, frame.height());
        int footerBottom = frame.y() + FRAME_TEXTURE_SIZE.scaleY(FOOTER_OPENING_BOTTOM, frame.height());
        int footerHeight = Math.max(1, footerBottom - footerTop);

        int detailsX = frame.x() + LAYOUT_REFERENCE_SIZE.scaleX(DETAILS_PANEL_X_OFFSET, frame.width());
        int detailsWidth = Math.max(1, LAYOUT_REFERENCE_SIZE.scaleX(DETAILS_PANEL_WIDTH, frame.width()));
        int scaledDetailsHeight = Math.max(1, LAYOUT_REFERENCE_SIZE.scaleY(DETAILS_PANEL_HEIGHT, frame.height()));

        int scaledActionGap = Math.max(0, LAYOUT_REFERENCE_SIZE.scaleY(ACTION_HOLDER_GAP, frame.height()));
        int actionGap = Math.min(scaledActionGap, Math.max(0, footerHeight - 2));
        int maxActionSize = Math.max(1, (footerHeight - actionGap) / 2);
        int actionHolderSize = accessor.getInteger("footer.actionHolderSize", 60);
        int actionWidth = LAYOUT_REFERENCE_SIZE.scaleX(actionHolderSize, frame.width());
        int actionHeight = LAYOUT_REFERENCE_SIZE.scaleY(actionHolderSize, frame.height());
        int actionSize = Math.max(1, Math.min(Math.min(actionWidth, actionHeight), maxActionSize));
        int actionGroupHeight = actionSize * 2 + actionGap;
        int primaryActionY = footerTop + Math.max(0, (footerHeight - actionGroupHeight) / 2);
        boolean alignDetails = accessor.getBoolean("footer.alignDetailsToActions", false);
        int detailsHeight = alignDetails ? actionGroupHeight : Math.max(1,
                Math.min(footerHeight, scaledDetailsHeight) - DETAILS_PANEL_VERTICAL_INSET * 2);
        int detailsY = alignDetails ? primaryActionY : footerTop + (footerHeight - detailsHeight) / 2;
        int actionX = frame.right()
                - LAYOUT_REFERENCE_SIZE.scaleX(ACTION_HOLDER_RIGHT_INSET, frame.width())
                - actionSize - accessor.getInteger("footer.actionExtraRightInset", 2);

        return new FrameFooterLayout(
                new Bounds(detailsX, detailsY, detailsWidth, detailsHeight),
                new Bounds(actionX, primaryActionY, actionSize, actionSize),
                new Bounds(actionX, primaryActionY + actionSize + actionGap, actionSize, actionSize)
        );
    }

    private float mainTextScale() {
        if (accessor.getBoolean("text.compact", false)) return compactTextScale();
        float scale = bigTextScale() * 1.05f;
        int framebufferHeight = minecraft.getWindow().getHeight();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (framebufferHeight <= 0 || guiHeight <= 0) return scale;
        float framebufferScale = framebufferHeight / (float) guiHeight;
        return Math.max(1.0f / framebufferScale,
                Math.round(scale * framebufferScale) / framebufferScale);
    }

    private float packTypeTextScale() {
        return accessor.getBoolean("text.compact", false) ? compactTextScale() : smallTextScale() * 1.05f;
    }

    private float compactTextScale() {
        return Math.clamp(
                uiScale / accessor.getFloat("text.compactReferenceScale", 0.94116f),
                accessor.getFloat("text.compactMinScale", 0.52f),
                1.0f
        );
    }

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
    protected void onWidgetListCreated(PlayerSkinWidgetList list) {
        applyCarouselTuning(false);
    }

    @Override
    protected void onAfterSkinPackChanged() {
        applyCarouselTuning(true);
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
                panel.x = Math.clamp(panel.x, minX, maxX);
                int minY = sc(layoutProfile.tooltipGroupMargin());
                int groupBottomTrim = Math.max(0, sc(layoutProfile.tooltipYOffset() - layoutProfile.tooltipHeightInset()));
                int groupHeight = panel.height + groupBottomTrim;
                int desiredGroupY = (ChangeSkinScreen.this.height - groupHeight) / 2;
                panel.y = desiredGroupY;
                int maxY = ChangeSkinScreen.this.height - controlTooltipFooterReserve() - groupHeight - sc(layoutProfile.tooltipGroupMargin());
                if (maxY < minY) maxY = minY;
                panel.y = Math.clamp(panel.y, minY, maxY);
                appearance(LegacySprites.POINTER_PANEL, tooltipWidth, panel.height - sc(layoutProfile.tooltipHeightInset()));
                pos(panel.x + panel.width - 2, panel.y + sc(layoutProfile.tooltipYOffset()));
            }

            @Override
            public void init() {
                init("tooltipBox");
            }

            @Override
            public void extractRenderState(GuiGraphicsExtractor g, int i, int j, float f) {
                LegacyRenderUtil.renderPointerPanel(g, getX(), getY(), getWidth(), getHeight());
            }
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
        return panel.x + panel.width;
    }

    private int carouselClipRight() {
        return previewOpeningRight(changeSkinFrameBounds());
    }

    @Override
    public void renderableVListInit() {
        int frameX = panel.x + sc(PACK_FRAME_INSET_X);
        int frameY = panel.y + sc(PACK_FRAME_TOP);
        int frameW = Math.max(1, panel.width - sc(PACK_FRAME_WIDTH_TRIM));
        int frameH = Math.max(1, panel.height - sc(PACK_FRAME_BOTTOM_TRIM));
        int arrowWidth = accessor.getInteger("packList.scrollArrow.width", 14);
        int arrowHeight = accessor.getInteger("packList.scrollArrow.height", 7);
        int arrowOffsetX = accessor.getInteger("packList.scrollArrow.offsetX", -3);
        int arrowOffsetY = accessor.getInteger("packList.scrollArrow.offsetY", -2);
        int arrowTop = frameY + frameH - sc(6) - arrowHeight;
        int x = frameX + sc(4);
        int w = Math.max(1, frameW - sc(8));
        packList.refreshPackIdsIfNeeded();
        packList.setReorderMode(isReorderingCustomPack());
        int visibleRows = visiblePackRows();
        int maxListBottom = arrowTop - Math.round(arrowHeight * 0.75f);
        int rowPitch = Math.max(10, resolvedPackRowHeight);
        int buttonHeight = rowPitch;
        int h = rowPitch * visibleRows + PACK_LIST_FOOTER_RESERVE;
        int frameListTop = maxListBottom - rowPitch * visibleRows;
        int y = frameListTop;
        int packFrameRenderX = frameX - sc(2);
        int packFrameRenderY = frameListTop - sc(3);
        int packFrameRenderW = frameW + sc(4);
        int packFrameRenderH = frameY + frameH - packFrameRenderY;
        int packIconTop = panel.y + sc(FRAME_TOP);
        int packIconBottom = packFrameRenderY;
        int packIconAvailableHeight = Math.max(1, packIconBottom - packIconTop);
        int packIconSize = Math.max(1, Math.round(Math.min(panel.width - sc(12), packIconAvailableHeight) * 0.8925f));
        int packIconY = packIconTop + Math.max(0, (packIconAvailableHeight - packIconSize) / 2) - sc(3);
        int packIconX = panel.x + Math.max(0, (panel.width - packIconSize) / 2);
        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, packIconX, packIconY, packIconSize, packIconSize));
        addRenderableOnly((g, i, j, f) -> {
            SkinPack pack = packList.getFocusedDisplayPack();
            Identifier icon = pack == null ? null : pack.icon();
            if (icon == null) return;
            int innerInset = 2;
            int innerX = packIconX + innerInset;
            int innerY = packIconY + innerInset;
            int innerSize = Math.max(1, packIconSize - innerInset * 2);
            int[] d = packIconDims(icon);
            float scale = Math.min(innerSize / (float) d[0], innerSize / (float) d[1]);
            scale += 1f / Math.max(d[0], d[1]);
            float edgeFill = 0.25f;
            float scaleX = scale + edgeFill / d[0];
            float scaleY = scale + edgeFill / d[1];
            float cx = innerX + innerSize / 2f + edgeFill / 2f;
            float cy = innerY + innerSize / 2f - edgeFill / 2f;
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx, cy);
            pose.scale(scaleX, scaleY);
            pose.translate(-d[0] / 2f, -d[1] / 2f);
            g.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0, 0, d[0], d[1], d[0], d[1]);
            pose.popMatrix();
        });

        addRenderableOnly((g, i, j, f) ->
                blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, packFrameRenderX, packFrameRenderY, packFrameRenderW, packFrameRenderH));
        packList.applyResolvedButtonHeight(buttonHeight);
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
        applyScreenScaleAdjustment();
        renderableVList.layoutSpacing(l -> 0);
        if (windowResized) {
            getRenderableVList().resetScroll();
        }
        addRenderableOnly(panel);
        panel.init();
        panel.panelSprite = LegacySprites.PANEL;
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

    private void stopHoldingPackStick() {
        packHold.stop();
    }

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
        FrameFooterLayout footer = resolveFrameFooter(changeSkinFrameBounds());
        if (footer.primaryAction().contains(mx, my)) {
            selectSkin();
            return true;
        }
        if (footer.secondaryAction().contains(mx, my)) {
            favoriteSkin();
            return true;
        }
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
            else if (ay >= triggerY) {
                int dir = sy < 0 ? -1 : 1;
                if (!packHold.active() || packHold.dir() != dir) startHoldingPackStick(dir);
            }
            pumpHoldingPackStick();
            if (Math.abs(sx) <= sideLimit && ay >= releaseY) {
                state.block();
                return;
            }
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
    public void renderDefaultBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);
        Bounds frame = changeSkinFrameBounds();
        FrameFooterLayout footer = resolveFrameFooter(frame);
        renderFrame(g, frame);
        renderFrameFooter(g, footer);
        renderActionIcons(g, footer);
        updateCarouselClip();
        LegacyFontUtil.applySDFont(ignored -> {
            renderSelectedSkinText(g, footer);
            renderPackText(g);
        });
    }

    private void renderFrame(GuiGraphicsExtractor g, Bounds frame) {
        int packHeaderY = panel.y + sc(PACK_HEADER_TOP);
        int packHeaderHeight = Math.max(1, sc(PACK_HEADER_HEIGHT));
        int packHeaderRenderHeight = packHeaderHeight
                + Math.max(1, Math.round(packHeaderHeight * PACK_HEADER_HEIGHT_OVERSCAN_RATIO))
                + sc(PACK_HEADER_BOTTOM_PADDING);
        int previewRight = previewOpeningRight(frame);
        int previewTop = frame.y() + scaleTextureY(PREVIEW_OPENING_TOP, frame.height());
        int previewBottom = frame.y() + scaleTextureY(PREVIEW_OPENING_BOTTOM, frame.height());

        if (previewRight > frame.x() && previewBottom > previewTop) {
            g.fill(frame.x(), previewTop, previewRight, previewBottom, SKIN_PREVIEW_FILL_COLOR);
            int packHeaderBottom = Math.min(previewBottom, packHeaderY + packHeaderRenderHeight);
            if (packHeaderBottom > previewTop)
                g.fill(frame.x(), previewTop, frame.right(), packHeaderBottom, PACK_HEADER_FILL_COLOR);
        }
        blitSprite(g, CHANGE_SKIN_FRAME, frame);
    }

    private static int previewOpeningRight(Bounds frame) {
        return frame.x() + scaleTextureX(PREVIEW_OPENING_RIGHT, frame.width());
    }

    private void renderFrameFooter(GuiGraphicsExtractor g, FrameFooterLayout footer) {
        Bounds detailsPanel = footer.detailsPanel();
        blitSprite(g, LegacySprites.SIZEABLE_ICON_HOLDER, detailsPanel);
        if (detailsPanel.width() > 2 && detailsPanel.height() > 2) {
            g.fill(detailsPanel.x() + 1, detailsPanel.y() + 1,
                    detailsPanel.right() - 1, detailsPanel.bottom() - 1,
                    DETAILS_PANEL_FILL_COLOR);
        }
        blitSprite(g, LegacySprites.SIZEABLE_ICON_HOLDER, footer.primaryAction());
        blitSprite(g, LegacySprites.SIZEABLE_ICON_HOLDER, footer.secondaryAction());
    }

    private void renderActionIcons(GuiGraphicsExtractor g, FrameFooterLayout footer) {
        PlayerSkinWidget center = getCenterWidget();
        if (center == null) return;

        String selected = center.skinId.get();
        String current = currentAppliedSkinId();
        boolean isAuto = SkinIdUtil.isAutoSelect(selected);
        boolean isAutoActive = current == null || current.isBlank();
        boolean isImport = customPacks.isImportSkinSelection(selected);
        boolean reordering = isReorderingCustomPack();
        boolean editing = isEditingCustomPack();
        boolean locked = customPacks.isLockedSkinSelection(selected);

        if (reordering) {
            drawTick(g, footer.primaryAction());
        } else if (editing) {
            if (isImport || locked) {
                drawPadlock(g, footer.primaryAction());
            } else if (selected != null) {
                drawTick(g, footer.primaryAction());
            }
            if (customPacks.isRemovableSkinSelection(selected))
                drawActionSprite(g, LegacySprites.ERROR_CROSS, footer.secondaryAction());
        } else if (isImport) {
            drawPadlock(g, footer.primaryAction());
        } else if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
            drawTick(g, footer.primaryAction());
        }
        if (!editing && !isImport && isSkinFavorite(selected))
            drawFavoriteHeart(g, footer.secondaryAction());
    }

    private void updateCarouselClip() {
        int clipLeft = carouselClipLeft();
        int clipTop = tooltipContentTop();
        int clipRight = carouselClipRight();
        int clipBottom = tooltipContentBottom() - sc(getLayoutMetrics().carouselClipBottomTrim());
        PlayerSkinWidget.setCarouselClip(clipLeft, clipTop, clipRight, clipBottom);
    }

    private void renderSelectedSkinText(GuiGraphicsExtractor g, FrameFooterLayout footer) {
        PlayerSkinWidget center = getCenterWidget();
        if (center == null) return;

        String skinId = center.skinId.get();
        String name = source.skinName(skinId);
        String theme = source.skinTheme(skinId);
        boolean hasTheme = theme != null && !theme.isBlank() && !theme.equals(name);
        int textX = centerTextX();
        float textScale = mainTextScale();
        int scaledLineHeight = Math.max(1, (int) (minecraft.font.lineHeight * textScale));
        int textGap = sc(THEME_TEXT_GAP);
        int skinNameY;
        if (accessor.getBoolean("text.centerInFooter", false)) {
            Bounds details = footer.detailsPanel();
            int blockHeight = hasTheme ? scaledLineHeight * 2 + textGap : scaledLineHeight;
            int drawTop = details.y() + Math.max(0, (details.height() - blockHeight) / 2);
            int yCorrection = (int) ((textScale - 1.0f) * minecraft.font.lineHeight / 2.0f);
            skinNameY = drawTop + yCorrection;
        } else {
            skinNameY = panel.y + tooltipBox.getHeight() - sc(SKIN_NAME_BOTTOM_TRIM);
        }
        skinNameY += SELECTED_SKIN_TEXT_Y_OFFSET;
        int maxTextWidth = Math.max(1,
                (int) ((tooltipBox.getWidth() - sc(THEME_TEXT_WIDTH_TRIM)) / textScale));
        String clippedName = PlayerSkinWidget.clipText(minecraft.font, name, maxTextWidth);
        drawScaledCentered(g, Component.literal(clippedName), textX, skinNameY,
                LegacyRenderUtil.getDefaultTextColor(true), textScale, true);

        if (!hasTheme) return;

        String clippedTheme = PlayerSkinWidget.clipText(minecraft.font, theme, maxTextWidth);
        int themeY = skinNameY + scaledLineHeight + textGap;
        int maxThemeY = panel.y + tooltipBox.getHeight() - sc(THEME_BOTTOM_INSET)
                + SELECTED_SKIN_TEXT_Y_OFFSET;
        drawScaledCentered(g, Component.literal(clippedTheme), textX, Math.min(themeY, maxThemeY),
                LegacyRenderUtil.getDefaultTextColor(true), textScale, true);
    }

    private void renderPackText(GuiGraphicsExtractor g) {
        SkinPack pack = packList.getFocusedDisplayPack();
        if (pack == null) return;

        int textX = centerTextX();
        int packTitleY = panel.y + sc(PACK_TITLE_TOP);
        float textScale = mainTextScale();
        int packMetaY = packTitleY + (int) (minecraft.font.lineHeight * textScale)
                + sc(PACK_META_GAP);
        int maxPackWidth = Math.max(1,
                (int) ((tooltipBox.getWidth() - sc(PACK_TEXT_WIDTH_TRIM)) / textScale));
        String packName = PlayerSkinWidget.clipText(minecraft.font, source.packName(pack), maxPackWidth);
        drawScaledCentered(g, Component.literal(packName), textX, packTitleY,
                LegacyRenderUtil.getDefaultTextColor(true), textScale, true);
        Component label = packLabel(pack);
        if (label != null)
            drawScaledCentered(g, label, textX, packMetaY,
                    LegacyRenderUtil.getDefaultTextColor(true), packTypeTextScale(), true);
    }

    @Override
    public void addControlTooltips(ControlTooltipList r) {
        addCommonControlTooltips(
                r,
                ControlTooltip.POINTER_MOVEMENT::get,
                () -> LegacyComponents.NAVIGATE
        );
    }

    @Override
    public void removed() {
        stopHoldingPackStick();
        super.removed();
    }

    private void drawTick(GuiGraphicsExtractor guiGraphics, Bounds holder) {
        int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
        int holderFramebufferSize = Math.min(holder.width(), holder.height()) * guiScale;
        int actionHolderSize = accessor.getInteger("footer.actionHolderSize", 60);
        int fullFramebufferSize = accessor.getInteger("footer.tick.fullFramebufferSize", 42);
        int sourceSize = accessor.getInteger("footer.tick.sourceSize", 21);
        int fittedFramebufferSize = Math.max(1, Math.round(holderFramebufferSize
                * (fullFramebufferSize / (float) actionHolderSize)));
        boolean fhdTick = sourceSize == 28;
        int framebufferSize = fhdTick
                ? sourceSize * Math.max(1, Math.round(fittedFramebufferSize / (float) sourceSize))
                : snapTickFramebufferSize(fittedFramebufferSize, sourceSize, fullFramebufferSize);
        float centerX = Math.round(holder.centerX() * guiScale) / (float) guiScale;
        float centerY = Math.round(holder.centerY() * guiScale) / (float) guiScale;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(centerX, centerY);
        guiGraphics.pose().scale(1.0f / guiScale, 1.0f / guiScale);
        blitSprite(guiGraphics, fhdTick ? LegacySprites.BEACON_CONFIRM : LegacySprites.SKIN_TICK,
                -framebufferSize / 2, -framebufferSize / 2, framebufferSize, framebufferSize);
        guiGraphics.pose().popMatrix();
    }

    private static int snapTickFramebufferSize(int fittedSize, int sourceSize, int fullSize) {
        if (fittedSize >= fullSize) return fullSize;
        if (fittedSize >= sourceSize) return sourceSize;
        return fittedSize;
    }

    private void drawPadlock(GuiGraphicsExtractor guiGraphics, Bounds holder) {
        float size = Math.max(1f, holder.width() - 8f);
        float scale = size / PADLOCK_TEXTURE_SIZE;
        float left = holder.x() + (holder.width() - PADLOCK_TEXTURE_SIZE * scale) / 2.0f;
        float top = holder.y() + (holder.height() - PADLOCK_TEXTURE_SIZE * scale) / 2.0f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(left, top);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PADLOCK_TEXTURE,
                0, 0, 0, 0,
                PADLOCK_TEXTURE_SIZE, PADLOCK_TEXTURE_SIZE,
                PADLOCK_TEXTURE_SIZE, PADLOCK_TEXTURE_SIZE);
        guiGraphics.pose().popMatrix();
    }

    private void drawFavoriteHeart(GuiGraphicsExtractor guiGraphics, Bounds holder) {
        float heartSize = Math.max(1f, holder.width() - accessor.getFloat("footer.heartInset", 8f));
        float heartScaleX = heartSize / HEART_TEXTURE_SIZE;
        float heartScaleY = (heartSize + 1.0f) / HEART_TEXTURE_SIZE;
        float centerX = holder.centerX() + holder.width() * HEART_HOLDER_OFFSET_X;
        float centerY = holder.centerY() + holder.height() * HEART_HOLDER_OFFSET_Y;
        float heartX = centerX - (HEART_TEXTURE_SIZE * heartScaleX) / 2.0f;
        float heartY = centerY - (HEART_TEXTURE_SIZE * heartScaleY) / 2.0f - 0.5f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(heartX, heartY);
        guiGraphics.pose().scale(heartScaleX, heartScaleY);
        blitSprite(guiGraphics, HEART_CONTAINER, 0, 0, HEART_TEXTURE_SIZE, HEART_TEXTURE_SIZE);
        blitSprite(guiGraphics, HEART_FULL, 0, 0, HEART_TEXTURE_SIZE, HEART_TEXTURE_SIZE);
        guiGraphics.pose().popMatrix();
    }

    private void drawActionSprite(GuiGraphicsExtractor guiGraphics, Identifier sprite, Bounds holder) {
        int size = Math.max(1, holder.width() - 8);
        int left = holder.x() + (holder.width() - size) / 2;
        int top = holder.y() + (holder.height() - size) / 2;
        blitSprite(guiGraphics, sprite, left, top, size, size);
    }

    private static void blitSprite(GuiGraphicsExtractor guiGraphics, Identifier sprite, Bounds bounds) {
        blitSprite(guiGraphics, sprite, bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static int scaleTextureX(int sourceX, int targetWidth) {
        return Math.round(targetWidth * (sourceX / (float) FRAME_TEXTURE_SIZE.width()));
    }

    private static int scaleTextureY(int sourceY, int targetHeight) {
        return Math.round(targetHeight * (sourceY / (float) FRAME_TEXTURE_SIZE.height()));
    }

    private record ReferenceSize(int width, int height) {
        int scaleX(int value, int targetWidth) {
            return Math.round(value * targetWidth / (float) width);
        }

        int scaleY(int value, int targetHeight) {
            return Math.round(value * targetHeight / (float) height);
        }
    }

    private record Bounds(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        float centerX() {
            return x + width / 2.0f;
        }

        float centerY() {
            return y + height / 2.0f;
        }

        boolean contains(double pointX, double pointY) {
            return inside(pointX, pointY, x, y, width, height);
        }
    }

    private record FrameFooterLayout(
            Bounds detailsPanel,
            Bounds primaryAction,
            Bounds secondaryAction
    ) {
    }

}
