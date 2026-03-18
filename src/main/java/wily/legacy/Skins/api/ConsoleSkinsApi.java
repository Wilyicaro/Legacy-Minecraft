package wily.legacy.Skins.api;

import wily.legacy.Skins.client.compat.ExternalSkinPackDescriptor;
import wily.legacy.Skins.client.compat.ExternalSkinProvider;
import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.skin.SkinSync;
import wily.legacy.Skins.skin.SkinPackSourceKind;
import wily.legacy.Skins.skin.SkinSyncClient;
import wily.legacy.Skins.util.DebugLog;

import java.util.List;

public final class ConsoleSkinsApi {

    private ConsoleSkinsApi() {
    }

    public static String modId() {
        return SkinSync.MODID;
    }

    public static boolean requestClientSkinChange(String skinId) {
        try {
            return SkinSyncClient.requestSetSkin(skinId);
        } catch (Throwable t) {
            DebugLog.warn("Skin change request failed: {}", t.toString());
            return false;
        }
    }

    public static boolean registerProvider(ExternalSkinProvider provider) {
        return ExternalSkinProviders.registerProvider(provider);
    }

    public static List<ExternalSkinProvider> providers() {
        return ExternalSkinProviders.providers();
    }

    public static boolean isExternalSkinId(String skinId) {
        return ExternalSkinProviders.isExternalSkinId(skinId);
    }

    public static boolean isSourceAvailable(SkinPackSourceKind sourceKind) {
        return ExternalSkinProviders.isSourceAvailable(sourceKind);
    }

    public static boolean hasAdditionalPackProvider() {
        return ExternalSkinProviders.hasAdditionalPackProvider();
    }

    public static String getCurrentSelectedSkinId() {
        return ExternalSkinProviders.getCurrentSelectedSkinId();
    }

    public static boolean applySelectedSkin(String skinId) {
        return ExternalSkinProviders.applySelectedSkin(skinId);
    }

    public static void clearAllSelectedSkins() {
        ExternalSkinProviders.clearAllSelectedSkins();
    }

    public static List<ExternalSkinPackDescriptor> loadPackDescriptors(SkinPackSourceKind sourceKind) {
        return ExternalSkinProviders.loadPackDescriptors(sourceKind);
    }
}
