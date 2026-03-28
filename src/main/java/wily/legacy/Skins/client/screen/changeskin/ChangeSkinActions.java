package wily.legacy.Skins.client.screen.changeskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import wily.legacy.Skins.SkinsClientBootstrap;
import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.client.compat.minecraft.TextureCompat;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.util.SkinPreviewWarmup;
import wily.legacy.Skins.skin.FavoritesStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIds;
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

    public interface Host {
        PlayerSkinWidgetList getPlayerSkinWidgetList();

        void setPlayerSkinWidgetList(PlayerSkinWidgetList list);

        PlayerSkinWidget addSkinWidget(PlayerSkinWidget widget);

        Panel getTooltipBox();

        Panel getPanel();

        float getUiScale();

        ChangeSkinLayoutMetrics getLayoutMetrics();

        Screen getScreen();
    }

    private static final int MAX_SKINS_PER_PACK = 100;
    private static final int WIDGET_POOL_SIZE = 9;

    private static final int BASE_W = 106;
    private static final int BASE_H = 150;

    private static final int FAV_REQUIRED_READY = 14;
    private static final int FAV_PREWARM_COUNT = 24;
    private static final int FAV_WARM_PER_TICK = 14;
    private static final long FAV_MAX_WAIT_MS = 20L;

    private final Minecraft minecraft;
    private final ChangeSkinPackList packList;
    private final Host host;

    private final List<PlayerSkinWidget> widgetPool = new ArrayList<>(WIDGET_POOL_SIZE);
    private PendingSwap pending;

    private static final class PendingSwap {
        final List<String> ids;
        final int index;
        final long startMs;
        int warmCursor;

        PendingSwap(List<String> ids, int index) {
            this.ids = ids;
            this.index = index;
            this.startMs = System.currentTimeMillis();
        }
    }

    public ChangeSkinActions(Minecraft minecraft, ChangeSkinPackList packList, Host host) {
        this.minecraft = minecraft;
        this.packList = packList;
        this.host = host;
    }

    public SkinPack getFocusedPack() {
        return packList.getFocusedPack();
    }

    public ResourceLocation getFocusedPackIcon() {
        return packList.getFocusedPackIcon();
    }

    public boolean isPendingSwap() {
        return pending != null;
    }

    public void tick() {
        if (pending == null) return;

        SkinPack focused = getFocusedPack();
        if (focused == null || !SkinIdUtil.isFavouritesPack(focused.id())) {
            pending = null;
            return;
        }

        PlayerSkinWidgetList currentList = host.getPlayerSkinWidgetList();
        if (currentList == null) {
            applySkinIds(pending.ids, pending.index, true);
            pending = null;
            return;
        }

        int required = Math.min(FAV_REQUIRED_READY, pending.ids.size());
        for (int i = 0; i < FAV_WARM_PER_TICK && pending.warmCursor < required; i++, pending.warmCursor++) {
            warmOne(pending.ids.get(pending.warmCursor));
        }

        boolean ready = areRequiredReady(pending.ids, required);
        long waited = System.currentTimeMillis() - pending.startMs;
        if (ready || waited >= FAV_MAX_WAIT_MS) {
            applySkinIds(pending.ids, pending.index, true);
            pending = null;
        }
    }

    public void skinPack(int index) {
        SkinPack pack = getFocusedPack();
        warmupPackIcon(pack);

        List<String> ids = collectPackSkinIds(pack);
        if (ids.isEmpty()) {
            clearCurrentList();
            pending = null;
            return;
        }

        if (pack != null && SkinIdUtil.isFavouritesPack(pack.id())) {
            pending = new PendingSwap(ids, index);

            int prewarmCount = Math.min(FAV_PREWARM_COUNT, Math.min(FAV_REQUIRED_READY, ids.size()));
            for (int i = 0; i < prewarmCount; i++) {
                warmOne(ids.get(i));
            }
            pending.warmCursor = prewarmCount;
            return;
        }

        pending = null;
        applySkinIds(ids, index, true);
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

    public void warmupFavouritesPack() {
        try {
            SkinPackLoader.ensureLoaded();
            SkinPackLoader.rebuildFavouritesPack();
        } catch (Throwable ignored) {
        }

        SkinPack favourites;
        try {
            favourites = SkinPackLoader.getPacks().get(SkinIds.PACK_FAVOURITES);
        } catch (Throwable throwable) {
            favourites = null;
        }
        if (favourites == null) return;

        warmupPackIcon(favourites);

        int touched = 0;
        for (SkinEntry entry : favourites.skins()) {
            if (entry == null) continue;
            SkinPreviewWarmup.enqueue(entry.id(), entry);
            if (++touched >= MAX_SKINS_PER_PACK) break;
        }
    }

    public void selectSkin() {
        String selectedId = getSelectedSkinId();
        if (selectedId == null) return;

        String skinId = SkinIdUtil.isAutoSelect(selectedId) ? "" : selectedId;
        rememberLastUsedCustomPack(selectedId, skinId);

        if (ExternalSkinProviders.isExternalSkinId(skinId)) {
            if (!ExternalSkinProviders.applySelectedSkin(skinId)) return;
        } else {
            ExternalSkinProviders.clearAllSelectedSkins();
        }

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

        SkinPack focused = getFocusedPack();
        if (focused != null && SkinIdUtil.isFavouritesPack(focused.id())) {
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

    public void playClick() {
        try {
            if (minecraft.getSoundManager() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } catch (Throwable ignored) {
        }
    }

    public void openLegacyChangeSkinScreen() {
        try {
            Screen skinScreen = host.getScreen();
            Screen rootParent = skinScreen instanceof LegacyScreen legacyScreen && legacyScreen.parent != null
                    ? legacyScreen.parent
                    : skinScreen;
            boolean wasTu3 = ConsoleSkinsClientSettings.isTu3ChangeSkinScreen();
            Screen built = HelpAndOptionsScreen.buildChangeSkinOptionsScreen(skinScreen);
            if (built instanceof OptionsScreen optionsScreen) {
                optionsScreen.onClose = screen -> {
                    try {
                        boolean wantTu3 = ConsoleSkinsClientSettings.isTu3ChangeSkinScreen();
                        if (wantTu3 != wasTu3) {
                            SkinsClientBootstrap.requestOpenChangeSkinScreen(minecraft, rootParent);
                        }
                    } catch (Throwable ignored) {
                    }
                };
            }
            minecraft.setScreen(built);
            playClick();
        } catch (Throwable throwable) {
            try {
                minecraft.setScreen(HelpAndOptionsScreen.buildChangeSkinOptionsScreen(host.getScreen()));
                playClick();
            } catch (Throwable ignored) {
            }
        }
    }

    private void warmupPackIcon(SkinPack pack) {
        if (pack == null) return;
        ResourceLocation icon = pack.icon();
        if (icon == null) return;
        try {
            minecraft.getTextureManager().getTexture(icon);
        } catch (Throwable ignored) {
        }
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
            try {
                if (!screen.children().contains(widget)) {
                    host.addSkinWidget(widget);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void applySkinIds(List<String> ids, int index, boolean animate) {
        ensureWidgetPool();

        float uiScale = host.getUiScale();
        if (uiScale <= 0f) uiScale = 1f;
        ChangeSkinLayoutMetrics layoutMetrics = host.getLayoutMetrics();
        if (layoutMetrics == null) layoutMetrics = ChangeSkinLayoutMetrics.DEFAULT;

        ChangeSkinWidgetLayout layout = ChangeSkinWidgetLayout.resolve(
                host.getTooltipBox(),
                host.getPanel(),
                uiScale,
                BASE_W,
                BASE_H,
                layoutMetrics
        );

        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null) {
            list = PlayerSkinWidgetList.of(layout.originX(), layout.originY(), widgetPool);
            host.setPlayerSkinWidgetList(list);
        } else {
            list.setOrigin(layout.originX(), layout.originY());
        }

        list.setUiScale(uiScale);
        list.setSkinIds(ids, animate);
        list.sortForIndex(index, animate);
    }

    private void warmupVisibleTextures(PlayerSkinWidgetList list) {
        if (list == null) return;
        touchTextureForId(list.element3);
        touchTextureForId(list.element2);
        touchTextureForId(list.element4);
        touchTextureForId(list.element1);
        touchTextureForId(list.element5);
        touchTextureForId(list.element0);
        touchTextureForId(list.element6);
    }

    private void warmOne(String id) {
        if (id == null || id.isBlank()) return;
        SkinEntry entry = SkinPackLoader.getSkin(id);
        if (entry == null) return;
        SkinPreviewWarmup.enqueue(id, entry);
    }

    private void touchTextureForId(PlayerSkinWidget widget) {
        if (widget == null) return;
        String id = widget.skinId.get();
        if (id == null || id.isBlank()) return;
        SkinEntry entry = SkinPackLoader.getSkin(id);
        if (entry == null || entry.texture() == null) return;
        SkinPreviewWarmup.enqueue(id, entry);
    }

    private boolean areRequiredReady(List<String> ids, int required) {
        if (required <= 0) return true;

        for (int i = 0; i < required; i++) {
            String id = ids.get(i);
            if (id == null || id.isBlank()) return false;

            SkinEntry entry = SkinPackLoader.getSkin(id);
            if (entry == null) return false;

            ResourceLocation texture = entry.texture();
            if (texture == null) {
                if (ExternalSkinProviders.canRenderPreview(id)) continue;
                return false;
            }
            if (TextureCompat.isMissingTexture(texture)) return false;
        }
        return true;
    }

    private String getSelectedSkinId() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null || list.element3 == null) return null;
        return list.element3.skinId.get();
    }

    private void rememberLastUsedCustomPack(String selectedId, String skinId) {
        try {
            if (skinId == null || skinId.isBlank()) {
                SkinPackLoader.setLastUsedCustomPackId(null);
                return;
            }

            SkinPack focused = getFocusedPack();
            String packId = focused != null ? focused.id() : null;
            if (packId != null && SkinIdUtil.isFavouritesPack(packId)) {
                packId = SkinPackLoader.getSourcePackId(selectedId);
            }
            SkinPackLoader.setLastUsedCustomPackId(packId);
        } catch (Throwable ignored) {
        }
    }

}
