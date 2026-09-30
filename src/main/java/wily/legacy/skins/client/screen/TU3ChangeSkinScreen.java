package wily.legacy.skins.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.tooltip.CommonIcon;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.screen.Panel;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.skins.client.preview.PlayerSkinWidgetList;
import wily.legacy.skins.skin.SkinIdUtil;
import wily.legacy.skins.skin.SkinPack;
import wily.legacy.skins.skin.SkinSync;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

public class TU3ChangeSkinScreen extends AbstractChangeSkinScreen {
    private static final int TAB_OUTSET = 2, MID_TAB_TOP_DROP = 6;
    private static final float GREY_TINT = 0.84f, SIDE_TAB_TINT = 0.94f;
    private static final int NAME_PLATE_HIGHLIGHT = 0xFFEBEB0F;
    private static final Identifier TOP_STRIP = Identifier.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_top_strip"),
            BOTTOM_STRIP = Identifier.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_bottom_strip"),
            TAB_PLATE = Identifier.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_tab_plate"),
            SELECTED_BADGE = Identifier.fromNamespaceAndPath("legacy", "tiles/tu3_selected");
    private final HoldRepeat horizontalHold = new HoldRepeat();
    private int layoutX, layoutY, layoutW, layoutH, stripY, stripH, bottomStripY, bottomStripH, tabY, tabH,
            tabLeftX, tabMidX, tabRightX, tabLeftW, tabMidW, tabRightW;
    private float textScale = 1f;
    private boolean carouselFocused = true;
    private String namePlateSkinId;

    private int tu3Int(String name, int fallback) {
        return accessor.getInteger("tu3." + name, fallback);
    }

    private float tu3Float(String name, float fallback) {
        return accessor.getFloat("tu3." + name, fallback);
    }

    private int carouselPad() {
        return sc(tu3Int("carouselPad", 6));
    }

    public TU3ChangeSkinScreen(Screen parent) {
        this(parent, ChangeSkinScreenSource.Default.INSTANCE);
    }

    public TU3ChangeSkinScreen(Screen parent, ChangeSkinScreenSource source) {
        super(parent, source, false);
    }

    private void startHoldingHorizontal(int dir) {
        horizontalHold.start(dir);
        handleNavMove(dir);
    }

    private void pumpHoldingHorizontal() {
        if (!horizontalHold.ready()) return;
        handleNavMove(horizontalHold.dir());
        horizontalHold.step();
    }

