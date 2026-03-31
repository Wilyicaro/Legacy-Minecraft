package wily.legacy.Skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.PackType;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryEvent;
import wily.legacy.Legacy4JClient;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.util.SkinPreviewWarmup;
import wily.legacy.Skins.client.util.ViewBobbingSkinOverride;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.SkinSyncClient;

public final class SkinsClientBootstrap {
    private SkinsClientBootstrap() {
    }

    private static final net.minecraft.server.packs.resources.ResourceManagerReloadListener SKIN_PACK_RELOAD_LISTENER = new net.minecraft.server.packs.resources.ResourceManagerReloadListener() {
        @Override
        public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    ClientSkinAssets.clearPreviewCaches();
                    SkinPreviewWarmup.clear();
                    BoxModelManager.reload(manager);
                    SkinPackLoader.loadPacks(manager);
                });
                return;
            }
            ClientSkinAssets.clearPreviewCaches();
            SkinPreviewWarmup.clear();
            BoxModelManager.reload(manager);
            SkinPackLoader.loadPacks(manager);
        }

        @Override
        public String getName() {
            return "legacy:skins";
        }
    };
    private static volatile boolean packPreloadStarted = false;
    private static volatile boolean defaultSelectionChecked = false;

    public static void initClient() {
        SkinSyncClient.initClient();

        Legacy4JClient.whenResetOptions.add(ConsoleSkinsClientSettings::resetToDefaults);

        Legacy4JClient.whenResetOptions.add(() -> {
            Minecraft mc = Minecraft.getInstance();
            SkinPackLoader.setLastUsedCustomPackId(null);
            SkinSyncClient.requestSetSkin(mc, "");
            ConsoleSkinsClientSettings.setSkinSelectionInitialized(true);
        });

        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, SKIN_PACK_RELOAD_LISTENER);
        FactoryAPIClient.setup(SkinSyncClient::ensureInitialSkinLoaded);

        FactoryAPIClient.postTick(SkinsClientBootstrap::postTick);

        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(p -> {
            SkinSyncClient.onClientDisconnect();
            ViewBobbingSkinOverride.reset(Minecraft.getInstance());
        });
        FactoryAPIClient.PlayerEvent.JOIN_EVENT.register(p -> SkinSyncClient.onClientJoin());
    }

    private static void postTick(Minecraft minecraft) {
        SkinSyncClient.postTick(minecraft);
        ViewBobbingSkinOverride.tick(minecraft);

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
            if (ConsoleSkinsClientSettings.isTu3ChangeSkinScreen()) {
                return new wily.legacy.Skins.client.screen.TU3ChangeSkinScreen(parent);
            }
            return new wily.legacy.Skins.client.screen.ChangeSkinScreen(parent);
        } catch (RuntimeException ex) {
            return parent;
        }
    }

    public static void requestOpenChangeSkinScreen(Minecraft minecraft, Screen parent) {
        if (minecraft == null) return;
        minecraft.setScreen(createChangeSkinScreen(parent));
    }

    private static void checkDefaultSelection(Minecraft minecraft) {
        if (minecraft == null || minecraft.getUser() == null) return;
        if (!ConsoleSkinsClientSettings.isSkinSelectionInitialized()) {
            java.util.UUID userId = minecraft.getUser().getProfileId();
            String existing = wily.legacy.Skins.skin.ClientSkinCache.get(userId);
            if (existing == null || existing.isBlank()) {
                existing = wily.legacy.Skins.skin.ClientSkinPersistence.load(userId);
            }
            if (existing == null || existing.isBlank()) {
                SkinPackLoader.setLastUsedCustomPackId(null);
                SkinSyncClient.requestSetSkin(minecraft, "");
            }
            ConsoleSkinsClientSettings.setSkinSelectionInitialized(true);
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
