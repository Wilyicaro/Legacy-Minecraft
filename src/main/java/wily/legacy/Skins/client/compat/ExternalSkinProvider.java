package wily.legacy.Skins.client.compat;

import wily.legacy.Skins.skin.SkinPackSourceKind;

public interface ExternalSkinProvider {

    String providerId();

    SkinPackSourceKind sourceKind();

    boolean ownsSkinId(String skinId);

    ExternalSkinProviderCapabilities capabilities();

    default boolean isAvailable() {
        return capabilities().coreAvailable();
    }
}