    private void tintBlitSprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h, float tint) {
        FactoryGuiGraphics.of(g).setBlitColor(tint, tint, tint, 1.0f);
        blitSprite(g, sprite, x, y, Math.max(1, w), Math.max(1, h));
        FactoryGuiGraphics.of(g).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private float activeTint() {
        return carouselFocused ? GREY_TINT : 1.0f;
    }

    private boolean handleNavMove(int dir) {
        if (dir == 0) return false;
        if (carouselFocused) return control(dir < 0, dir > 0);
        if (packList.getPackCount() <= 1) return true;
        focusRelativePack(dir, true);
        applyQueuedPackChange();
        return true;
    }

    private boolean handleNavVertical(boolean up, boolean down) {
        if (up && carouselFocused) {
            carouselFocused = false;
            return true;
        }
        if (down && !carouselFocused) {
            carouselFocused = true;
            return true;
        }
        return false;
    }

    @Override
    protected Panel createTooltipBox() {
        return new Panel(UIAccessor.of(this)) {
            @Override
            public void extractRenderState(GuiGraphicsExtractor g, int i, int j, float f) {
            }
        };
    }

    @Override
    protected void onWidgetListCreated(PlayerSkinWidgetList list) {
        list.setCenterOverlay(this::renderNamePlate);
    }

    @Override
    protected void onAfterSkinPackChanged() {
        applyCarouselTuning();
    }

    @Override
    protected boolean insideScrollRegion(double mx, double my) {
        return inside(mx, my, layoutX, layoutY, layoutW, layoutH);
    }

    @Override
    public void renderableVListInit() {
        packList.refreshPackIdsIfNeeded();
        getRenderableVList().renderables.clear();
    }

    @Override
    protected void panelInit() {
        refreshSharedLayout();
        textScale = tu3Float("textScale", 1f);
        renderableVList.layoutSpacing(l -> 0);
        panel.init();
        tooltipBox.init("tooltipBox");
        layoutX = 0;
        layoutW = Math.max(1, width);
        stripH = Math.max(1, Math.round(62f * uiScale * tu3Float("topStripScale", 0.6f)));
        bottomStripH = Math.max(1, Math.round(tu3Int("bottomStripBaseHeight", 20) * uiScale));
        int shrunkGrey = Math.max(1, Math.round(Math.max(1, height - stripH - bottomStripH) * tu3Float("greyHeightRatio", 0.67f)));
        stripY = Math.max(0, (height - (stripH + shrunkGrey + bottomStripH)) / 2 + sc(tu3Int("menuYOffset", 0)));
        tabY = stripY + Math.round((2f / 62f) * stripH);
        tabH = Math.min(Math.max(1, Math.round((50f / 62f) * stripH)), Math.max(1, stripY + stripH - tabY));
        layoutY = stripY + stripH;
        bottomStripY = Math.max(layoutY + 1, Math.min(height - bottomStripH, layoutY + shrunkGrey));
        layoutH = Math.max(1, bottomStripY - layoutY);
        panel.pos(layoutX, layoutY);
        panel.size(1, layoutH);
        tooltipBox.pos(layoutX, layoutY);
        tooltipBox.size(layoutW, layoutH);
        computeTabs();
    }

    private void computeTabs() {
        int inset = Math.max(0, Math.round((tu3Int("tabInsetNumerator", 105) / 1280f) * width));
        int left = inset, total = width - inset * 2;
        if (total < 1) {
            left = 0;
            total = Math.max(1, width);
        }
        int w = total / 3;
        tabLeftX = left;
        tabMidX = left + w;
        tabRightX = left + w * 2;
        tabLeftW = tabMidW = w;
        tabRightW = w + (total - w * 3);
    }

    private int midTabExtra() {
        return Math.max(1, sc(tu3Int("midExtra", 11)));
    }

    private int midTabY() {
        return tabY - tu3Int("activeTabLift", 2) - midTabExtra() + MID_TAB_TOP_DROP;
    }

    private int midTabH() {
        return tabH + midTabExtra() - MID_TAB_TOP_DROP;
    }

    private void renderTabs(GuiGraphicsExtractor g) {
        int midY = midTabY(), midH = midTabH();
        tintBlitSprite(g, TAB_PLATE, tabMidX, midY, tabMidW, midH, activeTint());
        int baseLabelY = tabY + (tabH - minecraft.font.lineHeight) / 2;
        int midLabelY = midY + (midH - minecraft.font.lineHeight) / 2;
        int idx = packList.getFocusedPackIndex();
        renderTabLabel(g, tabLeftX - TAB_OUTSET, tabLeftW + TAB_OUTSET, baseLabelY, idx - 1);
        renderTabLabel(g, tabMidX, tabMidW, midLabelY, idx);
        renderTabLabel(g, tabRightX, tabRightW + TAB_OUTSET, baseLabelY, idx + 1);
    }

    private void renderTabLabel(GuiGraphicsExtractor g, int x, int w, int y, int packIndex) {
        int maxPx = Math.max(1, Math.round((w - sc(tu3Int("tabLabelWidthTrim", 16))) / textScale));
        String text = PlayerSkinWidget.clipText(minecraft.font, packList.getWrappedLabelForIndex(packIndex).getString(), maxPx);
        int color = CommonColor.GRAY_TEXT.get();
        LegacyFontUtil.applySDFont(b -> {
            g.pose().pushMatrix();
            if (textScale == 1f) {
                g.pose().translate(0.4f, 0.4f);
                g.text(minecraft.font, Component.literal(text), x + (Math.max(1, w) - minecraft.font.width(text)) / 2, y, color, false);
            } else {
                int lineHeight = Math.max(1, Math.round(minecraft.font.lineHeight * textScale));
                g.pose().translate(x + Math.max(1, w) / 2f + 0.4f, y + (minecraft.font.lineHeight - lineHeight) / 2f + 0.4f);
                g.pose().scale(textScale, textScale);
                g.text(minecraft.font, Component.literal(text), -minecraft.font.width(text) / 2, 0, color, false);
            }
            g.pose().popMatrix();
        });
    }

    private void applyCarouselTuning() {
        if (playerSkinWidgetList == null) return;
        float carouselScale = tu3Float("carouselScale", 1.4175f);
        playerSkinWidgetList.setCarouselTuning(carouselScale, tu3Float("carouselSpacing", 1.1f));
        SkinPack p = packList.getFocusedPack();
        playerSkinWidgetList.setAvoidRepeatsWhenFew(p != null && SkinIdUtil.isFavouritesPack(p.id()), 7);
        float s0 = 0.935f * uiScale * carouselScale, s1 = 0.77f * uiScale * carouselScale;
        float s2 = 0.605f * uiScale * carouselScale, s3 = 0.44f * uiScale * carouselScale;
        float w0 = 106f * s0, w1 = 106f * s1, w2 = 106f * s2, w3 = 106f * s3;
        int pad = Math.max(1, carouselPad());
        float gap = (Math.max(1f, (float) layoutW - pad * 2f) - (w0 + (w1 + w2 + w3) * 2f)) / 6f;
        float cM3 = layoutX + pad + w3 / 2f;
        float cM2 = cM3 + w3 / 2f + gap + w2 / 2f;
        float cM1 = cM2 + w2 / 2f + gap + w1 / 2f;
        float c0 = cM1 + w1 / 2f + gap + w0 / 2f;
        float cP1 = c0 + w0 / 2f + gap + w1 / 2f;
        float cP2 = cP1 + w1 / 2f + gap + w2 / 2f;
        float cP3 = cP2 + w2 / 2f + gap + w3 / 2f;
        float spawnerExtra = Math.max(tu3Int("spawnerExtraMin", 10) * uiScale, w3 * tu3Float("spawnerExtraFactor", 0.35f));
        float spawnerStep = w3 / 2f + gap + w3 / 2f;
        playerSkinWidgetList.setCustomCarouselCenters(new int[]{
                Math.round(cM3 - spawnerStep - spawnerExtra), Math.round(cM3), Math.round(cM2), Math.round(cM1), Math.round(c0),
                Math.round(cP1), Math.round(cP2), Math.round(cP3), Math.round(cP3 + spawnerStep + spawnerExtra)
        });
        float areaCenter = (layoutY + bottomStripY) / 2f;
        int originY = Math.round(areaCenter - 150f * s0 / 2f - tu3Int("centerOriginYOffset", 32) * uiScale);
        playerSkinWidgetList.setOrigin(playerSkinWidgetList.x, originY);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }

    @Override
    protected boolean handlePackListStepNavigation(int key) {
        return handlePackListStepNavigation(key, false, false, true, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mx = e.x(), my = e.y();
            if (inside(mx, my, tabLeftX - TAB_OUTSET, tabY, Math.max(1, tabLeftW + TAB_OUTSET), tabH)) {
                carouselFocused = false;
                focusRelativePack(-1, false);
                return true;
            }
            if (inside(mx, my, tabRightX, tabY, Math.max(1, tabRightW + TAB_OUTSET), tabH)) {
                carouselFocused = false;
                focusRelativePack(1, false);
                return true;
            }
            if (inside(mx, my, tabMidX, midTabY(), Math.max(1, tabMidW), midTabH())) {
                carouselFocused = false;
                return true;
            }
        }
        if (handleCarouselMouseClicked(e, bl)) {
            carouselFocused = true;
            return true;
        }
        return super.mouseClicked(e, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();
        if (handleNavVertical(key == InputConstants.KEY_UP || key == InputConstants.KEY_W,
                key == InputConstants.KEY_DOWN || key == InputConstants.KEY_S))
            return true;
        int dir = key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A ? -1
                : key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D ? 1 : 0;
        if (handleNavMove(dir)) return true;
        return super.keyPressed(e);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state != null && (state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.RIGHT_BUMPER))) {
            if (state.pressed && state.canClick() && packList.getPackCount() > 1) {
                focusRelativePack(state.is(ControllerBinding.RIGHT_BUMPER) ? 1 : -1, true);
                applyQueuedPackChange();
            }
            state.block();
            return;
        }
        if ((buttonOnce(state, ControllerBinding.DPAD_UP) || buttonOnce(state, ControllerBinding.LEFT_STICK_UP)) && handleNavVertical(true, false))
            return;
        if ((buttonOnce(state, ControllerBinding.DPAD_DOWN) || buttonOnce(state, ControllerBinding.LEFT_STICK_DOWN)) && handleNavVertical(false, true))
            return;
        if (state != null && (state.is(ControllerBinding.DPAD_LEFT) || state.is(ControllerBinding.LEFT_STICK_LEFT)
                || state.is(ControllerBinding.DPAD_RIGHT) || state.is(ControllerBinding.LEFT_STICK_RIGHT))) {
            int dir = state.is(ControllerBinding.DPAD_RIGHT) || state.is(ControllerBinding.LEFT_STICK_RIGHT) ? 1 : -1;
            if (state.released) {
                if (horizontalHold.active() && horizontalHold.dir() == dir) horizontalHold.stop();
            } else if (state.pressed) {
                if (!horizontalHold.active() || horizontalHold.dir() != dir) startHoldingHorizontal(dir);
                pumpHoldingHorizontal();
                state.block();
                return;
            }
        }
        if (handleSharedBindingState(state)) return;
        if (!ControlType.getActiveType().isKbm() && state != null && state.is(ControllerBinding.LEFT_STICK)
                && state instanceof BindingState.Axis stick && Math.abs(stick.x) <= 0.45d && Math.abs(stick.y) >= 0.65d) {
            state.block();
            return;
        }
        super.bindingStateTick(state);
    }

    @Override
    protected boolean bumpersScrollCarousel() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        pumpHoldingHorizontal();
        tickScreenTail();
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);
        computeTabs();
        float sideTint = carouselFocused ? GREY_TINT : SIDE_TAB_TINT;
        tintBlitSprite(g, TAB_PLATE, tabLeftX - TAB_OUTSET, tabY, tabLeftW + TAB_OUTSET, tabH, sideTint);
        tintBlitSprite(g, TAB_PLATE, tabRightX, tabY, tabRightW + TAB_OUTSET, tabH, sideTint);
        tintBlitSprite(g, TOP_STRIP, 0, stripY, width, stripH, activeTint());
        g.fill(0, stripY + stripH, Math.max(1, width), Math.max(stripY + stripH + 1, bottomStripY), 0x880D0D0D);
        tintBlitSprite(g, BOTTOM_STRIP, 0, bottomStripY, width, bottomStripH, activeTint());
        int pad = carouselPad();
        PlayerSkinWidget.setCarouselClip(layoutX + pad, layoutY + pad, layoutX + layoutW - pad, bottomStripY - pad);
        renderTabs(g);
    }

    private void renderNamePlate(GuiGraphicsExtractor g, PlayerSkinWidget center) {
        int pad = carouselPad();
        g.enableScissor(layoutX + pad, layoutY + pad, layoutX + layoutW - pad, bottomStripY - pad);
        try {
            renderNamePlateClipped(g, center);
        } finally {
            g.disableScissor();
        }
    }

    private void renderNamePlateClipped(GuiGraphicsExtractor g, PlayerSkinWidget center) {
        String id = center.skinId.get();
        if (id == null) return;
        boolean settled = !carouselAnimating();
        if (settled || namePlateSkinId == null) namePlateSkinId = id;
        if (namePlateSkinId == null) return;
        var font = minecraft.font;
        int plateW = Math.max(1, tabMidW);
        int plateH = Math.max(1, Math.round(tu3Int("namePlateBaseHeight", 16) * uiScale) * 2);
        int plateX = width / 2 - plateW / 2;
        int clipTop = layoutY + carouselPad(), clipBottom = bottomStripY - carouselPad();
        int plateY = Math.max(stripY + stripH + Math.max(1, sc(tu3Int("namePlateTopMargin", 4))), bottomStripY - Math.max(1, sc(tu3Int("namePlateBottomMargin", 8))) - plateH);
        plateY = Math.max(clipTop, Math.min(plateY, clipBottom - plateH - 1));
        if (carouselFocused) {
            g.fill(plateX - 1, plateY - 1, plateX + plateW + 1, plateY, NAME_PLATE_HIGHLIGHT);
            g.fill(plateX - 1, plateY + plateH, plateX + plateW + 1, plateY + plateH + 1, NAME_PLATE_HIGHLIGHT);
            g.fill(plateX - 1, plateY, plateX, plateY + plateH, NAME_PLATE_HIGHLIGHT);
            g.fill(plateX + plateW, plateY, plateX + plateW + 1, plateY + plateH, NAME_PLATE_HIGHLIGHT);
        }
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, plateX, plateY, plateW, plateH);
        String name = source.skinName(namePlateSkinId);
        String theme = SkinIdUtil.isAutoSelect(namePlateSkinId) ? null : source.skinTheme(namePlateSkinId);
        if (theme != null && (theme.isBlank() || theme.equals(name))) theme = null;
        int maxPx = Math.max(1, Math.round((plateW - 8) / textScale));
        int lineHeight = Math.max(1, Math.round(font.lineHeight * textScale));
        int baseY = plateY + (plateH - lineHeight * (theme == null ? 1 : 2)) / 2;
        drawPlateText(g, PlayerSkinWidget.clipText(font, name, maxPx), plateX + plateW / 2, baseY);
        if (theme != null) drawPlateText(g, PlayerSkinWidget.clipText(font, theme, maxPx), plateX + plateW / 2, baseY + lineHeight);
        if (!settled || !isAppliedSkin(namePlateSkinId)) return;
        int badgeW = Math.max(1, Math.round(tabMidW * tu3Float("badgeWidthRatio", 0.52f)));
        int badgeH = Math.max(1, Math.round(tu3Int("badgeBaseHeight", 12) * uiScale));
        int badgeX = width / 2 - badgeW / 2;
        int badgeY = plateY - (Math.max(0, sc(tu3Int("badgeYOffset", 4))) + Math.max(1, sc(4))) - badgeH;
        badgeY = Math.max(clipTop, Math.min(badgeY, clipBottom - badgeH - 1));
        blitSprite(g, SELECTED_BADGE, badgeX, badgeY, badgeW, badgeH);
        drawPlateText(g, "Selected", badgeX + badgeW / 2, badgeY + (badgeH - lineHeight) / 2 + Math.round(2 * textScale));
    }

    private void drawPlateText(GuiGraphicsExtractor g, String text, int centerX, int y) {
        g.pose().pushMatrix();
        g.pose().translate(centerX, y);
        g.pose().scale(textScale, textScale);
        g.centeredText(minecraft.font, Component.literal(text), 0, 0, 0xFFFFFFFF);
        g.pose().popMatrix();
    }

    private boolean isAppliedSkin(String skinId) {
        String applied = currentAppliedSkinId();
        return SkinIdUtil.isAutoSelect(skinId) ? applied == null || applied.isBlank() : skinId.equals(applied);
    }

    @Override
    public void addControlTooltips(ControlTooltipList r) {
        addCommonControlTooltips(r, CommonIcon.POINTER_MOVEMENT::get, () -> LegacyComponents.NAVIGATE);
    }

    @Override
    public void removed() {
        horizontalHold.stop();
        super.removed();
    }
}
