package wily.legacy.Skins.client.screen;
import com.mojang.blaze3d.platform.InputConstants;
import wily.legacy.Skins.client.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.preview.*;
import wily.legacy.Skins.skin.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.*;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
public class TU3ChangeSkinScreen extends AbstractChangeSkinScreen {
    private static final int TU3_TAB_OUTSET = 2;
    private static final int TU3_MID_TAB_TOP_DROP = 6;
    private static final float TU3_GREY_TINT = 0.84f;
    private static final float TU3_SIDE_TAB_TINT = 0.94f;
    private enum Tu3NavZone { CAROUSEL, PACKS }
    private record Tu3LayoutMetrics(
            float topStripScale, int bottomStripBaseHeight, float greyHeightRatio, int tabInsetNumerator, int midExtra, int activeTabLift,
            float carouselScale, float carouselSpacing, int panelTopOffset, int tooltipWidthOverscan, int tooltipBottomOverscan, int carouselPad,
            int namePlateBaseHeight, int namePlateTopMargin, int namePlateBottomMargin, float badgeWidthRatio, int badgeBaseHeight, int badgeYOffset,
            float badgeScale, int tabLabelWidthTrim, int centerOriginYOffset, int spawnerExtraMin, float spawnerExtraFactor
    ) {
        static final Tu3LayoutMetrics DEFAULT = new Tu3LayoutMetrics(0.60f, 20, 0.67f, 105, 11, 2, 1.4175f, 1.1f, 45, 23, 90, 6, 16, 4, 8, 0.52f, 12, 4, 0.75f, 16, 32, 10, 0.35f);
        static final Tu3LayoutMetrics SD_480 = new Tu3LayoutMetrics(0.48f, 14, 0.60f, 84, 8, 1, 1.2096f, 0.95f, 34, 18, 72, 4, 14, 3, 6, 0.48f, 10, 3, 0.70f, 12, 24, 8, 0.25f);
    }
    private static final ResourceLocation TU3_TOP_STRIP = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_top_strip"),
            TU3_BOTTOM_STRIP = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_bottom_strip"),
            TU3_TAB_PLATE = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_tab_plate"),
            TU3_SELECTED_BADGE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected");
    private static final int TU3_NAME_PLATE_HIGHLIGHT = 0xFFEBEB0F;
    private int layoutX, layoutY, layoutW, layoutH, tu3StripY, tu3StripH, tu3BottomStripY, tu3BottomStripH, tu3TabY, tu3TabH,
            tu3TabLeftX, tu3TabMidX, tu3TabRightX, tu3TabLeftW, tu3TabMidW, tu3TabRightW;
    private Tu3LayoutMetrics tu3Layout = Tu3LayoutMetrics.DEFAULT;
    private Tu3NavZone tu3NavZone = Tu3NavZone.CAROUSEL;
    public TU3ChangeSkinScreen(Screen parent) { super(parent); }

    public TU3ChangeSkinScreen(Screen parent, ChangeSkinScreenSource source) { super(parent, source); }

