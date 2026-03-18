package wily.legacy.Skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4JClient;
import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.client.compat.bedrockskins.BedrockSkinsCompat;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.util.ViewBobbingSkinOverride;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinPackReloadListener;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.SkinSyncClient;

public final class SkinsClientBootstrap {
    private SkinsClientBootstrap() {
    }

    private static final SkinPackReloadListener SKIN_PACK_RELOAD_LISTENER = new SkinPackReloadListener();
    private static volatile boolean reloadListenerRegistered = false;
    private static volatile boolean packPreloadStarted = false;
    private static volatile boolean defaultSelectionChecked = false;

    public static void initClient() {
        SkinSyncClient.initClient();
        ExternalSkinProviders.logCapabilitiesOnce();

        Legacy4JClient.whenResetOptions.add(ConsoleSkinsClientSettings::resetToDefaults);

        Legacy4JClient.whenResetOptions.add(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                ExternalSkinProviders.clearAllSelectedSkins();
                SkinPackLoader.setLastUsedCustomPackId(null);
                SkinSyncClient.requestSetSkin(mc, "");
                ConsoleSkinsClientSettings.setSkinSelectionInitialized(true);
            } catch (Throwable ignored) {
            }
        });

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
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        BedrockSkinsCompat.redirectLegacyScreenIfNeeded(minecraft);
        SkinSyncClient.postTick(minecraft);
        ViewBobbingSkinOverride.tick(minecraft);

        if (!defaultSelectionChecked) {
            try {
                if (minecraft != null && minecraft.getUser() != null) {
                    if (!ConsoleSkinsClientSettings.isSkinSelectionInitialized()) {
                        java.util.UUID uid = minecraft.getUser().getProfileId();
                        String existing = null;
                        try {
                            existing = wily.legacy.Skins.skin.ClientSkinCache.get(uid);
                        } catch (Throwable ignored) {
                        }
                        if (existing == null || existing.isBlank()) {
                            try {
                                existing = wily.legacy.Skins.skin.ClientSkinPersistence.load(uid);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (existing == null || existing.isBlank()) {
                            existing = ExternalSkinProviders.getCurrentSelectedSkinId();
                        }
                        if (existing == null || existing.isBlank()) {
                            SkinPackLoader.setLastUsedCustomPackId(null);
                            SkinSyncClient.requestSetSkin(minecraft, "");
                        }
                        ConsoleSkinsClientSettings.setSkinSelectionInitialized(true);
                    }
                    defaultSelectionChecked = true;
                }
            } catch (Throwable ignored) {
            }
        }

        if (!packPreloadStarted) {
            try {
                if (!SkinPackLoader.isLoaded() && minecraft != null && minecraft.getResourceManager() != null) {
                    packPreloadStarted = true;
                    try {
                        SkinPackLoader.ensureLoaded();
                    } catch (Throwable ignored) {
                    }
                    try {
                        BoxModelManager.reload(minecraft.getResourceManager());
                    } catch (Throwable ignored) {
                    }
                    if (!SkinPackLoader.isLoaded()) {
                        packPreloadStarted = false;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static Screen createChangeSkinScreen(Screen parent) {
        try {
            SkinPackLoader.ensureLoaded();
        } catch (Throwable ignored) {
        }

        try {
            if (ConsoleSkinsClientSettings.isTu3ChangeSkinScreen()) {
                return new wily.legacy.Skins.client.screen.TU3ChangeSkinScreen(parent);
            }
            return new wily.legacy.Skins.client.screen.ChangeSkinScreen(parent);
        } catch (Throwable ignored) {
            return parent;
        }
    }

    public static void requestOpenChangeSkinScreen(Minecraft minecraft, Screen parent) {
        try {
            if (minecraft == null) return;
            minecraft.setScreen(createChangeSkinScreen(parent));
        } catch (Throwable ignored) {
        }
    }
}
