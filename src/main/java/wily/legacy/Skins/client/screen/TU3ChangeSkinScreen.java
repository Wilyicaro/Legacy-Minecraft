package wily.legacy.Skins.client.screen;

import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;

import wily.legacy.Skins.client.screen.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.FavoritesStore;
import wily.legacy.Skins.skin.SkinIdUtil;
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
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.util.client.LegacyRenderUtil;


public class TU3ChangeSkinScreen extends AbstractChangeSkinScreen {
    private record Tu3LayoutMetrics(
            float topStripScale,
            int bottomStripBaseHeight,
            float greyHeightRatio,
            int tabInsetNumerator,
            int midExtra,
            int activeTabLift,
            float carouselScale,
            float carouselSpacing,
            int panelTopOffset,
            int tooltipWidthOverscan,
            int tooltipBottomOverscan,
            int carouselPad,
            int namePlateBaseHeight,
            int namePlateTopMargin,
            int namePlateBottomMargin,
            float badgeWidthRatio,
            int badgeBaseHeight,
            int badgeYOffset,
            float badgeScale,
            int tabLabelWidthTrim,
            int centerOriginYOffset,
            int spawnerExtraMin,
            float spawnerExtraFactor
    ) {
        static final Tu3LayoutMetrics DEFAULT = new Tu3LayoutMetrics(
                0.60f, 20, 0.67f,
                105, 11, 2,
                1.5f, 1.1f,
                45, 23, 90,
                6,
                16, 4, 8,
                0.52f, 12, 4, 0.75f,
                16,
                20, 10, 0.35f
        );

        static final Tu3LayoutMetrics SD_480 = new Tu3LayoutMetrics(
                0.48f, 14, 0.60f,
                84, 8, 1,
                1.28f, 0.95f,
                34, 18, 72,
                4,
                14, 3, 6,
                0.48f, 10, 3, 0.70f,
                12,
                12, 8, 0.25f
        );
    }


    
    private static final ResourceLocation TU3_TOP_STRIP      = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_top_strip");
    private static final ResourceLocation TU3_BOTTOM_STRIP   = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_bottom_strip");
    private static final ResourceLocation TU3_TAB_PLATE      = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_tab_plate");
    private static final ResourceLocation TU3_NAME_PLATE     = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_nameplate");
    private static final ResourceLocation TU3_SELECTED_BADGE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected");

    
    private int layoutX, layoutY, layoutW, layoutH;
    private int tu3StripY, tu3StripH;
    private int tu3BottomStripY, tu3BottomStripH;
    private int tu3TabY, tu3TabH;
    private int tu3TabLeftX,  tu3TabMidX,  tu3TabRightX;
    private int tu3TabLeftW,  tu3TabMidW,  tu3TabRightW;

    

    private boolean lastPendingSwap;
    private Tu3LayoutMetrics tu3Layout = Tu3LayoutMetrics.DEFAULT;


    public TU3ChangeSkinScreen(Screen parent) {
        super(parent);
    }

