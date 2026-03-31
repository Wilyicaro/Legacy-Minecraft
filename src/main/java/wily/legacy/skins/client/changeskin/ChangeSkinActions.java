package wily.legacy.Skins.client.changeskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import wily.legacy.Skins.SkinsClientBootstrap;
import wily.legacy.Skins.client.preview.PlayerSkinWidget;
import wily.legacy.Skins.client.preview.PlayerSkinWidgetList;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.util.SkinPreviewWarmup;
import wily.legacy.Skins.skin.FavoritesStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSyncClient;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.LegacyScreen;
import wily.legacy.client.screen.OptionsScreen;
import wily.legacy.client.screen.Panel;

import java.util.ArrayList;
import java.util.List;

public final class ChangeSkinActions {
    public record ChangeSkinLayoutMetrics(
            int tooltipTopOffset, int tooltipWidthTrim, int tooltipHeightTrim, int tooltipFooterHeight, int tooltipHeightRecover,
            int carouselClipInset, int carouselClipBottomTrim, float centerScale, float rightCardScale, int originPadX,
            int originPadY, int panelMarginX, int carouselOffset, int minLeftClearance, int rightCardPadding
    ) {
        public static final ChangeSkinLayoutMetrics DEFAULT = new ChangeSkinLayoutMetrics(
                45, 23, 80, 50, 40, 2, 24, 0.935f, 0.44f, 8, 20, 6, 80, 88, 6
        );
        public static final ChangeSkinLayoutMetrics SD_480 = new ChangeSkinLayoutMetrics(
                34, 18, 64, 40, 30, 2, 18, 0.76f, 0.36f, 6, 12, 5, 58, 62, 4
        );
    }
    public interface Host {
        PlayerSkinWidgetList getPlayerSkinWidgetList();

        void setPlayerSkinWidgetList(PlayerSkinWidgetList list);

        void addSkinWidget(PlayerSkinWidget widget);

        Panel getTooltipBox();

        Panel getPanel();

        float getUiScale();

        ChangeSkinLayoutMetrics getLayoutMetrics();

        Screen getScreen();
    }

    private static final int MAX_SKINS_PER_PACK = 100, WIDGET_POOL_SIZE = 9, BASE_W = 106, BASE_H = 150, FAV_PREWARM_COUNT = 24;
    private final Minecraft minecraft;
    private final ChangeSkinPackList packList;
    private final Host host;
    private final List<PlayerSkinWidget> widgetPool = new ArrayList<>(WIDGET_POOL_SIZE);

    public ChangeSkinActions(Minecraft minecraft, ChangeSkinPackList packList, Host host) {
        this.minecraft = minecraft;
        this.packList = packList;
        this.host = host;
    }

    public void skinPack(int index) {
        SkinPack pack = packList.getFocusedPack();
        warmupPackIcon(pack);

        List<String> ids = collectPackSkinIds(pack);
        if (ids.isEmpty()) {
            clearCurrentList();
            return;
        }

        if (isFavouritesPack(pack)) { warmIds(ids, FAV_PREWARM_COUNT); }

        applySkinIds(ids, index);
        warmupVisibleTextures(host.getPlayerSkinWidgetList());
    }

