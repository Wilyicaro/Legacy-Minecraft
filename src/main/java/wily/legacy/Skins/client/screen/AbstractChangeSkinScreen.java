package wily.legacy.Skins.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Skins.client.changeskin.*;
import wily.legacy.Skins.client.changeskin.ChangeSkinActions.ChangeSkinLayoutMetrics;
import wily.legacy.Skins.client.preview.*;
import wily.legacy.Skins.client.util.*;
import wily.legacy.Skins.skin.*;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacyComponents;

public abstract class AbstractChangeSkinScreen extends PanelVListScreen
        implements wily.legacy.client.controller.Controller.Event, ControlTooltip.Event, InputTypeSwitchLock {
    protected record ChangeSkinScreenLayout(boolean compact480, int basePanelWidth, int basePanelHeight, int baseTooltipWidth,
                                            int previewBoxMinSize, int previewBoxBaseSize, int previewBoxXOffset, int previewBoxMaxInset,
                                            int previewBoxRightInset, int previewBoxYOffset, int previewBoxTopInset, int previewBoxBottomInset,
                                            int tooltipGroupMargin, int tooltipYOffset, int tooltipHeightInset,
                                            float bigTextScale, float bigTextMinScale, float smallTextScale, float smallTextMinScale,
                                            ChangeSkinLayoutMetrics widgetMetrics) {
        static final ChangeSkinScreenLayout DEFAULT = new ChangeSkinScreenLayout(
                false, 166, 262, 360,
                22, 104, 30, 18, 6,
                8, 4, 4, 6, 16, 9,
                1.42f, 0.65f, 1.00f, 0.60f,
                ChangeSkinLayoutMetrics.DEFAULT
        );

        static final ChangeSkinScreenLayout SD_480 = new ChangeSkinScreenLayout(
                true, 150, 198, 286,
                18, 72, 22, 14, 6,
                6, 4, 4, 4, 12, 8,
                1.14f, 0.56f, 0.88f, 0.52f,
                ChangeSkinLayoutMetrics.SD_480
        );
    }

    @Override
    public void tick() {
        super.tick();
        int reloadVersion = SkinPackLoader.getReloadVersion();
        if (reloadVersion != seenPackReloadVersion) {
            seenPackReloadVersion = reloadVersion;
            handleSkinPackReload();
            return;
        }
        boolean windowActive = minecraft == null || minecraft.isWindowActive();
        if (windowActive && !lastWindowActive) { onWindowRegainedFocus(); }
        lastWindowActive = windowActive;
        int budget = 2;
        if (queuedCarouselSteps > 0 || outerCarouselHold.active() || carouselAnimating()) budget = 3;
        SkinPreviewWarmup.pump(minecraft, budget);
    }

    protected final Minecraft minecraft;

    protected final Panel tooltipBox;

    protected final ChangeSkinPackList packList;
    protected final ChangeSkinActions  actions;

    protected float uiScale   = 1f;
    protected int tooltipWidth = ChangeSkinScreenLayout.DEFAULT.baseTooltipWidth();
    protected ChangeSkinScreenLayout layoutProfile = ChangeSkinScreenLayout.DEFAULT;
    protected boolean sdMode;

    protected final Map<ResourceLocation, int[]> packIconDims = new HashMap<>();
    protected boolean stickUpHeld, stickDownHeld, leftStickUpHeld, leftStickDownHeld, shiftHeld, pHeld, enterHeld;
    protected boolean firstOpen = true;
    protected boolean draggingCenterDoll, centerDragMoved, queuedCarouselSound;
    protected double centerDragStartX, centerDragStartY;
    protected int queuedCarouselSteps, queuedCarouselDir;
    protected final HoldRepeat outerCarouselHold = new HoldRepeat();
    protected PlayerSkinWidgetList playerSkinWidgetList;
    protected boolean lastWindowActive;
    protected int seenPackReloadVersion;

    protected AbstractChangeSkinScreen(Screen parent) {
        super(parent, s -> {
            ChangeSkinScreenLayout layout = LegacyOptions.getUIMode().isSD()
                    ? ChangeSkinScreenLayout.SD_480
                    : ChangeSkinScreenLayout.DEFAULT;
            float scale = computeScale(s.width, s.height, layout);
            return Panel.centered(s,
                    () -> Math.max(1, Math.round(layout.basePanelWidth() * scale)),
                    () -> Math.max(1, Math.round(layout.basePanelHeight() * scale)),
                    0, 0);
        }, Component.empty());

        minecraft  = Minecraft.getInstance();
        lastWindowActive = minecraft == null || minecraft.isWindowActive();
        tooltipBox = createTooltipBox();

        renderableVList.layoutSpacing(l -> 2);

        packList = new ChangeSkinPackList(this::playClick);

        actions = new ChangeSkinActions(minecraft, packList, new ChangeSkinActions.Host() {
            @Override public PlayerSkinWidgetList getPlayerSkinWidgetList()              { return playerSkinWidgetList; }
            @Override public void setPlayerSkinWidgetList(PlayerSkinWidgetList list) {
                playerSkinWidgetList = list;
                onWidgetListCreated(list);
            }
            @Override public void addSkinWidget(PlayerSkinWidget w)                     { addRenderableWidget(w); }
            @Override public Panel             getTooltipBox()                          { return tooltipBox; }
            @Override public Panel             getPanel()                               { return panel; }
            @Override public Screen            getScreen()                              { return AbstractChangeSkinScreen.this; }
            @Override public float             getUiScale()                             { return uiScale; }
            @Override public ChangeSkinLayoutMetrics getLayoutMetrics()                 { return layoutProfile.widgetMetrics(); }
        });

        SkinPackLoader.ensureLoaded();

        packList.initFromLoader();
        seenPackReloadVersion = SkinPackLoader.getReloadVersion();
    }

    protected abstract Panel createTooltipBox();

    protected void onWidgetListCreated(PlayerSkinWidgetList list) {}

    protected void onAfterSkinPackChanged() {}

    protected abstract boolean insideScrollRegion(double mx, double my);

    protected void playClick() { actions.playClick(); }

    protected boolean isInCarouselBounds(double mx, double my) { return true; }

    protected abstract boolean handlePackListStepNavigation(int key);

    protected void handleSkinPackReload() {
        syncCenterPreviewState();
        packIconDims.clear();
        SkinPreviewWarmup.clear();
        packList.refreshPackIdsIfNeeded();
        rebuildWidgets();
    }

    protected void onWindowRegainedFocus() {
        PlayerSkinWidget.clearCarouselClip();
        if (playerSkinWidgetList != null) { skinPack(); }
    }

    protected void focusPackListItem(Object item) {
        if (item instanceof ChangeSkinPackList.PackButton button && children().contains(button)) { setFocused(button); }
    }

    protected int sc(int v) { return Math.round(v * uiScale); }

    protected ChangeSkinScreenLayout resolveRuntimeLayout() { return sdMode ? ChangeSkinScreenLayout.SD_480 : ChangeSkinScreenLayout.DEFAULT; }

    protected void refreshSharedLayout() {
        sdMode = LegacyOptions.getUIMode().isSD();
        layoutProfile = resolveRuntimeLayout();
        uiScale = computeScale(width, height, layoutProfile);
        tooltipWidth = Math.max(1, Math.round(layoutProfile.baseTooltipWidth() * uiScale));
    }

    protected void applyResolvedPanelBounds() {
        int panelWidth = Math.max(1, Math.round(layoutProfile.basePanelWidth() * uiScale));
        int panelHeight = Math.max(1, Math.round(layoutProfile.basePanelHeight() * uiScale));
        panel.size(panelWidth, panelHeight);
        panel.pos((width - panelWidth) / 2, (height - panelHeight) / 2);
    }

    protected boolean isCompact480() { return sdMode; }

    protected ChangeSkinLayoutMetrics getLayoutMetrics() { return layoutProfile.widgetMetrics(); }

    protected int tooltipContentTop() { return panel.y + sc(getLayoutMetrics().tooltipTopOffset()); }

    protected int tooltipContentRight() { return tooltipBox.x + tooltipBox.getWidth() - sc(getLayoutMetrics().tooltipWidthTrim()); }

    protected int tooltipContentBottom() {
        ChangeSkinLayoutMetrics metrics = getLayoutMetrics();
        return tooltipContentTop() + tooltipBox.getHeight() - sc(metrics.tooltipHeightTrim() + metrics.tooltipFooterHeight() - metrics.tooltipHeightRecover());
    }

    protected float bigTextScale() { return Math.max(layoutProfile.bigTextMinScale(), layoutProfile.bigTextScale() * uiScale); }

    protected float smallTextScale() { return Math.max(layoutProfile.smallTextMinScale(), layoutProfile.smallTextScale() * uiScale); }

    protected static int controlTooltipFooterReserve() {
        float hudDistance = Math.max(0.0f, LegacyOptions.hudDistance.get().floatValue() - 0.5f) * 2.0f;
        float hudDiff = 1.0f - hudDistance;
        float tooltipTopOffset = 29.0f - (15.0f - ControlType.getActiveType().iconHeight()) / 2.0f - 16.0f * hudDiff;
        return Math.max(24, Math.round(tooltipTopOffset + 12.0f));
    }

    protected static float computeScale(int w, int h, ChangeSkinScreenLayout layout) {
        float groupWidth = layout.basePanelWidth() + layout.baseTooltipWidth() - 2f;
        float groupHeight = layout.basePanelHeight() + Math.max(0f, layout.tooltipYOffset() - layout.tooltipHeightInset());
        float footerReserve = controlTooltipFooterReserve();
        if (layout.compact480()) {
            groupWidth += 10f;
            groupHeight += 22f;
            footerReserve += 10f;
        }
        float sw = (w - 20f) / groupWidth;
        float sh = (h - 20f - footerReserve) / groupHeight;
        float sc = Math.min(1f, Math.min(sw, sh));
        if (layout.compact480()) {
            sc = Math.min(sc, 0.82f);
            sc *= 0.88f;
        }
        if (sc > 0.8f) sc *= 0.92f;
        sc *= 0.93f;
        if (sc <= 0f) sc = 1f;
        return sc;
    }

    protected int[] packIconDims(ResourceLocation icon) {
        int[] d = packIconDims.get(icon);
        if (d != null) return d;
        int w = 128, h = 128;
        try {
            Resource r = minecraft.getResourceManager().getResource(icon).orElse(null);
            if (r != null) {
                try (var in = r.open(); NativeImage img = NativeImage.read(in)) {
                    w = img.getWidth();
                    h = img.getHeight();
                }
            }
        } catch (IOException | RuntimeException ignored) {}
        int[] out = {Math.max(1, w), Math.max(1, h)};
        packIconDims.put(icon, out);
        return out;
    }

    protected int previewBoxSize() {
        int min  = Math.max(1, sc(layoutProfile.previewBoxMinSize()));
        int size = Math.max(1, sc(layoutProfile.previewBoxBaseSize()));
        int max  = panel.width - sc(layoutProfile.previewBoxMaxInset());
        if (max < size) size = Math.max(min, max);
        return Math.max(1, size);
    }

    protected int previewBoxX() {
        int s     = previewBoxSize();
        int x     = panel.x + sc(layoutProfile.previewBoxXOffset());
        int right = panel.x + panel.width - sc(layoutProfile.previewBoxRightInset());
        if (x + s > right) x = panel.x + Math.max(sc(7), (panel.width - s) / 2);
        return x;
    }

    protected int previewBoxY() {
        int s      = previewBoxSize();
        int y      = panel.y + sc(layoutProfile.previewBoxYOffset());
        int top    = panel.y + sc(layoutProfile.previewBoxTopInset());
        int bottom = panel.y + panel.height - s - sc(layoutProfile.previewBoxBottomInset());
        if (bottom < top) bottom = top;
        return Math.max(top, Math.min(y, bottom));
    }

    protected void skinPack(int i) {
        syncCenterPreviewState();
        actions.skinPack(i);
        onAfterSkinPackChanged();
    }

    protected void skinPack() {
        syncCenterPreviewState();
        actions.skinPack();
        onAfterSkinPackChanged();
    }

    protected PlayerSkinWidget getCenterWidget() { return playerSkinWidgetList == null ? null : playerSkinWidgetList.getCenter(); }

    protected String currentAppliedSkinId() {
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        return self == null ? null : ClientSkinCache.get(self);
    }

    protected Component favoriteActionLabel() {
        PlayerSkinWidget center = getCenterWidget();
        String skinId = center == null ? null : center.skinId.get();
        return skinId != null && FavoritesStore.isFavorite(skinId)
                ? Component.literal("Remove Favorite")
                : Component.literal("Add Favorite");
    }

    protected int resolveFocusedPackSkinIndex(String skinId) {
        if (skinId == null || skinId.isBlank()) return 0;
        SkinPack focused = packList.getFocusedPack();
        if (focused == null || focused.skins() == null) return 0;
        int limit = Math.min(100, focused.skins().size());
        for (int i = 0; i < limit; i++) {
            SkinEntry entry = focused.skins().get(i);
            if (entry != null && skinId.equals(entry.id())) return i;
        }
        return 0;
    }

    protected void focusInitialPack() {
        String selectedId = currentAppliedSkinId();
        if (selectedId != null && !selectedId.isBlank()) {
            String sourcePackId = SkinPackLoader.getSourcePackId(selectedId);
            if (sourcePackId != null) {
                packList.focusPackId(sourcePackId, false);
                return;
            }
        }

        String preferredDefaultPackId = SkinPackLoader.getPreferredDefaultPackId();
        String focusId = preferredDefaultPackId != null ? preferredDefaultPackId : SkinPackLoader.getLastUsedCustomPackId();
        if (focusId != null) packList.focusPackId(focusId, false);
    }

    protected boolean focusPackIndex(int index, boolean requestFocus) {
        int count = packList.getPackCount();
        if (count <= 1) return false;
        int target = Math.floorMod(index, count);
        ChangeSkinPackList.PackButton button = packList.getButtonForIndex(target);
        if (button == null) return false;
        packList.setFocusedPackIndex(target, true);
        if (requestFocus) {
            setFocused(button);
            focusPackListItem(button);
        }
        return true;
    }

    protected boolean focusRelativePack(int delta, boolean requestFocus) { return delta != 0 && focusPackIndex(packList.getFocusedPackIndex() + delta, requestFocus); }

    protected boolean applyQueuedPackChange() {
        if (!packList.consumeQueuedChangePack()) return false;
        stopHoldingOuterCarousel();
        cancelQueuedCarousel();
        skinPack(resolveFocusedPackSkinIndex(currentAppliedSkinId()));
        return true;
    }

    private void sortCarouselBy(int offset) {
        playerSkinWidgetList.setCenterRotation(0, 0);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + offset);
    }

    protected boolean control(boolean left, boolean right) {
        if (!(left || right) || playerSkinWidgetList == null) return false;
        if (playerSkinWidgetList.widgets.stream().anyMatch(w -> w.progress <= 1f)) return true;
        sortCarouselBy((left ? -1 : 0) + (right ? 1 : 0));
        actions.playClick();
        return true;
    }

    protected boolean carouselAnimating() {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return false;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets)
            if (w != null && w.visible && w.progress <= 1f) return true;
        return false;
    }

    @Override
    public boolean legacy$lockInputTypeSwitch() { return carouselAnimating(); }

    protected PlayerSkinWidget getControllableCenterPreview() {
        PlayerSkinWidget center = getCenterWidget();
        if (center == null) return null;
        if (!center.visible || center.isInterpolating()) return null;
        return center;
    }

    protected void syncCenterPreviewState(PlayerSkinWidget center) {
        if (center == null || playerSkinWidgetList == null) return;
        playerSkinWidgetList.setCenterRotation(center.getRotationX(), center.getRotationY());
        playerSkinWidgetList.setCenterPose(center.getPoseMode(), center.isPunchLoop());
    }

    protected void syncCenterPreviewState() { syncCenterPreviewState(getCenterWidget()); }

    private boolean updateCenterPreview(boolean playClick, Consumer<PlayerSkinWidget> action) {
        PlayerSkinWidget center = getControllableCenterPreview();
        if (center == null) return false;
        action.accept(center);
        syncCenterPreviewState(center);
        if (playClick) actions.playClick();
        return true;
    }

    protected boolean rotateCenterPreview(double dragX, double dragY) { return updateCenterPreview(false, center -> center.applyDrag(dragX, dragY)); }

    protected void addCommonControlTooltips(ControlTooltip.Renderer r, Supplier<ControlTooltip.Icon> navigateIcon, Supplier<Component> navigateLabel) {
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), () -> Component.literal("Select"));
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.cancel"));
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_F) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), this::favoriteActionLabel);
        r.add(navigateIcon, navigateLabel);
        if (LegacyOptions.hideAdvancedOptionsTooltip.get() || LegacyOptions.legacySettingsMenus.get()) return;
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> LegacyComponents.SHOW_ADVANCED_OPTIONS);
    }

    protected PlayerSkinWidget pickCarouselWidget(double mx, double my) {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return null;
        if (!isInCarouselBounds(mx, my)) return null;
        PlayerSkinWidget best  = null;
        double           bestD = Double.MAX_VALUE;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets) {
            if (w == null || !w.visible) continue;
            int wx = w.getX(), wy = w.getY(), ww = w.getWidth(), wh = w.getHeight();
            if (!inside(mx, my, wx, wy, ww, wh)) continue;
            double cx = wx + ww * 0.5, cy = wy + wh * 0.5;
            double dx = mx - cx,       dy = my - cy;
            double d  = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = w; }
        }
        return best;
    }

    protected void cancelQueuedCarousel() {
        queuedCarouselSteps = 0;
        queuedCarouselDir   = 0;
        queuedCarouselSound = false;
    }

    protected void pumpQueuedCarousel() {
        if (queuedCarouselSteps <= 0) return;
        if (carouselAnimating()) return;
        if (playerSkinWidgetList == null) { cancelQueuedCarousel(); return; }
        if (queuedCarouselSound) { queuedCarouselSound = false; actions.playClick(); }
        sortCarouselBy(queuedCarouselDir);
        if (--queuedCarouselSteps <= 0) cancelQueuedCarousel();
    }

    protected boolean startQueuedCarousel(int offset) {
        if (playerSkinWidgetList == null) return false;
        if (carouselAnimating()) return false;
        int abs = Math.abs(offset);
        if (abs == 0) return false;
        if (abs == 1) {
            sortCarouselBy(offset);
            actions.playClick();
            return true;
        }
        queuedCarouselDir   = offset > 0 ? 1 : -1;
        queuedCarouselSteps = abs;
        queuedCarouselSound = true;
        pumpQueuedCarousel();
        return true;
    }

    protected void startHoldingOuterCarousel(int dir) { outerCarouselHold.start(dir); }

    protected void stopHoldingOuterCarousel() { outerCarouselHold.stop(); }

    protected void pumpHoldingOuterCarousel() {
        if (!outerCarouselHold.ready() || queuedCarouselSteps > 0 || carouselAnimating()) return;
        startQueuedCarousel(outerCarouselHold.dir());
        outerCarouselHold.step();
    }

    @Override
    protected void init() {
        super.init();

        ChangeSkinPackList.PackButton b = packList.getButtonForIndex(packList.getFocusedPackIndex());
        if (b != null && children().contains(b))
            setFocused(b);
        else
            for (var c : children())
                if (c instanceof ChangeSkinPackList.PackButton pb) { setFocused(pb); break; }

        boolean queuedPackChange = packList.consumeQueuedChangePack();
        if (firstOpen) {
            firstOpen = false;
            skinPack(resolveFocusedPackSkinIndex(currentAppliedSkinId()));
        } else if (queuedPackChange) {
            skinPack(0);
        } else if (playerSkinWidgetList != null) { skinPack(playerSkinWidgetList.index); }
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();

        if (key == InputConstants.KEY_RETURN) {
            if (!enterHeld) enterHeld = true;
            return true;
        }
        if (key == InputConstants.KEY_F) { actions.favorite(); return true; }

        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
            if (!shiftHeld) {
                shiftHeld = true;
                updateCenterPreview(true, PlayerSkinWidget::togglePose);
            }
            return true;
        }
        if (key == InputConstants.KEY_P) {
            if (!pHeld) {
                pHeld = true;
                updateCenterPreview(true, PlayerSkinWidget::togglePunch);
            }
            return true;
        }
        if (key == InputConstants.KEY_O) { actions.openLegacyChangeSkinScreen(); return true; }

        if (handlePackListStepNavigation(key)) return true;

        if (control(
                key == InputConstants.KEY_LEFT || key == InputConstants.KEY_LBRACKET || key == InputConstants.KEY_A,
                key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_RBRACKET || key == InputConstants.KEY_D))
            return true;

        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();
        if (key == InputConstants.KEY_RETURN) {
            if (enterHeld) { enterHeld = false; actions.selectSkin(); }
            return true;
        }
        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) { shiftHeld = false; return true; }
        if (key == InputConstants.KEY_P)                                           { pHeld    = false; return true; }
        return super.keyReleased(e);
    }

    protected boolean handleCarouselMouseClicked(MouseButtonEvent e, boolean bl) {
        double mx = e.x(), my = e.y();
        PlayerSkinWidget hit = pickCarouselWidget(mx, my);
        if (hit == null) return false;
        if (carouselAnimating()) return true;

        if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            updateCenterPreview(true, PlayerSkinWidget::recenterView);
            return true;
        }
        if (e.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

        int off = hit.slotOffset;
        if (off == 0) {
            stopHoldingOuterCarousel();
            draggingCenterDoll  = true;
            centerDragMoved     = false;
            centerDragStartX    = mx;
            centerDragStartY    = my;
            return true;
        }

        stopHoldingOuterCarousel();
        boolean moved = startQueuedCarousel(off);
        if (moved && Math.abs(off) == 2) startHoldingOuterCarousel(off);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            PlayerSkinWidget center = getCenterWidget();
            if (center != null && center.visible && !center.isInterpolating()) {
                if (!centerDragMoved) {
                    if (Math.abs(event.x() - centerDragStartX) > 2.0 || Math.abs(event.y() - centerDragStartY) > 2.0)
                        centerDragMoved = true;
                }
                if (centerDragMoved) { rotateCenterPreview(-dragX, 0); }
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && outerCarouselHold.active())
            stopHoldingOuterCarousel();

        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            draggingCenterDoll = false;

            if (!centerDragMoved) actions.selectSkin();
            centerDragMoved = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (g == 0) return false;

        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList != null) { vList.mouseScrolled(g); return true; }

        if (playerSkinWidgetList == null) return false;
        if (!insideScrollRegion(d, e) && pickCarouselWidget(d, e) == null) return false;
        if (draggingCenterDoll) return true;
        if (carouselAnimating()) return true;

        startQueuedCarousel(g > 0 ? -1 : 1);
        return true;
    }

    private static boolean buttonOnce(BindingState state, ControllerBinding binding) { return state != null && state.is(binding) && state.onceClick(true); }

    protected boolean handleSharedBindingState(BindingState state) {
        if (buttonOnce(state, ControllerBinding.LEFT_BUTTON)) { actions.favorite(); return true; }

        if (!ControlType.getActiveType().isKbm() && buttonOnce(state, ControllerBinding.UP_BUTTON)) {
            actions.openLegacyChangeSkinScreen();
            return true;
        }

        if (!ControlType.getActiveType().isKbm() && buttonOnce(state, ControllerBinding.RIGHT_STICK_BUTTON)) {
            updateCenterPreview(true, PlayerSkinWidget::recenterView);
            return true;
        }

        if (getCenterWidget() != null && state != null && state.is(ControllerBinding.RIGHT_STICK)
                && state instanceof BindingState.Axis stick) {
            PlayerSkinWidget center = getControllableCenterPreview();
            if (center != null) {
                final double triggerY = 0.85d, sideLimit = 0.35d;
                double sx = stick.x, sy = stick.y;

                if (Math.abs(sx) <= sideLimit) {
                    if (sy <= -triggerY) {
                        if (!stickUpHeld) {
                            stickUpHeld = true;
                            updateCenterPreview(true, preview -> {
                                if (preview.isPunchLoop()) preview.setPoseMode(1, false, false);
                                else preview.togglePunch();
                            });
                        }
                        state.block();
                        return true;
                    }
                    if (sy >= triggerY) {
                        if (!stickDownHeld) {
                            stickDownHeld = true;
                            updateCenterPreview(true, preview -> {
                                if (preview.getPoseMode() == 1) preview.setPoseMode(0, true, false);
                                else preview.togglePose();
                            });
                        }
                        state.block();
                        return true;
                    }
                }
                if (Math.abs(sy) < 0.25d) { stickUpHeld = false; stickDownHeld = false; }

                double dz = stick.getDeadZone();
                double dx = dz > Math.abs(sx) ? 0 : -sx * 0.12;
                if (dx != 0) {
                    if (rotateCenterPreview(dx, 0)) {
                        state.block();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected boolean sharedTick() {
        PlayerSkinWidget center = getCenterWidget();
        if (center != null && !center.isInterpolating()) syncCenterPreviewState(center);
        return applyQueuedPackChange();
    }

    @Override
    public void removed() {
        stickUpHeld = stickDownHeld = leftStickUpHeld = leftStickDownHeld = false;
        shiftHeld = pHeld = enterHeld = false;
        stopHoldingOuterCarousel();
        draggingCenterDoll = centerDragMoved = false;
        PlayerSkinWidget.clearCarouselClip();
        SkinPreviewWarmup.clear();
        super.removed();
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    protected static void blitSprite(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) { FactoryGuiGraphics.of(g).blitSprite(id, x, y, w, h); }

    protected void drawBigCentered(GuiGraphics g, Component text, int centerX, int y, int color) { drawScaledCentered(g, text, centerX, y, color, bigTextScale()); }

    protected void drawSmallCentered(GuiGraphics g, Component text, int centerX, int y, int color) { drawScaledCentered(g, text, centerX, y, color, smallTextScale()); }

    private void drawScaledCentered(GuiGraphics g, Component text, int centerX, int y, int color, float scale) {
        int yAdj = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawCenteredString(minecraft.font, text, 0, 0, color);
        pose.popMatrix();
    }
    protected static final class HoldRepeat {
        private int dir;
        private long startAt, nextAt;
        boolean active() { return dir != 0; }
        int dir() { return dir; }
        void start(int dir) { this.dir = dir < 0 ? -1 : 1; long now = net.minecraft.Util.getMillis(); startAt = now; nextAt = now + 220L; }
        void stop() { dir = 0; startAt = 0L; nextAt = 0L; }
        boolean ready() { return active() && net.minecraft.Util.getMillis() >= nextAt; }
        void step() { long now = net.minecraft.Util.getMillis(); nextAt = now + (now - startAt < 700L ? 120L : 80L); }
    }
}