    private void refreshTu3Layout() {
        tu3Layout = isCompact480() ? Tu3LayoutMetrics.SD_480 : Tu3LayoutMetrics.DEFAULT;
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
    protected void onWidgetListCreated(PlayerSkinWidgetList list) {
        if (list != null) list.setAlwaysVirtualCarousel(true);
    }

    @Override
    protected void onAfterSkinPackChanged() {
        applyTu3CarouselTuning();
    }

    @Override
    protected boolean insideScrollRegion(double mx, double my) {
        return inside(mx, my, layoutX, layoutY, layoutW, layoutH);
    }


    @Override
    public void renderableVListInit() {
        packList.refreshPackIdsIfNeeded();
        packList.populateInto(getRenderableVList());
        getRenderableVList().scrollArrowYOffset(0);
        getRenderableVList().init("consoleskins.packList.tu3.hidden", -100000, -100000, 1, 1);
    }

    @Override
    protected void panelInit() {
        refreshSharedLayout();
        refreshTu3Layout();
        renderableVList.layoutSpacing(l -> 0);
        packList.applyUiScale(uiScale);

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

    
    private String tu3PackNameAt(int idx) {
        idx = wrapPackIndex(idx);
        ChangeSkinPackList.PackButton b = packList.getButtonForIndex(idx);
        if (b == null || b.getMessage() == null) return "";
        String s = b.getMessage().getString();
        return s == null ? "" : s;
    }

    private int tu3MidExtra() { return Math.max(1, sc(tu3Layout.midExtra())); }

    private int wrapPackIndex(int idx) {
        int n = packList.getPackCount();
        if (n <= 0) return 0;
        int r = idx % n;
        return r < 0 ? r + n : r;
    }

    
    private void renderTu3TabsBehindStrip(GuiGraphics g) {
        computeTu3Tabs();
        blitSprite(g, TU3_TAB_PLATE, tu3TabLeftX,  tu3TabY, Math.max(1, tu3TabLeftW),  tu3TabH);
        blitSprite(g, TU3_TAB_PLATE, tu3TabRightX, tu3TabY, Math.max(1, tu3TabRightW), tu3TabH);
    }

    private void renderTu3Tabs(GuiGraphics g) {
        computeTu3Tabs();
        int midExtra = tu3MidExtra(), shiftUpPx = tu3Layout.activeTabLift();
        int baseY    = tu3TabY - shiftUpPx;
        int midY     = baseY - midExtra, midH = tu3TabH + midExtra;
        blitSprite(g, TU3_TAB_PLATE, tu3TabMidX, midY, Math.max(1, tu3TabMidW), midH);

        int baseLabelY = tu3TabY + (tu3TabH - minecraft.font.lineHeight) / 2;
        int midLabelY  = midY    + (midH   - minecraft.font.lineHeight) / 2;
        int idx        = packList.getFocusedPackIndex();
        renderTu3TabLabel(g, tu3TabLeftX,  tu3TabLeftW,  baseLabelY, tu3PackNameAt(idx - 1), 0xDDFFFFFF);
        renderTu3TabLabel(g, tu3TabMidX,   tu3TabMidW,   midLabelY,  tu3PackNameAt(idx),     0xFFFFFFFF);
        renderTu3TabLabel(g, tu3TabRightX, tu3TabRightW, baseLabelY, tu3PackNameAt(idx + 1), 0xDDFFFFFF);
    }

    private void renderTu3TabLabel(GuiGraphics g, int x, int w, int y, String label, int color) {
        if (label == null) label = "";
        int maxPx = Math.max(1, w - sc(tu3Layout.tabLabelWidthTrim()));
        String show = label;
        show = show.replace("\u00E2\u20AC\u00A6", "...");
        if (minecraft.font.width(show) > maxPx) {
            int ellW = minecraft.font.width("…");
            show = minecraft.font.plainSubstrByWidth(show, Math.max(0, maxPx - ellW)) + "…";
        }
        show = show.replace("\u00E2\u20AC\u00A6", "...");
        g.drawCenteredString(minecraft.font, Component.literal(show), x + Math.max(1, w) / 2, y, color);
    }

    
    private void applyTu3CarouselTuning() {
        if (playerSkinWidgetList == null) return;
        playerSkinWidgetList.setCarouselTuning(tu3Layout.carouselScale(), tu3Layout.carouselSpacing());

        SkinPack p   = actions.getFocusedPack();
        boolean fav  = p != null && SkinIdUtil.isFavouritesPack(p.id());
        playerSkinWidgetList.setAvoidRepeatsWhenFew(fav, 7);
        playerSkinWidgetList.clearLinearCarousel();

        
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
        boolean kbm  = ControlType.getActiveType().isKbm();
        boolean up   = key == InputConstants.KEY_UP;
        boolean down = key == InputConstants.KEY_DOWN;
        if (!(up || down)) return false;

        
        if (!kbm) return true;

        var f = getFocused();
        if (!(f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton)) return false;

        int count = packList.getPackCount();
        if (count <= 1) return true;

        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1; else if (target >= count) target = 0;

        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return true;

        packList.setFocusedPackIndex(target, true);
        setFocused(btn);
        focusPackListItem(btn);

        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();
            skinPack(resolveSelectedSkinIndex());
        }
        return true;
    }

    private void stepPack(boolean up) {
        int count = packList != null ? packList.getPackCount() : 0;
        if (count <= 1) return;
        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1; else if (target >= count) target = 0;
        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return;
        packList.setFocusedPackIndex(target, true);
        setFocused(btn);
        focusPackListItem(btn);
        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();
            skinPack(resolveSelectedSkinIndex());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            computeTu3Tabs();
            double mx = e.x(), my = e.y();
            int midExtra = tu3MidExtra(), shiftUpPx = tu3Layout.activeTabLift();
            int midY = tu3TabY - shiftUpPx - midExtra, midH = tu3TabH + midExtra;

            if (inside(mx, my, tu3TabLeftX,  tu3TabY, Math.max(1, tu3TabLeftW),  tu3TabH)) { packList.setFocusedPackIndex(packList.getFocusedPackIndex() - 1, true); return true; }
            if (inside(mx, my, tu3TabRightX, tu3TabY, Math.max(1, tu3TabRightW), tu3TabH)) { packList.setFocusedPackIndex(packList.getFocusedPackIndex() + 1, true); return true; }
            if (inside(mx, my, tu3TabMidX,   midY,    Math.max(1, tu3TabMidW),   midH))    { return true; }
        }

        if (handleCarouselMouseClicked(e, bl)) return true;
        return super.mouseClicked(e, bl);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        
        if (state != null && (state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.RIGHT_BUMPER))) {
            if (state.pressed && state.canClick()) {
                int dir = state.is(ControllerBinding.RIGHT_BUMPER) ? 1 : -1;
                if (packList.getPackCount() > 1) {
                    int target = wrapPackIndex(packList.getFocusedPackIndex() + dir);
                    ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
                    packList.setFocusedPackIndex(target, true);
                    if (btn != null) setFocused(btn);
                    if (packList.consumeQueuedChangePack()) {
                        stopHoldingOuterCarousel();
                        cancelQueuedCarousel();
                        skinPack(resolveSelectedSkinIndex());
                    }
                }
            }
            state.block();
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
        } else {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
        }
        manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE),         InputConstants.KEY_F2, state);
    }

    @Override
    public void tick() {
        super.tick();

        
        boolean wasPending = false;
        try { wasPending = actions.isPendingSwap(); } catch (Throwable ignored) {}

        if (sharedTick()) return;

        boolean isPending = false;
        try { isPending = actions.isPendingSwap(); } catch (Throwable ignored) {}
        if ((wasPending || lastPendingSwap) && !isPending) applyTu3CarouselTuning();
        lastPendingSwap = isPending;

        pumpQueuedCarousel();
        pumpHoldingOuterCarousel();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false, false, false);

        renderTu3TabsBehindStrip(g);

        if (tu3StripH > 0) blitSprite(g, TU3_TOP_STRIP, 0, tu3StripY, Math.max(1, width), Math.max(1, tu3StripH));

        g.fill(0, tu3StripY + tu3StripH, Math.max(1, width), Math.max(tu3StripY + tu3StripH + 1, tu3BottomStripY), 0x880D0D0D);

        if (tu3BottomStripH > 0) blitSprite(g, TU3_BOTTOM_STRIP, 0, tu3BottomStripY, Math.max(1, width), Math.max(1, tu3BottomStripH));

        int pad = sc(tu3Layout.carouselPad());
        PlayerSkinWidget.setCarouselClip(layoutX + pad, layoutY + pad, layoutX + layoutW - pad, tu3BottomStripY - pad);
        PlayerSkinWidget.setCarouselYawDenom(Math.max(1f, 240f * uiScale));

        renderTu3Tabs(g);

        
        
        
        int plateH = Math.max(1, Math.round(tu3Layout.namePlateBaseHeight() * uiScale) * 2);
        int plateY = Math.max(tu3StripY + tu3StripH + Math.max(1, sc(tu3Layout.namePlateTopMargin())), tu3BottomStripY - Math.max(1, sc(tu3Layout.namePlateBottomMargin())) - plateH);
        PlayerSkinWidget.setCenterNamePlate(true, tu3TabMidW, plateH, 0, plateY);
        PlayerSkinWidget.setCenterNamePlateCenterX(width / 2);
        PlayerSkinWidget.setCenterNamePlateSprite(TU3_NAME_PLATE);
        int badgeW = Math.max(1, Math.round(tu3TabMidW * tu3Layout.badgeWidthRatio()));
        int badgeH = Math.max(1, Math.round(tu3Layout.badgeBaseHeight() * uiScale));
        PlayerSkinWidget.setCenterSelectedBadge(true, badgeW, badgeH, Math.max(0, sc(tu3Layout.badgeYOffset())), tu3Layout.badgeScale(), TU3_SELECTED_BADGE);
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
                    ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_A), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_D)})
                    : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.literal("Navigate"));
            if (canUsePackFilter()) {
                r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_T) : ControllerBinding.BACK.bindingState.getIcon(), this::currentPackFilterLabel);
            }
            r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> Component.literal("Advanced Options"));
        }
    }

    @Override
    public void removed() {
        PlayerSkinWidget.setCenterNamePlate(false, 1, 1, 0, -1);
        PlayerSkinWidget.setCenterNamePlateCenterX(-1);
        PlayerSkinWidget.setCenterSelectedBadge(false, 1, 1, 0, 1f, TU3_SELECTED_BADGE);
        super.removed();
    }
}