    private void refreshTu3Layout() { tu3Layout = isCompact480() ? Tu3LayoutMetrics.SD_480 : Tu3LayoutMetrics.DEFAULT; }
    private boolean carouselNavActive() { return tu3NavZone == Tu3NavZone.CAROUSEL; }
    private void setTu3NavZone(Tu3NavZone zone) { tu3NavZone = zone == null ? Tu3NavZone.CAROUSEL : zone; }
    private boolean once(BindingState state, ControllerBinding<?> binding) { return state != null && state.is(binding) && state.onceClick(true); }
    private boolean repeat(BindingState state, ControllerBinding<?> binding) {
        if (state == null || !state.is(binding) || !state.pressed || !state.canClick()) return false;
        state.block();
        return true;
    }
    private void tintBlitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, float tint) {
        FactoryGuiGraphics.of(g).setBlitColor(tint, tint, tint, 1.0f);
        blitSprite(g, sprite, x, y, Math.max(1, w), Math.max(1, h));
        FactoryGuiGraphics.of(g).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    private boolean handleTu3NavMove(boolean left, boolean right) {
        if (!(left || right)) return false;
        if (carouselNavActive()) return control(left, right);
        int dir = (left ? -1 : 0) + (right ? 1 : 0);
        if (dir == 0 || packList.getPackCount() <= 1) return true;
        focusRelativePack(dir, true);
        applyQueuedPackChange();
        return true;
    }
    private boolean handleTu3NavVertical(boolean up, boolean down) {
        if (up && carouselNavActive()) {
            setTu3NavZone(Tu3NavZone.PACKS);
            return true;
        }
        if (down && !carouselNavActive()) {
            setTu3NavZone(Tu3NavZone.CAROUSEL);
            return true;
        }
        return false;
    }

    @Override
    protected Panel createTooltipBox() {
        return new Panel(UIAccessor.of(this)) {
            @Override public void init(String name) { super.init(name); }
            @Override public void init()            { init("tooltipBox"); }
            @Override public void render(GuiGraphics g, int i, int j, float f) {}
        };
    }
    @Override
    protected void onAfterSkinPackChanged() { applyTu3CarouselTuning(); }

    @Override
    protected boolean insideScrollRegion(double mx, double my) { return inside(mx, my, layoutX, layoutY, layoutW, layoutH); }

    @Override
    public void renderableVListInit() {
        packList.refreshPackIdsIfNeeded();
        getRenderableVList().renderables.clear();
    }
    @Override
    protected void panelInit() {
        refreshSharedLayout();
        refreshTu3Layout();
        renderableVList.layoutSpacing(l -> 0);
        panel.init();
        tooltipBox.init("tooltipBox");
        layoutX = 0;
        layoutW = Math.max(1, width);
        tu3StripH         = Math.max(1, Math.round(62f * uiScale * tu3Layout.topStripScale()));
        tu3BottomStripH   = Math.max(1, Math.round(tu3Layout.bottomStripBaseHeight() * uiScale));
        int maxGrey    = height - tu3StripH - tu3BottomStripH;
        if (maxGrey < 1) maxGrey = 1;
        int shrunkGrey = Math.max(1, Math.round(maxGrey * tu3Layout.greyHeightRatio()));
        int blockH     = tu3StripH + shrunkGrey + tu3BottomStripH;
        tu3StripY      = Math.max(0, (height - blockH) / 2);
        tu3TabH        = Math.max(1, Math.round((50f / 62f) * tu3StripH));
        tu3TabY        = tu3StripY + Math.round((2f / 62f) * tu3StripH);
        int tabMaxH    = (tu3StripY + tu3StripH) - tu3TabY;
        if (tabMaxH < 1) tabMaxH = 1;
        if (tu3TabH > tabMaxH) tu3TabH = tabMaxH;
        layoutY          = tu3StripY + tu3StripH;
        tu3BottomStripY  = Math.max(layoutY + 1, Math.min(height - tu3BottomStripH, layoutY + shrunkGrey));
        layoutH          = Math.max(1, tu3BottomStripY - layoutY);
        int off45 = Math.round(tu3Layout.panelTopOffset() * uiScale);
        int off23 = Math.round(tu3Layout.tooltipWidthOverscan() * uiScale);
        int off90 = Math.round(tu3Layout.tooltipBottomOverscan() * uiScale);
        panel.pos(layoutX, layoutY - off45);
        panel.size(1, layoutH);
        tooltipBox.pos(layoutX, layoutY);
        tooltipBox.size(layoutW + off23, layoutH + off90);
    }
    private void computeTu3Tabs() {
        int inset = Math.round((tu3Layout.tabInsetNumerator() / 1280f) * width);
        if (inset < 0) inset = 0;
        int left  = inset;
        int total = width - inset * 2;
        if (total < 1) { left = 0; total = Math.max(1, width); }
        int w = total / 3, extra = total - (w * 3);
        tu3TabLeftX = left;       tu3TabMidX = left + w;      tu3TabRightX = left + w * 2;
        tu3TabLeftW = w;          tu3TabMidW = w;              tu3TabRightW = w + extra;
    }
    private String tu3PackNameAt(int idx) { return packList.getWrappedLabelForIndex(idx).getString(); }

    private int tu3MidExtra() { return Math.max(1, sc(tu3Layout.midExtra())); }

    private void renderTu3TabsBehindStrip(GuiGraphics g) {
        computeTu3Tabs();
        float sideTint = carouselNavActive() ? TU3_GREY_TINT : TU3_SIDE_TAB_TINT;
        tintBlitSprite(g, TU3_TAB_PLATE, tu3TabLeftX - TU3_TAB_OUTSET,  tu3TabY, tu3TabLeftW + TU3_TAB_OUTSET,  tu3TabH, sideTint);
        tintBlitSprite(g, TU3_TAB_PLATE, tu3TabRightX, tu3TabY, tu3TabRightW + TU3_TAB_OUTSET, tu3TabH, sideTint);
    }
    private void renderTu3Tabs(GuiGraphics g) {
        computeTu3Tabs();
        int midExtra = tu3MidExtra(), shiftUpPx = tu3Layout.activeTabLift();
        int baseY    = tu3TabY - shiftUpPx;
        int midY     = baseY - midExtra + TU3_MID_TAB_TOP_DROP, midH = tu3TabH + midExtra - TU3_MID_TAB_TOP_DROP;
        tintBlitSprite(g, TU3_TAB_PLATE, tu3TabMidX, midY, tu3TabMidW, midH, carouselNavActive() ? TU3_GREY_TINT : 1.0f);
        int baseLabelY = tu3TabY + (tu3TabH - minecraft.font.lineHeight) / 2;
        int midLabelY  = midY    + (midH   - minecraft.font.lineHeight) / 2;
        int idx        = packList.getFocusedPackIndex();
        renderTu3TabLabel(g, tu3TabLeftX - TU3_TAB_OUTSET,  tu3TabLeftW + TU3_TAB_OUTSET,  baseLabelY, tu3PackNameAt(idx - 1), CommonColor.GRAY_TEXT.get());
        renderTu3TabLabel(g, tu3TabMidX,   tu3TabMidW,   midLabelY,  tu3PackNameAt(idx),     CommonColor.GRAY_TEXT.get());
        renderTu3TabLabel(g, tu3TabRightX, tu3TabRightW + TU3_TAB_OUTSET, baseLabelY, tu3PackNameAt(idx + 1), CommonColor.GRAY_TEXT.get());
    }
    private void renderTu3TabLabel(GuiGraphics g, int x, int w, int y, String label, int color) {
        int maxPx = Math.max(1, w - sc(tu3Layout.tabLabelWidthTrim()));
        String text = PlayerSkinWidget.clipText(minecraft.font, label, maxPx);
        int drawX = x + (Math.max(1, w) - minecraft.font.width(text)) / 2;
        LegacyFontUtil.applySDFont(b -> {
            g.pose().pushMatrix();
            g.pose().translate(0.4f, 0.4f);
            g.drawString(minecraft.font, Component.literal(text), drawX, y, color, false);
            g.pose().popMatrix();
        });
    }
    private void applyTu3CarouselTuning() {
        if (playerSkinWidgetList == null) return;
        playerSkinWidgetList.setCarouselTuning(tu3Layout.carouselScale(), tu3Layout.carouselSpacing());
        SkinPack p   = packList.getFocusedPack();
        boolean fav  = p != null && SkinIdUtil.isFavouritesPack(p.id());
        playerSkinWidgetList.setAvoidRepeatsWhenFew(fav, 7);
        float mult = tu3Layout.carouselScale();
        float s0 = 0.935f * uiScale * mult, s1 = 0.77f * uiScale * mult;
        float s2 = 0.605f * uiScale * mult, s3 = 0.44f * uiScale * mult;
        float w0 = 106f * s0, w1 = 106f * s1, w2 = 106f * s2, w3 = 106f * s3;
        int   pad       = Math.max(1, sc(tu3Layout.carouselPad()));
        float available = Math.max(1f, (float) layoutW - pad * 2f);
        float sum       = w0 + (w1 + w2 + w3) * 2f;
        float gap       = (available - sum) / 6f;
        float leftEdge  = layoutX + pad;
        float cM3 = leftEdge + w3 / 2f;
        float cM2 = cM3 + w3 / 2f + gap + w2 / 2f;
        float cM1 = cM2 + w2 / 2f + gap + w1 / 2f;
        float c0  = cM1 + w1 / 2f + gap + w0 / 2f;
        float cP1 = c0  + w0 / 2f + gap + w1 / 2f;
        float cP2 = cP1 + w1 / 2f + gap + w2 / 2f;
        float cP3 = cP2 + w2 / 2f + gap + w3 / 2f;
        float spawnerExtra = Math.max(tu3Layout.spawnerExtraMin() * uiScale, w3 * tu3Layout.spawnerExtraFactor());
        float cM4 = cM3 - (w3 / 2f + gap + w3 / 2f) - spawnerExtra;
        float cP4 = cP3 + (w3 / 2f + gap + w3 / 2f) + spawnerExtra;
        int[] centers = {
            Math.round(cM4), Math.round(cM3), Math.round(cM2), Math.round(cM1), Math.round(c0),
            Math.round(cP1), Math.round(cP2), Math.round(cP3), Math.round(cP4)
        };
        playerSkinWidgetList.setCustomCarouselCenters(centers);
        int   baseCenterW    = Math.round(106f * 0.935f * uiScale);
        int   dx             = Math.round(baseCenterW * (tu3Layout.carouselScale() - 1f) / 2f);
        float areaCenter     = (layoutY + tu3BottomStripY) / 2f;
        float centerH        = 150f * s0;
        int   desiredOriginY = Math.round(areaCenter - (centerH / 2f) - tu3Layout.centerOriginYOffset() * uiScale);
        playerSkinWidgetList.setOrigin(playerSkinWidgetList.x - dx, desiredOriginY);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }
    @Override
    protected boolean handlePackListStepNavigation(int key) {
        return handlePackListStepNavigation(key, false, false, true, true);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            computeTu3Tabs();
            double mx = e.x(), my = e.y();
            int midExtra = tu3MidExtra(), shiftUpPx = tu3Layout.activeTabLift();
            int midY = tu3TabY - shiftUpPx - midExtra + TU3_MID_TAB_TOP_DROP, midH = tu3TabH + midExtra - TU3_MID_TAB_TOP_DROP;

            if (inside(mx, my, tu3TabLeftX - TU3_TAB_OUTSET,  tu3TabY, Math.max(1, tu3TabLeftW + TU3_TAB_OUTSET),  tu3TabH)) { setTu3NavZone(Tu3NavZone.PACKS); focusRelativePack(-1, false); return true; }
            if (inside(mx, my, tu3TabRightX, tu3TabY, Math.max(1, tu3TabRightW + TU3_TAB_OUTSET), tu3TabH)) { setTu3NavZone(Tu3NavZone.PACKS); focusRelativePack(1, false); return true; }
            if (inside(mx, my, tu3TabMidX,   midY,    Math.max(1, tu3TabMidW),   midH))    { setTu3NavZone(Tu3NavZone.PACKS); return true; }
        }
        if (handleCarouselMouseClicked(e, bl)) { setTu3NavZone(Tu3NavZone.CAROUSEL); return true; }
        return super.mouseClicked(e, bl);
    }
    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();
        if (handleTu3NavVertical(
                key == InputConstants.KEY_UP || key == InputConstants.KEY_W,
                key == InputConstants.KEY_DOWN || key == InputConstants.KEY_S))
            return true;
        if (handleTu3NavMove(
                key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A,
                key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D))
            return true;
        return super.keyPressed(e);
    }
    @Override
    public void bindingStateTick(BindingState state) {
        if (state != null && (state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.RIGHT_BUMPER))) {
            if (state.pressed && state.canClick()) {
                int dir = state.is(ControllerBinding.RIGHT_BUMPER) ? 1 : -1;
                if (packList.getPackCount() > 1) {
                    focusRelativePack(dir, true);
                    applyQueuedPackChange();
                }
            }
            state.block();
            return;
        }
        if (once(state, ControllerBinding.DPAD_UP) || once(state, ControllerBinding.LEFT_STICK_UP)) {
            if (handleTu3NavVertical(true, false)) return;
        }
        if (once(state, ControllerBinding.DPAD_DOWN) || once(state, ControllerBinding.LEFT_STICK_DOWN)) {
            if (handleTu3NavVertical(false, true)) return;
        }
        if (repeat(state, ControllerBinding.DPAD_LEFT) || repeat(state, ControllerBinding.LEFT_STICK_LEFT)) {
            handleTu3NavMove(true, false);
            return;
        }
        if (repeat(state, ControllerBinding.DPAD_RIGHT) || repeat(state, ControllerBinding.LEFT_STICK_RIGHT)) {
            handleTu3NavMove(false, true);
            return;
        }
        if (handleSharedBindingState(state)) return;
        if (!ControlType.getActiveType().isKbm()
                && state != null && state.is(ControllerBinding.LEFT_STICK)
                && state instanceof BindingState.Axis stick) {
            double sx = stick.x, sy = stick.y;
            if (Math.abs(sx) <= 0.45d) {
                if      (sy <= -0.65d) { if (!leftStickUpHeld)   leftStickUpHeld   = true; state.block(); return; }
                else if (sy >=  0.65d) { if (!leftStickDownHeld) leftStickDownHeld = true; state.block(); return; }
            }
            if (Math.abs(sy) < 0.25d) { leftStickUpHeld = false; leftStickDownHeld = false; }
        }
        super.bindingStateTick(state);
    }
    @Override
    public void simulateKeyAction(ControllerManager manager, BindingState state) {
        if (manager.isCursorDisabled)
            manager.simulateKeyAction(s -> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUTTON),   InputConstants.KEY_ESCAPE,   state, true);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUTTON),    InputConstants.KEY_X,        state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.UP_BUTTON),      InputConstants.KEY_O,        state);
        if (manager.isCursorDisabled) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_TRIGGER),  InputConstants.KEY_PAGEUP,   state);
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_PAGEDOWN, state);
        } else { manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state); }
        manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE),         InputConstants.KEY_F2, state);
    }
    @Override
    public void tick() {
        super.tick();
        tickScreenTail();
    }
    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);
        renderTu3TabsBehindStrip(g);
        if (tu3StripH > 0) tintBlitSprite(g, TU3_TOP_STRIP, 0, tu3StripY, width, tu3StripH, carouselNavActive() ? TU3_GREY_TINT : 1.0f);
        g.fill(0, tu3StripY + tu3StripH, Math.max(1, width), Math.max(tu3StripY + tu3StripH + 1, tu3BottomStripY), 0x880D0D0D);
        if (tu3BottomStripH > 0) tintBlitSprite(g, TU3_BOTTOM_STRIP, 0, tu3BottomStripY, width, tu3BottomStripH, carouselNavActive() ? TU3_GREY_TINT : 1.0f);
        int pad = sc(tu3Layout.carouselPad());
        PlayerSkinWidget.setCarouselClip(layoutX + pad, layoutY + pad, layoutX + layoutW - pad, tu3BottomStripY - pad);
        renderTu3Tabs(g);
        int plateH = Math.max(1, Math.round(tu3Layout.namePlateBaseHeight() * uiScale) * 2);
        int plateY = Math.max(tu3StripY + tu3StripH + Math.max(1, sc(tu3Layout.namePlateTopMargin())), tu3BottomStripY - Math.max(1, sc(tu3Layout.namePlateBottomMargin())) - plateH);
        PlayerSkinWidget.setCenterNamePlateReady(!carouselAnimating());
        PlayerSkinWidget.setCenterNamePlate(true, tu3TabMidW, plateH, 0, plateY);
        PlayerSkinWidget.setCenterNamePlateCenterX(width / 2);
        PlayerSkinWidget.setCenterNamePlateSprite(LegacySprites.SQUARE_RECESSED_PANEL);
        PlayerSkinWidget.setCenterNamePlateHighlight(carouselNavActive(), 1, 1, TU3_NAME_PLATE_HIGHLIGHT);
        int badgeW = Math.max(1, Math.round(tu3TabMidW * tu3Layout.badgeWidthRatio()));
        int badgeH = Math.max(1, Math.round(tu3Layout.badgeBaseHeight() * uiScale));
        PlayerSkinWidget.setCenterSelectedBadge(true, badgeW, badgeH, Math.max(0, sc(tu3Layout.badgeYOffset())) + Math.max(1, sc(4)), TU3_SELECTED_BADGE);
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
        PlayerSkinWidget.setCenterNamePlate(false, 1, 1, 0, -1);
        PlayerSkinWidget.setCenterNamePlateReady(true);
        PlayerSkinWidget.setCenterNamePlateCenterX(-1);
        PlayerSkinWidget.setCenterNamePlateHighlight(false, 0, 1, TU3_NAME_PLATE_HIGHLIGHT);
        PlayerSkinWidget.setCenterSelectedBadge(false, 1, 1, 0, TU3_SELECTED_BADGE);
        super.removed();
    }
}
