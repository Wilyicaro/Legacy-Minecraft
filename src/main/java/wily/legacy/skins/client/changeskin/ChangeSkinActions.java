package wily.legacy.Skins.client.changeskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import wily.legacy.Skins.client.preview.PlayerSkinWidget;
import wily.legacy.Skins.client.preview.PlayerSkinWidgetList;
import wily.legacy.Skins.client.util.SkinPreviewWarmup;
import wily.legacy.Skins.skin.SkinDataStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSyncClient;
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

        List<PlayerSkinWidget> getSkinWidgetPool();

        Panel getTooltipBox();

        Panel getPanel();

        float getUiScale();

        ChangeSkinLayoutMetrics getLayoutMetrics();
    }

    private static final int MAX_SKINS_PER_PACK = 100, BASE_W = 106, BASE_H = 150, FAV_PREWARM_COUNT = 24;
    private final Minecraft minecraft;
    private final ChangeSkinPackList packList;
    private final Host host;

    public ChangeSkinActions(Minecraft minecraft, ChangeSkinPackList packList, Host host) {
        this.minecraft = minecraft;
        this.packList = packList;
        this.host = host;
    }

    public void skinPack(int index) {
        SkinPack pack = packList.getFocusedPack();
        ResourceLocation icon = pack == null ? null : pack.icon();
        if (icon != null && minecraft != null) minecraft.getTextureManager().getTexture(icon);

        List<String> ids = collectPackSkinIds(pack);
        if (ids.isEmpty()) {
            PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
            if (list != null) {
                list.setSkinIds(List.of(), true);
                list.sortForIndex(0, true);
            }
            return;
        }

        if (pack != null && SkinIdUtil.isFavouritesPack(pack.id())) {
            for (int i = 0; i < Math.min(FAV_PREWARM_COUNT, ids.size()); i++) {
                String id = ids.get(i);
                if (id != null && !id.isBlank()) SkinPreviewWarmup.enqueue(id);
            }
        }

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
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        PlayerSkinWidget center = list == null ? null : list.getCenter();
        String selectedId = center == null ? null : center.skinId.get();
        if (selectedId == null) return;

        String skinId = SkinIdUtil.isAutoSelect(selectedId) ? "" : selectedId;
        rememberLastUsedCustomPack(selectedId, skinId);
        SkinSyncClient.requestSetSkin(minecraft, skinId);
        playClick();
    }

    public void favorite() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        PlayerSkinWidget center = list == null ? null : list.getCenter();
        String skinId = center == null ? null : center.skinId.get();
        if (skinId == null) return;

        int previousIndex = list != null ? list.index : 0;

        SkinDataStore.toggleFavorite(skinId);
        SkinPackLoader.rebuildFavouritesPack();

        SkinPack focused = packList.getFocusedPack();
        if (focused != null && SkinIdUtil.isFavouritesPack(focused.id())) {
            int targetIndex = previousIndex;
            List<SkinEntry> skins = focused.skins();
            int size = skins == null ? 0 : skins.size();
            if (SkinDataStore.isFavorite(skinId) && skins != null) {
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

    public void playClick() {
        if (minecraft == null || minecraft.getSoundManager() == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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

    private void applySkinIds(List<String> ids, int index) {
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
            list = new PlayerSkinWidgetList(originX, originY, host.getSkinWidgetPool());
            host.setPlayerSkinWidgetList(list);
        } else { list.setOrigin(originX, originY); }

        list.setUiScale(uiScale);
        list.setSkinIds(ids, true);
        list.sortForIndex(index, true);
    }

    private void warmupVisibleTextures(PlayerSkinWidgetList list) {
        if (list == null) return;
        for (int offset : new int[]{0, -1, 1, -2, 2, -3, 3}) {
            PlayerSkinWidget widget = list.getVisible(offset);
            String id = widget == null ? null : widget.skinId.get();
            if (id != null && !id.isBlank()) SkinPreviewWarmup.enqueue(id);
        }
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
