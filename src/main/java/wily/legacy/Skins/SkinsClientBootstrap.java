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
    private static volatile int pendingOpenReadyDelayTicks = -1;
    private static volatile net.minecraft.client.gui.screens.Screen pendingOpenParent;
    private static volatile String pendingOpenSelectedId;
    private static volatile net.minecraft.resources.ResourceLocation pendingOpenSelectedTexture;
    private static volatile List<String> pendingOpenRequiredIds;
    private static volatile List<net.minecraft.resources.ResourceLocation> pendingOpenRequiredTextures;

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
        if (Legacy4J.INTERNAL_CPM_ENABLED) {
            CpmModelManager.tick(minecraft);
        }
        ViewBobbingSkinOverride.tick(minecraft);

        if (pendingOpenTimeoutTicks >= 0) {
            boolean openNow = false;
            try {
                List<String> reqIds = pendingOpenRequiredIds;
                List<net.minecraft.resources.ResourceLocation> reqTex = pendingOpenRequiredTextures;
                if (reqIds != null && !reqIds.isEmpty() && reqTex != null && reqTex.size() == reqIds.size()) {
                    boolean allReady = true;
                    for (int i = 0; i < reqIds.size(); i++) {
                        String id = reqIds.get(i);
                        net.minecraft.resources.ResourceLocation tex = reqTex.get(i);
                        try {
                            GuiCpmPreviewCache.resolveMenuPreviewNow(id, tex);
                        } catch (Throwable ignored2) {
                        }
                        if (id == null || id.isBlank() || tex == null || !GuiCpmPreviewCache.isResolved(id, tex)) {
                            allReady = false;
                            break;
                        }
                    }
                    openNow = allReady;
                } else {
                    if (pendingOpenSelectedId == null || pendingOpenSelectedId.isBlank()) {
                        openNow = true;
                    } else if (!SkinIdUtil.isCpm(pendingOpenSelectedId)) {
                        openNow = true;
                    } else if (pendingOpenSelectedTexture != null) {
                        try {
                            GuiCpmPreviewCache.resolveMenuPreviewNow(pendingOpenSelectedId, pendingOpenSelectedTexture);
                        } catch (Throwable ignored2) {
                        }
                        if (GuiCpmPreviewCache.isResolved(pendingOpenSelectedId, pendingOpenSelectedTexture)) {
                            openNow = true;
                        }
                    }
                }
            } catch (Throwable ignored) {
                openNow = true;
            }

            boolean hasRequired = pendingOpenRequiredIds != null && !pendingOpenRequiredIds.isEmpty() && pendingOpenRequiredTextures != null && pendingOpenRequiredTextures.size() == pendingOpenRequiredIds.size();
            if (!openNow) {
                if (pendingOpenTimeoutTicks > 0) {
                    pendingOpenTimeoutTicks--;
                } else if (!hasRequired) {
                    openNow = true;
                }
            }

            if (openNow) {
                if (pendingOpenReadyDelayTicks < 0) {
                    pendingOpenReadyDelayTicks = 2;
                    openNow = false;
                } else if (pendingOpenReadyDelayTicks > 0) {
                    pendingOpenReadyDelayTicks--;
                    openNow = false;
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
                pendingOpenRequiredIds = null;
                pendingOpenRequiredTextures = null;
            }
        }
    }

    private static int mod(int value, int n) {
        int m = value % n;
        return m < 0 ? m + n : m;
    }

    public static void requestOpenChangeSkinScreen(Minecraft minecraft, net.minecraft.client.gui.screens.Screen parent) {
        pendingOpenParent = parent;
        pendingOpenSelectedId = null;
        pendingOpenSelectedTexture = null;
        pendingOpenRequiredIds = null;
        pendingOpenRequiredTextures = null;
        pendingOpenTimeoutTicks = 1200;
        pendingOpenReadyDelayTicks = -1;

        try {
            if (minecraft == null) return;
            SkinPackLoader.ensureLoaded();
            String selected = null;
            if (minecraft.player != null) {
                selected = ClientSkinCache.get(minecraft.player.getUUID());
                pendingOpenSelectedId = selected;
                SkinEntry selectedEntry = selected != null ? SkinPackLoader.getSkin(selected) : null;
                pendingOpenSelectedTexture = selectedEntry != null ? selectedEntry.texture() : null;
            }

            String packId = null;
            if (selected != null && !selected.isBlank()) packId = SkinPackLoader.getSourcePackId(selected);
            if (packId == null || packId.isBlank()) packId = SkinPackLoader.getLastUsedCustomPackId();
            if (packId == null || packId.isBlank()) packId = SkinPackLoader.getPreferredDefaultPackId();
            if (packId != null && !packId.isBlank()) {
                SkinPack pack = SkinPackLoader.getPacks().get(packId);
                if (pack != null) {
                    List<SkinEntry> skins = pack.skins();
                    int n = skins == null ? 0 : skins.size();
                    if (n > 0) {
                        int selectedIndex = 0;
                        if (selected != null && !selected.isBlank()) {
                            int limit = Math.min(100, n);
                            for (int i = 0; i < limit; i++) {
                                SkinEntry se = skins.get(i);
                                if (se != null && selected.equals(se.id())) {
                                    selectedIndex = i;
                                    break;
                                }
                            }
                        }
                        int[] offsets = {0, -1, 1, -2, 2};
                        List<String> reqIds = new java.util.ArrayList<>(5);
                        List<net.minecraft.resources.ResourceLocation> reqTex = new java.util.ArrayList<>(5);
                        for (int off : offsets) {
                            SkinEntry e = skins.get(mod(selectedIndex + off, n));
                            if (e == null) continue;
                            String id = e.id();
                            if (id == null || id.isBlank()) continue;
                            if (!SkinIdUtil.isCpm(id)) continue;
                            net.minecraft.resources.ResourceLocation tex = e.texture();
                            if (tex == null) continue;
                            reqIds.add(id);
                            reqTex.add(tex);
                            GuiCpmPreviewCache.prewarmMenuPreview(id, tex);
                        }
                        if (!reqIds.isEmpty()) {
                            pendingOpenRequiredIds = reqIds;
                            pendingOpenRequiredTextures = reqTex;
                        }
                    }
                }
            }

            if (selected != null && !selected.isBlank() && SkinIdUtil.isCpm(selected) && pendingOpenSelectedTexture != null) {
                GuiCpmPreviewCache.prewarmMenuPreview(selected, pendingOpenSelectedTexture);
            }
        } catch (Throwable ignored) {
        }
    }
}
