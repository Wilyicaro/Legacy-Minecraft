package wily.legacy.Skins;

import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.CustomModelSkins.cpm.CustomPlayerModels;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.Legacy4JClient;
import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.SkinPackReloadListener;
import wily.legacy.Skins.skin.SkinSyncClient;

public final class SkinsClientBootstrap {
    private SkinsClientBootstrap() {
    }

    private static final SkinPackReloadListener SKIN_PACK_RELOAD_LISTENER = new SkinPackReloadListener();
    private static volatile boolean reloadListenerRegistered = false;

    public static void initClient() {

        CustomPlayerModels.initCommon();
        CustomPlayerModelsClient.initClient();

        SkinSyncClient.initClient();
        CpmModelManager.init();

        Legacy4JClient.whenResetOptions.add(ConsoleSkinsClientSettings::resetToDefaults);

        FactoryAPIClient.setup(m -> {
            try {
                SkinSyncClient.ensureInitialSkinLoaded(m);
            } catch (Throwable ignored) {
            }
        });

        FactoryAPIClient.postTick(SkinsClientBootstrap::postTick);

        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(p -> SkinSyncClient.onClientDisconnect());
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
    }
}
