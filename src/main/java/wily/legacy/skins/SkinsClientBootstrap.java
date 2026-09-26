package wily.legacy.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.PackType;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryEvent;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.ScreenSection;
import wily.legacy.skins.client.gui.GuiSessionSkin;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.screen.AbstractChangeSkinScreen;
import wily.legacy.skins.client.util.ViewBobbingSkinOverride;
import wily.legacy.skins.skin.*;
import wily.legacy.skins.util.SkinsLogger;

public final class SkinsClientBootstrap {
    private static final net.minecraft.server.packs.resources.ResourceManagerReloadListener SKIN_PACK_RELOAD_LISTENER = new net.minecraft.server.packs.resources.ResourceManagerReloadListener() {
        @Override
        public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    ClientSkinAssets.clearPreviewCaches();
                    ClientSkinAssets.clearPreviewWarmup();
                    BoxModelManager.reload(manager);
                    SkinPackLoader.loadPacks(manager);
                    SkinSyncClient.onSkinAssetsReloaded(mc);
                });
                return;
            }
            ClientSkinAssets.clearPreviewCaches();
            ClientSkinAssets.clearPreviewWarmup();
            BoxModelManager.reload(manager);
            SkinPackLoader.loadPacks(manager);
            SkinSyncClient.onSkinAssetsReloaded(mc);
        }

        @Override
        public String getName() {
            return "legacy:skins";
        }
    };
    private static volatile boolean packPreloadStarted = false;
    private static volatile boolean defaultSelectionChecked = false;

    private SkinsClientBootstrap() {
    }

    public static void init() {
        SkinSyncClient.init();

        Legacy4JClient.whenResetOptions.add(() -> {
            Minecraft mc = Minecraft.getInstance();
            SkinPackLoader.setLastUsedCustomPackId(null);
            SkinSyncClient.requestSetSkin(mc, "");
            LegacyOptions.skinSelectionInitialized.set(true);
        });

        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, SKIN_PACK_RELOAD_LISTENER);
        FactoryAPIClient.PlayerEvent.JOIN_EVENT.register(p -> SkinSyncClient.onClientJoin());
    }

    public static void postTick(Minecraft minecraft) {
        SkinSyncClient.postTick(minecraft);
        ViewBobbingSkinOverride.tick(minecraft);
        GuiSessionSkin.prewarm();
        if (!defaultSelectionChecked) {
            checkDefaultSelection(minecraft);
        }
        if (!packPreloadStarted) {
            preloadPacks(minecraft);
        }
    }

    public static Screen createChangeSkinScreen(Screen parent) {
        SkinPackLoader.ensureLoaded();
        try {
            if (LegacyOptions.tu3ChangeSkinScreen.get()) {
                return new wily.legacy.skins.client.screen.TU3ChangeSkinScreen(parent);
            }
            return new wily.legacy.skins.client.screen.ChangeSkinScreen(parent);
        } catch (RuntimeException ex) {
            SkinsLogger.debug("Failed to create change skin screen {}", ex.toString());
            return parent;
        }
    }

    public static void requestOpenChangeSkinScreen(Minecraft minecraft, Screen parent) {
        if (minecraft == null) return;
        GuiSessionSkin.prewarm();
        minecraft.setScreen(createChangeSkinScreen(parent));
    }

    public static void reloadChangeSkinScreen(Minecraft minecraft, Screen parent) {
        if (minecraft == null) return;
        if (parent instanceof AbstractChangeSkinScreen screen) {
            String packId = screen.focusedPackId();
            String skinId = screen.focusedSkinId();
            if (packId != null && !packId.isBlank()) SkinPackLoader.requestFocusPack(packId);
            if (skinId != null && !skinId.isBlank()) SkinPackLoader.requestFocusSkin(skinId);
        }
        minecraft.setScreen(new wily.legacy.client.screen.LegacyLoadingScreen());
        minecraft.reloadResourcePacks().thenRun(() -> minecraft.execute(() -> requestOpenChangeSkinScreen(minecraft, parent)));
    }

    public static void reloadChangeSkinScreen(Minecraft minecraft, Screen parent, String packId, String skinId) {
        reloadChangeSkinScreen(minecraft, parent, packId, skinId, false);
    }

    public static void reloadChangeSkinScreen(Minecraft minecraft, Screen parent, String packId, String skinId, boolean reorder) {
        if (packId != null && !packId.isBlank()) {
            SkinPackLoader.requestFocusPack(packId);
            SkinPackLoader.setLastUsedCustomPackId(packId);
            if (reorder) SkinPackLoader.requestReorderPack(packId);
        }
        if (skinId != null && !skinId.isBlank()) SkinPackLoader.requestFocusSkin(skinId);
        if (minecraft == null) return;
        minecraft.setScreen(new wily.legacy.client.screen.LegacyLoadingScreen());
        minecraft.reloadResourcePacks().thenRun(() -> minecraft.execute(() -> requestOpenChangeSkinScreen(minecraft, parent)));
    }

    private static void checkDefaultSelection(Minecraft minecraft) {
        if (minecraft == null || minecraft.getUser() == null) return;
        if (!LegacyOptions.skinSelectionInitialized.get()) {
            java.util.UUID userId = minecraft.getUser().getProfileId();
            String existing = wily.legacy.skins.skin.ClientSkinCache.get(userId);
            if (existing == null || existing.isBlank()) existing = SkinDataStore.getSelectedSkin(userId);
            if (existing == null || existing.isBlank()) {
                SkinPackLoader.setLastUsedCustomPackId(null);
                SkinSyncClient.requestSetSkin(minecraft, "");
            }
            LegacyOptions.skinSelectionInitialized.set(true);
        }
        defaultSelectionChecked = true;
    }

    private static void preloadPacks(Minecraft minecraft) {
        if (minecraft == null || minecraft.getResourceManager() == null || SkinPackLoader.isLoaded()) return;
        packPreloadStarted = true;
        SkinPackLoader.ensureLoaded();
        BoxModelManager.reload(minecraft.getResourceManager());
        if (!SkinPackLoader.isLoaded()) {
            packPreloadStarted = false;
        }
    }
}
