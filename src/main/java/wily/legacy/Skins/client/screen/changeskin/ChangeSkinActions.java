package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.FavoritesStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSync;
import wily.legacy.Skins.skin.SkinSyncClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.Skins.client.compat.minecraft.TextureCompat;

public final class ChangeSkinActions {

    public interface Host {
        PlayerSkinWidgetList getPlayerSkinWidgetList();

        void setPlayerSkinWidgetList(PlayerSkinWidgetList list);

        PlayerSkinWidget addSkinWidget(PlayerSkinWidget widget);

        Panel getTooltipBox();

        Panel getPanel();

        float getUiScale();

        Screen getScreen();
    }

    private static final int MAX_SKINS_PER_PACK = 100;
    private static final int WIDGET_POOL_SIZE = 9;

    private static final int BASE_W = 106;
    private static final int BASE_H = 150;


    private static final int FAV_REQUIRED_READY = 14;

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

    private void warmupPackIcon(SkinPack pack) {
        if (pack == null) return;
        ResourceLocation icon = pack.icon();
        if (icon == null) return;
        try {
            minecraft.getTextureManager().getTexture(icon);
        } catch (Throwable ignored) {
        }
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
            String id = pending.ids.get(pending.warmCursor);
            warmOne(id);
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

        List<SkinEntry> base = pack == null ? List.of() : pack.skins();
        if (base == null || base.isEmpty()) {
            PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
            if (list != null) {
                list.setSkinIds(List.of(), true);
                list.sortForIndex(0, true);
            }
            return;
        }

        List<String> ids = new ArrayList<>(Math.min(base.size(), MAX_SKINS_PER_PACK));
        for (SkinEntry e : base) {
            if (e == null) continue;
            ids.add(e.id());
            if (ids.size() >= MAX_SKINS_PER_PACK) break;
        }


        if (pack != null && SkinIdUtil.isFavouritesPack(pack.id())) {

            pending = new PendingSwap(ids, index);


            int pre = Math.min(24, Math.min(FAV_REQUIRED_READY, ids.size()));
            for (int i = 0; i < pre; i++) warmOne(ids.get(i));

            pending.warmCursor = pre;


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

    private void ensureWidgetPool() {
        while (widgetPool.size() < WIDGET_POOL_SIZE) {
            PlayerSkinWidget w = new PlayerSkinWidget(BASE_W, BASE_H);
            w.invisible();
            w.resetPose();
            widgetPool.add(w);
        }

        Screen screen = host.getScreen();
        for (PlayerSkinWidget w : widgetPool) {
            try {
                if (!screen.children().contains(w)) {
                    host.addSkinWidget(w);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void applySkinIds(List<String> ids, int index, boolean animate) {
        ensureWidgetPool();

        Panel tooltipBox = host.getTooltipBox();
        Panel panel = host.getPanel();

        float uiScale = host.getUiScale();
        if (uiScale <= 0f) uiScale = 1f;

        int off45 = Math.round(45 * uiScale);
        int off23 = Math.round(23 * uiScale);
        int off80 = Math.round(80 * uiScale);
        int off50 = Math.round(50 * uiScale);
        int off40 = Math.round(40 * uiScale);

        int x = tooltipBox.x;
        int y = panel.y + off45;
        int width = tooltipBox.getWidth() - off23;
        int height = tooltipBox.getHeight() - off80 - off50 + off40;

        float centerScale = 0.935f * uiScale;
        int centerW = Math.round(BASE_W * centerScale);
        int centerH = Math.round(BASE_H * centerScale);
        int padX = Math.round(8 * uiScale);
        int padY = Math.round(20 * uiScale);

        int startX = x + width / 2 - centerW / 2 - padX;
        int startY = y + height / 2 - centerH / 2 - padY;


        final int OFFSET = Math.max(1, Math.round(80 * uiScale));
        int marginX = Math.max(2, Math.round(6 * uiScale));

        int panelLeft = x + marginX;
        int panelRight = x + width - marginX;

        int minStartX = panelLeft + (OFFSET * 4) - Math.round(88 * uiScale);

        int rightCardW = Math.round((BASE_W * 0.44f) * uiScale) + Math.round(6 * uiScale);
        int maxStartX = panelRight - rightCardW - (OFFSET * 4);

        if (minStartX <= maxStartX) {
            startX = Math.max(minStartX, Math.min(startX, maxStartX));
        } else {

            int available = Math.max(0, panelRight - panelLeft);
            startX = panelLeft + (available / 2) - (centerW / 2) - padX;
        }

        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null) {
            list = PlayerSkinWidgetList.of(startX, startY, widgetPool);
            host.setPlayerSkinWidgetList(list);
        } else {
            list.setOrigin(startX, startY);
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

    public void warmupFavouritesPack() {
        try {
            SkinPackLoader.ensureLoaded();
            SkinPackLoader.rebuildFavouritesPack();
        } catch (Throwable ignored) {
        }

        SkinPack fav;
        try {
            fav = SkinPackLoader.getPacks().get(SkinIds.PACK_FAVOURITES);
        } catch (Throwable t) {
            fav = null;
        }
        if (fav == null) return;

        warmupPackIcon(fav);

        int touched = 0;
        for (SkinEntry e : fav.skins()) {
            if (e == null) continue;
            ResourceLocation tex = e.texture();
            if (tex == null) continue;
            try {
                minecraft.getTextureManager().getTexture(tex);
            } catch (Throwable ignored) {
            }
            if (++touched >= MAX_SKINS_PER_PACK) break;
        }
    }

    private void warmOne(String id) {
        if (id == null || id.isBlank()) return;
        SkinEntry entry = SkinPackLoader.getSkin(id);
        if (entry == null) return;

        ResourceLocation tex = entry.texture();
        if (tex != null) {
            try {
                minecraft.getTextureManager().getTexture(tex);
            } catch (Throwable ignored) {
            }
        }


        if (SkinIdUtil.isCpm(id)) {
            ResourceLocation skinTex = tex != null ? tex : ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "skinpacks/default/skins/steve.png");
            try {
                GuiCpmPreviewCache.prewarmMenuPreview(id, skinTex);
            } catch (Throwable ignored) {
            }
        }
    }

    private void touchTextureForId(PlayerSkinWidget w) {
        if (w == null) return;
        String id = w.skinId.get();
        if (id == null || id.isBlank()) return;
        SkinEntry entry = SkinPackLoader.getSkin(id);
        if (entry == null || entry.texture() == null) return;
        try {
            minecraft.getTextureManager().getTexture(entry.texture());
        } catch (Throwable ignored) {
        }
    }

    private boolean areRequiredReady(List<String> ids, int required) {
        if (required <= 0) return true;

        for (int i = 0; i < required; i++) {
            String id = ids.get(i);
            if (id == null || id.isBlank()) return false;

            SkinEntry entry = SkinPackLoader.getSkin(id);
            if (entry == null) return false;

            ResourceLocation tex = entry.texture();

            if (SkinIdUtil.isCpm(id)) {

                if (tex == null) return false;
                if (!GuiCpmPreviewCache.isResolved(id, tex)) return false;
            } else {

                if (tex == null) return false;
                if (TextureCompat.isMissingTexture(tex)) return false;
            }
        }
        return true;
    }

    private String getSelectedSkinId() {
        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        if (list == null || list.element3 == null) return null;
        return list.element3.skinId.get();
    }

    public void selectSkin() {
        String selectedId = getSelectedSkinId();
        if (selectedId == null) return;

        try {
            SkinPack focused = getFocusedPack();
            String packId = focused != null ? focused.id() : null;
            if (packId != null && SkinIdUtil.isFavouritesPack(packId)) {

                packId = SkinPackLoader.getSourcePackId(selectedId);
            }
            SkinPackLoader.setLastUsedCustomPackId(packId);
        } catch (Throwable ignored) {
        }

        String skinId = SkinIdUtil.isAutoSelect(selectedId) ? "" : selectedId;
        SkinSyncClient.requestSetSkin(minecraft, skinId);

        try {
            if (minecraft.player != null && SkinIdUtil.isCpm(skinId)) {
                CpmModelManager.applyToProfile(minecraft.player.getGameProfile(), skinId);
            }
        } catch (Throwable ignored) {
        }

        UUID self = minecraft.player != null ? minecraft.player.getUUID()
                : (minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null);
        if (self != null) ClientSkinCache.set(self, skinId);

        playClick();
    }

    public void favorite() {
        String skinId = getSelectedSkinId();
        if (skinId == null) return;

        PlayerSkinWidgetList list = host.getPlayerSkinWidgetList();
        int prevIndex = list != null ? list.index : 0;

        FavoritesStore.toggle(skinId);
        SkinPackLoader.rebuildFavouritesPack();

        SkinPack focused = getFocusedPack();
        if (focused != null && SkinIdUtil.isFavouritesPack(focused.id())) {
            int targetIndex = prevIndex;
            List<SkinEntry> skins = focused.skins();
            int size = skins == null ? 0 : skins.size();
            if (FavoritesStore.isFavorite(skinId) && skins != null) {
                for (int i = 0; i < size; i++) {
                    SkinEntry se = skins.get(i);
                    if (se != null && skinId.equals(se.id())) {
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
            minecraft.setScreen(HelpAndOptionsScreen.CHANGE_SKIN.build(host.getScreen()));
            playClick();
        } catch (Throwable t) {
            try {
                minecraft.setScreen(new HelpAndOptionsScreen(host.getScreen()));
                playClick();
            } catch (Throwable ignored) {
            }
        }
    }
}