    public void skinPack() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null) {
            skinPack(0);
            return;
        }
        skinPack(list.index);
    }

    public void selectSkin() {
        String selectedId = getSelectedSkinId();
        if (selectedId == null) return;

        String skinId = SkinIdUtil.isAutoSelect(selectedId) ? "" : selectedId;
        rememberLastUsedCustomPack(selectedId, skinId);
        SkinSyncClient.requestSetSkin(minecraft, skinId);
        playClick();
    }

    public void favorite() {
        String skinId = getSelectedSkinId();
        if (skinId == null) return;

        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        int previousIndex = list != null ? list.index : 0;

        FavoritesStore.toggle(skinId);
        SkinPackLoader.rebuildFavouritesPack();

        SkinPack focused = packList.getFocusedPack();
        if (isFavouritesPack(focused)) {
            int targetIndex = previousIndex;
            List<SkinEntry> skins = focused.skins();
            int size = skins == null ? 0 : skins.size();
            if (FavoritesStore.isFavorite(skinId) && skins != null) {
                for (int i = 0; i < size; i++) {
                    SkinEntry entry = skins.get(i);
                    if (entry != null && skinId.equals(entry.id())) {
                        targetIndex = i;
                        break;
                    }
                }
            }
            if (size <= 0) targetIndex = 0;
            else if (targetIndex >= size) targetIndex = size - 1;
            else if (targetIndex < 0) targetIndex = 0;

            skinPack(targetIndex);
        }

        playClick();
    }

    private boolean isFavouritesPack(SkinPack pack) { return pack != null && SkinIdUtil.isFavouritesPack(pack.id()); }

    public void playClick() {
        if (minecraft == null || minecraft.getSoundManager() == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    public void openLegacyChangeSkinScreen() {
        if (minecraft == null) return;
        Screen skinScreen = host.getScreen();
        Screen rootParent = skinScreen instanceof LegacyScreen legacyScreen && legacyScreen.parent != null
                ? legacyScreen.parent
                : skinScreen;
        boolean wasTu3 = ConsoleSkinsClientSettings.isTu3ChangeSkinScreen();
        Screen built = HelpAndOptionsScreen.buildChangeSkinOptionsScreen(skinScreen);
        if (built instanceof OptionsScreen optionsScreen) {
            optionsScreen.onClose = screen -> {
                boolean wantTu3 = ConsoleSkinsClientSettings.isTu3ChangeSkinScreen();
                if (wantTu3 != wasTu3) { SkinsClientBootstrap.requestOpenChangeSkinScreen(minecraft, rootParent); }
            };
        }
        minecraft.setScreen(built);
        playClick();
    }

    private void warmupPackIcon(SkinPack pack) {
        if (pack == null) return;
        ResourceLocation icon = pack.icon();
        if (icon == null || minecraft == null) return;
        minecraft.getTextureManager().getTexture(icon);
    }

    private void clearCurrentList() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null) return;
        list.setSkinIds(List.of(), true);
        list.sortForIndex(0, true);
    }

    private List<String> collectPackSkinIds(SkinPack pack) {
        List<SkinEntry> skins = pack == null ? List.of() : pack.skins();
        if (skins == null || skins.isEmpty()) return List.of();

        ArrayList<String> ids = new ArrayList<>(Math.min(skins.size(), MAX_SKINS_PER_PACK));
        for (SkinEntry entry : skins) {
            if (entry == null) continue;
            ids.add(entry.id());
            if (ids.size() >= MAX_SKINS_PER_PACK) break;
        }
        return ids;
    }

    private void ensureWidgetPool() {
        while (widgetPool.size() < WIDGET_POOL_SIZE) {
            PlayerSkinWidget widget = new PlayerSkinWidget(BASE_W, BASE_H);
            widget.invisible();
            widget.resetPose();
            widgetPool.add(widget);
        }

        Screen screen = host.getScreen();
        for (PlayerSkinWidget widget : widgetPool) {
            if (!screen.children().contains(widget)) { host.addSkinWidget(widget); }
        }
    }

    private void applySkinIds(List<String> ids, int index) {
        ensureWidgetPool();

        float uiScale = host.getUiScale();
        if (uiScale <= 0f) uiScale = 1f;
        ChangeSkinLayoutMetrics layoutMetrics = host.getLayoutMetrics();
        if (layoutMetrics == null) layoutMetrics = ChangeSkinLayoutMetrics.DEFAULT;

        Panel tooltipBox = host.getTooltipBox();
        Panel panel = host.getPanel();
        int topOffset = Math.round(layoutMetrics.tooltipTopOffset() * uiScale);
        int contentX = tooltipBox.x;
        int contentY = panel.y + topOffset;
        int contentWidth = tooltipBox.getWidth() - Math.round(layoutMetrics.tooltipWidthTrim() * uiScale);
        int contentHeight = tooltipBox.getHeight()
                - Math.round(layoutMetrics.tooltipHeightTrim() * uiScale)
                - Math.round(layoutMetrics.tooltipFooterHeight() * uiScale)
                + Math.round(layoutMetrics.tooltipHeightRecover() * uiScale);

        float centerScale = layoutMetrics.centerScale() * uiScale;
        int centerWidth = Math.round(BASE_W * centerScale);
        int centerHeight = Math.round(BASE_H * centerScale);
        int originX = contentX + contentWidth / 2 - centerWidth / 2 - Math.round(layoutMetrics.originPadX() * uiScale);
        int originY = contentY + contentHeight / 2 - centerHeight / 2 - Math.round(layoutMetrics.originPadY() * uiScale);

        int carouselOffset = Math.max(1, Math.round(layoutMetrics.carouselOffset() * uiScale));
        int marginX = Math.max(2, Math.round(layoutMetrics.panelMarginX() * uiScale));
        int panelLeft = contentX + marginX;
        int panelRight = contentX + contentWidth - marginX;
        int minOriginX = panelLeft + carouselOffset * 4 - Math.round(layoutMetrics.minLeftClearance() * uiScale);
        int rightCardWidth = Math.round(BASE_W * layoutMetrics.rightCardScale() * uiScale) + Math.round(layoutMetrics.rightCardPadding() * uiScale);
        int maxOriginX = panelRight - rightCardWidth - carouselOffset * 4;
        if (minOriginX <= maxOriginX) {
            originX = Math.max(minOriginX, Math.min(originX, maxOriginX));
        } else { originX = panelLeft + Math.max(0, panelRight - panelLeft) / 2 - centerWidth / 2 - Math.round(layoutMetrics.originPadX() * uiScale); }

        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null) {
            list = new PlayerSkinWidgetList(originX, originY, widgetPool);
            host.setPlayerSkinWidgetList(list);
        } else { list.setOrigin(originX, originY); }

        list.setUiScale(uiScale);
        list.setSkinIds(ids, true);
        list.sortForIndex(index, true);
    }

    private void warmupVisibleTextures(PlayerSkinWidgetList list) {
        if (list == null) return;
        for (int offset : new int[]{0, -1, 1, -2, 2, -3, 3}) { touchTextureForId(list.getVisible(offset)); }
    }

    private void warmIds(List<String> ids, int limit) {
        for (int i = 0; i < Math.min(limit, ids.size()); i++) { warmId(ids.get(i)); }
    }

    private void warmId(String id) {
        if (id == null || id.isBlank()) return;
        SkinPreviewWarmup.enqueue(id);
    }

    private void touchTextureForId(PlayerSkinWidget widget) {
        if (widget == null) return;
        warmId(widget.skinId.get());
    }

    private String getSelectedSkinId() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        PlayerSkinWidget center = list == null ? null : list.getCenter();
        return center == null ? null : center.skinId.get();
    }

    private void rememberLastUsedCustomPack(String selectedId, String skinId) {
        if (skinId == null || skinId.isBlank()) {
            SkinPackLoader.setLastUsedCustomPackId(null);
            return;
        }

        SkinPack focused = packList.getFocusedPack();
        String packId = focused != null ? focused.id() : null;
        if (packId != null && SkinIdUtil.isFavouritesPack(packId)) { packId = SkinPackLoader.getSourcePackId(selectedId); }
        SkinPackLoader.setLastUsedCustomPackId(packId);
    }
}
