package wily.legacy.skins.client.screen;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.packs.resources.Resource;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.skins.client.changeskin.*;
import wily.legacy.skins.client.preview.*;
import wily.legacy.skins.client.util.*;
import wily.legacy.skins.skin.*;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacySoundUtil;
public abstract class AbstractChangeSkinScreen extends PanelVListScreen
        implements wily.legacy.client.controller.Controller.Event, ControlTooltip.Event, InputTypeSwitchLock {
    private static final int[] WARMUP_OFFSETS = {0, -1, 1, -2, 2, -3, 3};
    protected record ChangeSkinLayoutMetrics(
            int tooltipTopOffset, int tooltipWidthTrim, int tooltipHeightTrim, int tooltipFooterHeight, int tooltipHeightRecover, int carouselClipInset,
            int carouselClipBottomTrim, float centerScale, float rightCardScale, int originPadX, int originPadY, int panelMarginX, int carouselOffset,
            int minLeftClearance, int rightCardPadding
    ) {
        static final ChangeSkinLayoutMetrics DEFAULT = new ChangeSkinLayoutMetrics(45, 23, 80, 50, 40, 2, 24, 0.935f, 0.44f, 8, 20, 6, 80, 88, 6);
        static final ChangeSkinLayoutMetrics SD_480 = new ChangeSkinLayoutMetrics(34, 18, 64, 40, 30, 2, 18, 0.76f, 0.36f, 6, 12, 5, 58, 62, 4);
    }
    protected record ChangeSkinScreenLayout(
            boolean compact480, int basePanelWidth, int basePanelHeight, int baseTooltipWidth, int previewBoxMinSize, int previewBoxBaseSize,
            int previewBoxXOffset, int previewBoxMaxInset, int previewBoxRightInset, int previewBoxYOffset, int previewBoxTopInset, int previewBoxBottomInset,
            int tooltipGroupMargin, int tooltipYOffset, int tooltipHeightInset, float bigTextScale, float bigTextMinScale, float smallTextScale,
            float smallTextMinScale, ChangeSkinLayoutMetrics widgetMetrics
    ) {
        static final ChangeSkinScreenLayout DEFAULT = new ChangeSkinScreenLayout(false, 166, 262, 360, 22, 104, 30, 18, 6, 8, 4, 4, 6, 16, 9, 1.42f, 0.65f, 1.00f, 0.60f, ChangeSkinLayoutMetrics.DEFAULT);
        static final ChangeSkinScreenLayout SD_480 = new ChangeSkinScreenLayout(true, 150, 198, 286, 18, 72, 22, 14, 6, 6, 4, 4, 4, 12, 8, 1.14f, 0.56f, 0.88f, 0.52f, ChangeSkinLayoutMetrics.SD_480);
    }
    @Override
    public void tick() {
        super.tick();
        int reloadVersion = source.version();
        if (reloadVersion != seenPackReloadVersion) {
            seenPackReloadVersion = reloadVersion;
            handleSkinPackReload();
            return;
        }
        applyPendingInitialPackFocus();
        boolean windowActive = minecraft == null || minecraft.isWindowActive();
        if (windowActive && !lastWindowActive) { onWindowRegainedFocus(); }
        lastWindowActive = windowActive;
        int budget = 2;
        if (queuedCarouselSteps > 0 || outerCarouselHold.active() || carouselAnimating()) budget = 3;
        ClientSkinAssets.pumpPreviewWarmup(minecraft, budget);
    }
    protected final Minecraft minecraft;
    protected final ChangeSkinScreenSource source;
    protected final Panel tooltipBox;
    protected final ChangeSkinPackList packList;
    protected float uiScale = 1f;
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
    protected final List<PlayerSkinWidget> previewWidgets = new ArrayList<>(9);
    protected PlayerSkinWidgetList playerSkinWidgetList;
    protected boolean lastWindowActive;
    protected int seenPackReloadVersion;
    protected final CustomSkinPackFlow customPacks;
    protected String pendingInitialPackId;
    protected AbstractChangeSkinScreen(Screen parent) {
        this(parent, ChangeSkinScreenSource.Default.INSTANCE);
    }

    protected AbstractChangeSkinScreen(Screen parent, ChangeSkinScreenSource source) {
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
        this.source = source;
        lastWindowActive = minecraft == null || minecraft.isWindowActive();
        tooltipBox = createTooltipBox();
        renderableVList.layoutSpacing(l -> 2);
        packList = new ChangeSkinPackList(source, this::playFocusSound, this::playPackSelectionSound);
        packList.init();
        seenPackReloadVersion = source.version();
        customPacks = new CustomSkinPackFlow(this);
    }
    protected abstract Panel createTooltipBox();
    protected void onWidgetListCreated(PlayerSkinWidgetList list) { }
    protected void onAfterSkinPackChanged() { }
    protected abstract boolean insideScrollRegion(double mx, double my);
    protected void ensurePreviewWidgets() {
        while (previewWidgets.size() < 9) {
            PlayerSkinWidget widget = new PlayerSkinWidget(source, 106, 150);
            widget.invisible();
            widget.resetPose();
            previewWidgets.add(widget);
        }
        if (playerSkinWidgetList == null) {
            playerSkinWidgetList = new PlayerSkinWidgetList(0, 0, previewWidgets);
            onWidgetListCreated(playerSkinWidgetList);
        }
    }
    protected void openChangeSkinOptionsScreen() {
        if (minecraft == null || !source.supportsAdvancedOptions()) return;
        Screen rootParent = rootParentScreen();
        String packId = focusedPackId();
        String skinId = focusedSkinId();
        boolean wasTu3 = ConsoleSkinsClientSettings.isTu3ChangeSkinScreen();
        Screen built = HelpAndOptionsScreen.buildChangeSkinOptionsScreen(this);
        if (built instanceof OptionsScreen optionsScreen) {
            optionsScreen.onClose = screen -> {
                if (ConsoleSkinsClientSettings.isTu3ChangeSkinScreen() != wasTu3) {
                    source.requestFocus(packId, skinId);
                    minecraft.setScreen(source.create(rootParent));
                }
            };
        }
        minecraft.setScreen(built);
        playPressSound();
    }
    boolean isEditingCustomPack(String packId) { return customPacks.isEditing(packId); }
    boolean isEditingCustomPack() { return customPacks.isEditing(); }
    void setEditingCustomPack(String packId) { customPacks.setEditing(packId); }
    boolean isReorderingCustomPack() { return customPacks.isReordering(); }
    void setReorderingCustomPack(String packId) { customPacks.setReordering(packId); }
    void queueCustomPackRefresh(String packId, String skinId) { customPacks.queueRefresh(packId, skinId); }
    void applyPendingCustomPackRefresh() { customPacks.applyPendingRefresh(); }
    protected void playFocusSound() {
        LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), 1.0f, true);
    }
    protected void playScrollSound() {
        LegacySoundUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(), 1.0f);
    }
    protected void playPressSound() {
        LegacySoundUtil.playSimpleUISound(LegacyRegistries.ACTION.get(), 1.0f);
    }
    protected void playPackSelectionSound() {
        LegacySoundUtil.playSimpleUISound(LegacyRegistries.PACK_SELECTION.get(), 1.0f);
    }
    protected void playClickSound() {
        LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }
    protected void openImportSkinScreen(String packId, Consumer<String> importedAction) {
        if (minecraft == null || packId == null || packId.isBlank()) return;
        Screen rootParent = parent != null ? parent : this;
        minecraft.setScreen(new ImportCustomSkinScreen(this, rootParent, packId, importedAction));
        playPressSound();
    }
    Screen rootParentScreen() { return parent != null ? parent : this; }
    void rebuildScreenWidgets() { rebuildWidgets(); }
    void showError(Component title, Exception ex) {
        if (minecraft != null) minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, title, Component.literal(errorText(ex))));
    }
    String errorText(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.toString() : message;
    }
    protected void selectSkin() {
        if (isReorderingCustomPack()) {
            customPacks.finishReorderingPack();
            return;
        }
        String selectedId = centerSkinId();
        if (selectedId == null) return;
        if (customPacks.isImportSkinSelection(selectedId)) {
            if (isEditingCustomPack()) {
                String packId = customPacks.focusedCustomPackId();
                openImportSkinScreen(packId, savedSkinId -> queueCustomPackRefresh(packId, savedSkinId));
                return;
            }
            openImportSkinScreen(customPacks.focusedCustomPackId(), null);
            return;
        }
        if (isEditingCustomPack()) {
            if (customPacks.isLockedSkinSelection(selectedId)) return;
            customPacks.editSelectedSkin();
            return;
        }
        source.selectSkin(packList.getFocusedPackId(), selectedId);
        playClickSound();
    }
    protected void favoriteSkin() {
        if (isReorderingCustomPack()) return;
        String skinId = centerSkinId();
        if (skinId == null) return;
        if (isEditingCustomPack()) {
            if (!customPacks.isEditableSkinSelection(skinId)) return;
            customPacks.openDeleteSelectedSkin();
            return;
        }
        if (customPacks.isImportSkinSelection(skinId)) {
            openImportSkinScreen(customPacks.focusedCustomPackId(), null);
            return;
        }
        if (!source.supportsFavorites()) return;
        int targetIndex = playerSkinWidgetList == null ? 0 : playerSkinWidgetList.index;
        source.toggleFavorite(skinId);
        SkinPack focused = packList.getFocusedPack();
        List<SkinEntry> skins = focused == null ? null : focused.skins();
        if (focused != null && SkinIdUtil.isFavouritesPack(focused.id())) {
            int size = skins == null ? 0 : skins.size();
            if (source.isFavorite(skinId) && skins != null) targetIndex = findSkinIndex(skins, skinId, size);
            if (size <= 0) targetIndex = 0;
            else targetIndex = Math.max(0, Math.min(targetIndex, size - 1));
            skinPack(targetIndex);
        }
        playClickSound();
    }
    private List<String> collectPackSkinIds(SkinPack pack) {
        List<SkinEntry> skins = pack == null ? List.of() : pack.skins();
        if (skins == null || skins.isEmpty()) return List.of();
        int limit = Math.min(skins.size(), 100);
        ArrayList<String> ids = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            SkinEntry entry = skins.get(i);
            if (entry == null) continue;
            ids.add(entry.id());
        }
        return ids;
    }
    private void applySkinIds(List<String> ids, int index) {
        ChangeSkinLayoutMetrics metrics = getLayoutMetrics();
        int contentX = tooltipBox.x;
        int contentY = panel.y + sc(metrics.tooltipTopOffset());
        int contentWidth = tooltipBox.getWidth() - sc(metrics.tooltipWidthTrim());
        int contentHeight = tooltipBox.getHeight() - sc(metrics.tooltipHeightTrim() + metrics.tooltipFooterHeight() - metrics.tooltipHeightRecover());
        int centerWidth = Math.round(106 * metrics.centerScale() * uiScale);
        int centerHeight = Math.round(150 * metrics.centerScale() * uiScale);
        int originX = contentX + contentWidth / 2 - centerWidth / 2 - sc(metrics.originPadX());
        int originY = contentY + contentHeight / 2 - centerHeight / 2 - sc(metrics.originPadY());
        int carouselOffset = Math.max(1, sc(metrics.carouselOffset()));
        int panelLeft = contentX + Math.max(2, sc(metrics.panelMarginX()));
        int panelRight = contentX + contentWidth - Math.max(2, sc(metrics.panelMarginX()));
        int minOriginX = panelLeft + carouselOffset * 4 - sc(metrics.minLeftClearance());
        int rightCardWidth = Math.round(106 * metrics.rightCardScale() * uiScale) + sc(metrics.rightCardPadding());
        int maxOriginX = panelRight - rightCardWidth - carouselOffset * 4;
        if (minOriginX <= maxOriginX) originX = Math.max(minOriginX, Math.min(originX, maxOriginX));
        else originX = panelLeft + Math.max(0, panelRight - panelLeft) / 2 - centerWidth / 2 - sc(metrics.originPadX());
        if (playerSkinWidgetList == null) {
            playerSkinWidgetList = new PlayerSkinWidgetList(originX, originY, previewWidgets);
            onWidgetListCreated(playerSkinWidgetList);
        } else playerSkinWidgetList.setOrigin(originX, originY);
        playerSkinWidgetList.setUiScale(uiScale);
        playerSkinWidgetList.setSkinIds(ids, true);
        playerSkinWidgetList.sortForIndex(index, true);
    }
    private void warmupVisibleTextures() {
        if (playerSkinWidgetList == null) return;
        for (int offset : WARMUP_OFFSETS) {
            PlayerSkinWidget widget = playerSkinWidgetList.getVisible(offset);
            String id = widget == null ? null : widget.skinId.get();
            if (id != null && !id.isBlank()) source.prewarmPreview(id);
        }
    }
    protected boolean isInCarouselBounds(double mx, double my) { return true; }

    protected abstract boolean handlePackListStepNavigation(int key);
    protected boolean handlePackListStepNavigation(int key, boolean allowWasd, boolean guardArrowsOnly, boolean applyQueuedChange, boolean requireKeyboard) {
        boolean kbm = ControlType.getActiveType().isKbm();
        boolean upArrow = key == InputConstants.KEY_UP;
        boolean downArrow = key == InputConstants.KEY_DOWN;
        boolean up = upArrow || allowWasd && kbm && key == InputConstants.KEY_W;
        boolean down = downArrow || allowWasd && kbm && key == InputConstants.KEY_S;
        if (!(up || down)) return false;
        if (requireKeyboard && !kbm) return true;
        if (packList.getPackCount() <= 1) return true;
        if (isReorderingCustomPack()) {
            customPacks.moveReorderingPack(up ? -1 : 1);
            return true;
        }
        if ((!guardArrowsOnly || upArrow || downArrow) && !packListNavigationAllowed()) return false;
        focusRelativePack(up ? -1 : 1, true);
        if (applyQueuedChange) applyQueuedPackChange();
        return true;
    }
    protected boolean packListNavigationAllowed() {
        Object focused = getFocused();
        return focused == null || focused == getRenderableVList() || focused instanceof ChangeSkinPackList.PackButton;
    }
    protected void handleSkinPackReload() {
        String packId = packList.getFocusedPackId();
        String skinId = centerSkinId();
        syncCenterPreviewState();
        packIconDims.clear();
        ClientSkinAssets.clearPreviewWarmup();
        packList.refreshPackIdsIfNeeded();
        if (packId != null) packList.focusPackId(packId, false);
        if (skinId != null && !skinId.isBlank()) source.requestFocus(null, skinId);
        if (source.supportsCustomPackOptions()) customPacks.handlePackReload();
        rebuildWidgets();
    }
    protected void onWindowRegainedFocus() {
        PlayerSkinWidget.clearCarouselClip();
        if (playerSkinWidgetList != null) { skinPack(); }
    }
    protected void focusPackListItem(Object item) {
        if (item instanceof ChangeSkinPackList.PackButton button && children().contains(button)) { setFocused(button); }
    }
    protected ChangeSkinPackList.PackButton findPackButton(int packIndex) {
        if (packIndex < 0) return null;
        for (var renderable : getRenderableVList().renderables) {
            if (renderable instanceof ChangeSkinPackList.PackButton button && button.getPackIndex() == packIndex) return button;
        }
        return null;
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
        boolean compact480 = layout.compact480();
        if (compact480) {
            groupWidth += 10f;
            groupHeight += 22f;
            footerReserve += 10f;
        }
        float sw = (w - 20f) / groupWidth;
        float sh = (h - 20f - footerReserve) / groupHeight;
        float sc = Math.min(1f, Math.min(sw, sh));
        if (compact480) {
            sc = Math.min(sc, 0.82f);
            sc *= 0.88f;
        }
        if (sc > 0.8f) sc *= 0.92f;
        sc *= 0.93f;
        if (sc <= 0f) sc = 1f;
        return sc;
    }
    protected int[] packIconDims(ResourceLocation icon) {
        if (icon == null) return new int[]{128, 128};
        return packIconDims.computeIfAbsent(icon, this::readPackIconDims);
    }
    private int[] readPackIconDims(ResourceLocation icon) {
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
        return new int[]{Math.max(1, w), Math.max(1, h)};
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
        ensurePreviewWidgets();
        syncCenterPreviewState();
        SkinPack pack = packList.getFocusedPack();
        ResourceLocation icon = pack == null ? null : pack.icon();
        if (icon != null && minecraft != null) minecraft.getTextureManager().getTexture(icon);
        List<String> ids = collectPackSkinIds(pack);
        if (ids.isEmpty()) {
            if (playerSkinWidgetList != null) {
                playerSkinWidgetList.setSkinIds(List.of(), true);
                playerSkinWidgetList.sortForIndex(0, true);
            }
        } else {
            String requestedSkinId = source.consumeRequestedFocusSkinId();
            if (requestedSkinId != null) i = Math.max(0, ids.indexOf(requestedSkinId));
            if (pack != null && SkinIdUtil.isFavouritesPack(pack.id())) {
                for (int index = 0; index < Math.min(24, ids.size()); index++) {
                    String id = ids.get(index);
        if (id != null && !id.isBlank()) source.prewarmPreview(id);
                }
            }
            applySkinIds(ids, i);
            warmupVisibleTextures();
        }
        onAfterSkinPackChanged();
    }
    protected void skinPack() {
        skinPack(playerSkinWidgetList == null ? 0 : playerSkinWidgetList.index);
    }
    protected PlayerSkinWidget getCenterWidget() { return playerSkinWidgetList == null ? null : playerSkinWidgetList.getCenter(); }
    protected String centerSkinId() {
        PlayerSkinWidget center = getCenterWidget();
        return center == null ? null : center.skinId.get();
    }
    public String focusedPackId() { return packList.getFocusedPackId(); }
    public String focusedSkinId() { return centerSkinId(); }
    private static int findSkinIndex(List<SkinEntry> skins, String skinId, int limit) {
        for (int i = 0; i < limit; i++) {
            SkinEntry entry = skins.get(i);
            if (entry != null && (skinId.equals(entry.id()) || skinId.equals(entry.sourceId()))) return i;
        }
        return 0;
    }

    protected String currentAppliedSkinId() {
        return source.currentAppliedSkinId();
    }
    protected boolean isSkinFavorite(String skinId) {
        return source.supportsFavorites() && skinId != null && source.isFavorite(skinId);
    }
    protected Component packSubtitle(SkinPack pack) {
        return source.packSubtitle(pack);
    }
    protected Component favoriteActionLabel() {
        if (isReorderingCustomPack()) return null;
        String skinId = centerSkinId();
        if (isEditingCustomPack()) {
            if (skinId == null || !customPacks.isEditableSkinSelection(skinId)) return null;
            return Component.translatable("legacy.menu.remove_custom_skin");
        }
        if (skinId != null && customPacks.isImportSkinSelection(skinId)) return Component.translatable("legacy.menu.import_skin");
        if (!source.supportsFavorites()) return null;
        return isSkinFavorite(skinId)
                ? Component.literal("Remove Favorite")
                : Component.literal("Add Favorite");
    }
    protected Component selectActionLabel() {
        if (isReorderingCustomPack()) return Component.translatable("gui.done");
        String skinId = centerSkinId();
        if (skinId != null && customPacks.isImportSkinSelection(skinId)) return Component.translatable("legacy.menu.import_skin");
        if (customPacks.isLockedSkinSelection(skinId)) return null;
        if (isEditingCustomPack()) return Component.translatable("legacy.menu.edit_custom_skin");
        return Component.literal("Select");
    }
    protected int resolveFocusedPackSkinIndex(String skinId) {
        if (skinId == null || skinId.isBlank()) return 0;
        SkinPack focused = packList.getFocusedPack();
        if (focused == null || focused.skins() == null) return 0;
        int limit = Math.min(100, focused.skins().size());
        return findSkinIndex(focused.skins(), skinId, limit);
    }
    protected void focusInitialPack() {
        String focusId = source.initialPackId(currentAppliedSkinId());
        if (focusId == null) return;
        pendingInitialPackId = focusId;
        if (!isReorderingCustomPack()) packList.promotePackId(focusId);
        packList.focusPackId(focusId, false);
    }
    protected void applyPendingInitialPackFocus() {
        if (pendingInitialPackId == null) return;
        packList.focusPackId(pendingInitialPackId, false);
        if (!pendingInitialPackId.equals(packList.getFocusedPackId())) return;
        restorePackButtonFocus();
        if (playerSkinWidgetList != null) skinPack(resolveFocusedPackSkinIndex(currentAppliedSkinId()));
        pendingInitialPackId = null;
    }
    protected boolean focusPackIndex(int index, boolean requestFocus) {
        int count = packList.getPackCount();
        if (count <= 1) return false;
        int target = Math.floorMod(index, count);
        packList.setFocusedPackIndex(target, true);
        if (requestFocus) {
            RenderableVList list = getRenderableVList();
            if (target >= 0 && target < list.renderables.size()) list.focusRenderable(list.renderables.get(target));
            else {
                ChangeSkinPackList.PackButton button = findPackButton(target);
                if (button != null) focusPackListItem(button);
            }
        }
        return true;
    }
    protected boolean focusRelativePack(int delta, boolean requestFocus) { return delta != 0 && focusPackIndex(packList.getFocusedPackIndex() + delta, requestFocus); }

    protected boolean applyQueuedPackChange() {
        if (!packList.consumeQueuedChangePack() || isReorderingCustomPack()) return false;
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
        if (carouselAnimating()) return true;
        sortCarouselBy((left ? -1 : 0) + (right ? 1 : 0));
        playScrollSound();
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
        if (playClick) playPressSound();
        return true;
    }
    protected boolean rotateCenterPreview(double dragX, double dragY) { return updateCenterPreview(false, center -> center.applyDrag(dragX, dragY)); }

    protected void addCommonControlTooltips(ControlTooltip.Renderer r, Supplier<ControlTooltip.Icon> navigateIcon, Supplier<Component> navigateLabel) {
        boolean kbm = ControlType.getActiveType().isKbm();
        r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), this::selectActionLabel);
        r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.cancel"));
        if (isEditingCustomPack() || source.supportsFavorites()) {
            r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_F) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), this::favoriteActionLabel);
        }
        if (isEditingCustomPack()) {
            r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT) : ControllerBinding.LEFT_TRIGGER.getIcon(), () -> movableCustomSkinSelected() ? Component.translatable("legacy.action.move_left") : null);
            r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT) : ControllerBinding.RIGHT_TRIGGER.getIcon(), () -> movableCustomSkinSelected() ? Component.translatable("legacy.action.move_right") : null);
        }
        if (source.supportsCustomPackOptions() && ConsoleSkinsClientSettings.isShowCustomPackOptionsTooltip()) {
            r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_C) : ControllerBinding.BACK.getIcon(), () -> LegacyComponents.CUSTOM_SKIN_PACK_OPTIONS);
        }
        r.add(navigateIcon, navigateLabel);
        if (!source.supportsAdvancedOptions() || LegacyOptions.hideAdvancedOptionsTooltip.get() || LegacyOptions.legacySettingsMenus.get()) return;
        r.add(() -> kbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> LegacyComponents.SHOW_ADVANCED_OPTIONS);
    }
    private boolean movableCustomSkinSelected() {
        if (!isEditingCustomPack()) return false;
        return customPacks.isEditableSkinSelection(centerSkinId());
    }
    protected PlayerSkinWidget pickCarouselWidget(double mx, double my) {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return null;
        if (!isInCarouselBounds(mx, my)) return null;
        PlayerSkinWidget best  = null;
        double           bestD = Double.MAX_VALUE;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets) {
            if (w == null || !w.visible) continue;
            int wx = w.getX(), wy = w.getY(), ww = w.getWidth(), wh = w.getHeight();
            if (mx < wx || mx >= wx + ww || my < wy || my >= wy + wh) continue;
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
        if (queuedCarouselSound) { queuedCarouselSound = false; playScrollSound(); }
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
            playScrollSound();
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
    protected boolean tickScreenTail() {
        if (sharedTick()) return true;
        pumpQueuedCarousel();
        pumpHoldingOuterCarousel();
        return false;
    }
    protected void restorePackButtonFocus() {
        ChangeSkinPackList.PackButton button = findPackButton(packList.getFocusedPackIndex());
        if (button != null && children().contains(button)) {
            setFocused(button);
            return;
        }
        for (var child : children()) {
            if (child instanceof ChangeSkinPackList.PackButton buttonChild) {
                setFocused(buttonChild);
                return;
            }
        }
    }
    protected void refreshSkinPackState() {
        boolean queuedPackChange = packList.consumeQueuedChangePack();
        if (firstOpen) {
            firstOpen = false;
            skinPack(resolveFocusedPackSkinIndex(currentAppliedSkinId()));
            return;
        }
        if (isReorderingCustomPack()) {
            customPacks.syncReorderingPackList();
            if (playerSkinWidgetList != null) skinPack(playerSkinWidgetList.index);
            return;
        }
        if (queuedPackChange) {
            skinPack(0);
            return;
        }
        if (playerSkinWidgetList != null) skinPack(playerSkinWidgetList.index);
    }
    @Override
    protected void init() {
        if (firstOpen && source.supportsCustomPackOptions()) {
            String requestedReorderPackId = SkinPackLoader.consumeRequestedReorderPackId();
            if (requestedReorderPackId != null) {
                setReorderingCustomPack(requestedReorderPackId);
                SkinPackLoader.requestFocusPack(requestedReorderPackId);
            }
            String requestedEditPackId = SkinPackLoader.consumeRequestedEditPackId();
            if (requestedReorderPackId == null && requestedEditPackId != null) {
                setEditingCustomPack(requestedEditPackId);
                SkinPackLoader.requestFocusPack(requestedEditPackId);
            }
        }
        applyPendingCustomPackRefresh();
        if (firstOpen) focusInitialPack();
        super.init();
        ensurePreviewWidgets();
        for (PlayerSkinWidget widget : previewWidgets) addRenderableWidget(widget);
        restorePackButtonFocus();
        refreshSkinPackState();
    }
    @Override
    public void repositionElements() {
        syncCenterPreviewState();
        super.repositionElements();
        ensurePreviewWidgets();
        restorePackButtonFocus();
        refreshSkinPackState();
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();
        if (key == InputConstants.KEY_RETURN) {
            if (!enterHeld) enterHeld = true;
            return true;
        }
        if (key == InputConstants.KEY_F) { favoriteSkin(); return true; }

        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
            if (!shiftHeld) {
                shiftHeld = true;
                updateCenterPreview(false, PlayerSkinWidget::togglePose);
            }
            return true;
        }
        if (key == InputConstants.KEY_P) {
            if (!pHeld) {
                pHeld = true;
                updateCenterPreview(false, PlayerSkinWidget::togglePunch);
            }
            return true;
        }
        if (isEditingCustomPack()) {
            if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_PAGEUP) {
                customPacks.moveSelectedSkin(-1);
                return true;
            }
            if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_PAGEDOWN) {
                customPacks.moveSelectedSkin(1);
                return true;
            }
            if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
                customPacks.openDeleteSelectedSkin();
                return true;
            }
        }
        if (key == InputConstants.KEY_C && source.supportsCustomPackOptions()) { customPacks.openOptionsScreen(); return true; }
        if (key == InputConstants.KEY_O && source.supportsAdvancedOptions()) { openChangeSkinOptionsScreen(); return true; }

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
            if (enterHeld) { enterHeld = false; selectSkin(); }
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
            updateCenterPreview(false, PlayerSkinWidget::recenterView);
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
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (outerCarouselHold.active()) stopHoldingOuterCarousel();
            if (!draggingCenterDoll) return super.mouseReleased(event);
            draggingCenterDoll = false;
            if (!centerDragMoved) selectSkin();
            centerDragMoved = false;
            return true;
        }
        return super.mouseReleased(event);
    }
    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (g == 0) return false;
        int scrollDir = g > 0 ? -1 : 1;
        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList == getRenderableVList()) {
            if (!packListNavigationAllowed() || packList.getPackCount() <= 1) return true;
            if (isReorderingCustomPack()) {
                customPacks.moveReorderingPack(scrollDir, false);
                return true;
            }
            focusRelativePack(scrollDir, true);
            applyQueuedPackChange();
            return true;
        }
        if (vList != null) { vList.mouseScrolled(g); return true; }

        if (playerSkinWidgetList == null) return false;
        if (!insideScrollRegion(d, e) && pickCarouselWidget(d, e) == null) return false;
        if (draggingCenterDoll) return true;
        if (carouselAnimating()) return true;
        startQueuedCarousel(scrollDir);
        return true;
    }
    private static boolean buttonOnce(BindingState state, ControllerBinding binding) { return state != null && state.is(binding) && state.onceClick(true); }

    protected boolean handleSharedBindingState(BindingState state) {
        if (buttonOnce(state, ControllerBinding.LEFT_BUTTON)) { favoriteSkin(); return true; }
        boolean kbm = ControlType.getActiveType().isKbm();
        if (!kbm && source.supportsAdvancedOptions() && buttonOnce(state, ControllerBinding.UP_BUTTON)) {
            openChangeSkinOptionsScreen();
            return true;
        }
        if (!kbm && buttonOnce(state, ControllerBinding.RIGHT_STICK_BUTTON)) {
            updateCenterPreview(false, PlayerSkinWidget::recenterView);
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
                            updateCenterPreview(false, preview -> {
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
                            updateCenterPreview(false, preview -> {
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
    public void simulateKeyAction(ControllerManager manager, BindingState state) {
        if (manager.isCursorDisabled) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
        }
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUTTON), InputConstants.KEY_ESCAPE, state, true);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUTTON), InputConstants.KEY_X, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.UP_BUTTON), InputConstants.KEY_O, state);
        if (manager.isCursorDisabled) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_TRIGGER), InputConstants.KEY_PAGEUP, state);
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_PAGEDOWN, state);
        } else {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
        }
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUMPER), InputConstants.KEY_RBRACKET, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUMPER), InputConstants.KEY_LBRACKET, state);
        if (source.supportsCustomPackOptions()) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.BACK), InputConstants.KEY_C, state, true);
        }
        manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE), InputConstants.KEY_F2, state);
    }

    @Override
    public void removed() {
        stickUpHeld = stickDownHeld = leftStickUpHeld = leftStickDownHeld = false;
        shiftHeld = pHeld = enterHeld = false;
        stopHoldingOuterCarousel();
        draggingCenterDoll = centerDragMoved = false;
        PlayerSkinWidget.clearCarouselClip();
        ClientSkinAssets.clearPreviewWarmup();
        super.removed();
    }
    protected static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    protected static void blitSprite(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) { FactoryGuiGraphics.of(g).blitSprite(id, x, y, w, h); }

    protected void drawBigCentered(GuiGraphics g, Component text, int centerX, int y, int color) { drawScaledCentered(g, text, centerX, y, color, bigTextScale()); }

    protected void drawSmallCentered(GuiGraphics g, Component text, int centerX, int y, int color) { drawScaledCentered(g, text, centerX, y, color, smallTextScale()); }

    protected void drawScaledCentered(GuiGraphics g, Component text, int centerX, int y, int color, float scale) {
        drawScaledCentered(g, text, centerX, y, color, scale, false);
    }

    protected void drawScaledCentered(GuiGraphics g, Component text, int centerX, int y, int color, float scale, boolean shadow) {
        int yAdj = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        int textWidth = minecraft.font.width(text);
        int textX = -textWidth / 2;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawString(minecraft.font, text, textX, 0, color, shadow);
        pose.popMatrix();
    }
    protected static final class HoldRepeat {
        private int dir;
        private long startAt, nextAt;
        boolean active() { return dir != 0; }
        int dir() { return dir; }
        void start(int dir) { this.dir = dir < 0 ? -1 : 1; long now = net.minecraft.Util.getMillis(); startAt = now; nextAt = now + 220L; }
        void stop() { dir = 0; startAt = 0L; nextAt = 0L; }
        boolean ready() { return dir != 0 && net.minecraft.Util.getMillis() >= nextAt; }
        void step() { long now = net.minecraft.Util.getMillis(); nextAt = now + (now - startAt < 700L ? 120L : 80L); }
    }
}
