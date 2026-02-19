package wily.legacy.Skins;

import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.CustomModelSkins.cpm.CustomPlayerModels;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.util.ViewBobbingSkinOverride;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinPackReloadListener;
import wily.legacy.Skins.skin.SkinSyncClient;

import java.util.List;

public final class SkinsClientBootstrap {
    private SkinsClientBootstrap() {
    }

    private static final SkinPackReloadListener SKIN_PACK_RELOAD_LISTENER = new SkinPackReloadListener();
    private static volatile boolean reloadListenerRegistered = false;
    private static volatile int pendingOpenTimeoutTicks = -1;
    private static volatile net.minecraft.client.gui.screens.Screen pendingOpenParent;
    private static volatile String pendingOpenSelectedId;
    private static volatile net.minecraft.resources.ResourceLocation pendingOpenSelectedTexture;

    public static void initClient() {
        if (Legacy4J.INTERNAL_CPM_ENABLED) {
            CustomPlayerModels.initCommon();
            CustomPlayerModelsClient.initClient();
        }
        SkinSyncClient.initClient();
        if (Legacy4J.INTERNAL_CPM_ENABLED) {
            CpmModelManager.init();
        }

        Legacy4JClient.whenResetOptions.add(ConsoleSkinsClientSettings::resetToDefaults);

        FactoryAPIClient.setup(m -> {
            try {
                SkinSyncClient.ensureInitialSkinLoaded(m);
            } catch (Throwable ignored) {
            }
        });

        FactoryAPIClient.postTick(SkinsClientBootstrap::postTick);

        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(p -> {
            SkinSyncClient.onClientDisconnect();
            try {
                ViewBobbingSkinOverride.reset(Minecraft.getInstance());
            } catch (Throwable ignored) {
            }
        });
        FactoryAPIClient.PlayerEvent.JOIN_EVENT.register(p -> SkinSyncClient.onClientJoin());
    }

    private static void postTick(Minecraft minecraft) {

        if (!reloadListenerRegistered) {
            try {
                if (minecraft != null) {
                    Object rm = minecraft.getResourceManager();
                    if (rm != null) {
                        try {

                            java.lang.reflect.Method m = rm.getClass().getMethod(
                                    "registerReloadListener",
                                    net.minecraft.server.packs.resources.PreparableReloadListener.class
                            );
                            m.invoke(rm, SKIN_PACK_RELOAD_LISTENER);
                            reloadListenerRegistered = true;
                        } catch (NoSuchMethodException ignoredNsme) {

                        }
                    }
                }
            } catch (Throwable ignored) {

            }
        }
        SkinSyncClient.postTick(minecraft);
        CpmModelManager.tick(minecraft);
        ViewBobbingSkinOverride.tick(minecraft);

        if (pendingOpenTimeoutTicks >= 0) {
            boolean openNow = false;
            try {
                if (pendingOpenSelectedId == null || pendingOpenSelectedId.isBlank()) {
                    openNow = true;
                } else if (!SkinIdUtil.isCpm(pendingOpenSelectedId)) {
                    openNow = true;
                } else if (pendingOpenSelectedTexture != null && GuiCpmPreviewCache.isResolved(pendingOpenSelectedId, pendingOpenSelectedTexture)) {
                    openNow = true;
                }
            } catch (Throwable ignored) {
                openNow = true;
            }

            if (!openNow) {
                if (pendingOpenTimeoutTicks > 0) {
                    pendingOpenTimeoutTicks--;
                } else {
                    openNow = true;
                }
            }

            if (openNow) {
                try {
                    if (minecraft != null) minecraft.setScreen(new wily.legacy.Skins.client.screen.ChangeSkinScreen(pendingOpenParent));
                } catch (Throwable ignored) {
                }
                pendingOpenTimeoutTicks = -1;
                pendingOpenParent = null;
                pendingOpenSelectedId = null;
                pendingOpenSelectedTexture = null;
            }
        }
    }

    public static void requestOpenChangeSkinScreen(Minecraft minecraft, net.minecraft.client.gui.screens.Screen parent) {
        pendingOpenParent = parent;
        pendingOpenSelectedId = null;
        pendingOpenSelectedTexture = null;
        pendingOpenTimeoutTicks = 6;

        try {
            if (minecraft == null) return;
            if (minecraft.player != null) {
                SkinPackLoader.ensureLoaded();
                String selected = ClientSkinCache.get(minecraft.player.getUUID());
                pendingOpenSelectedId = selected;
                SkinEntry selectedEntry = selected != null ? SkinPackLoader.getSkin(selected) : null;
                pendingOpenSelectedTexture = selectedEntry != null ? selectedEntry.texture() : null;
                String packId = null;
                if (selected != null && !selected.isBlank()) packId = SkinPackLoader.getSourcePackId(selected);
                if (packId == null || packId.isBlank()) packId = SkinPackLoader.getLastUsedCustomPackId();
                if (packId != null && !packId.isBlank()) {
                    SkinPack pack = SkinPackLoader.getPacks().get(packId);
                    if (pack != null) {
                        List<SkinEntry> skins = pack.skins();
                        int limit = Math.min(48, skins.size());
                        for (int i = 0; i < limit; i++) {
                            SkinEntry e = skins.get(i);
                            if (e == null) continue;
                            String id = e.id();
                            if (id == null || id.isBlank()) continue;
                            if (SkinIdUtil.isCpm(id)) GuiCpmPreviewCache.prewarmMenuPreview(id, e.texture());
                        }
                    }
                }
                if (selected != null && !selected.isBlank() && SkinIdUtil.isCpm(selected) && pendingOpenSelectedTexture != null) {
                    GuiCpmPreviewCache.prewarmMenuPreview(selected, pendingOpenSelectedTexture);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
