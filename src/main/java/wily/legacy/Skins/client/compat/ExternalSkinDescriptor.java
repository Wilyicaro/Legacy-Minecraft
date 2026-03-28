package wily.legacy.Skins.client.compat;

import wily.legacy.Skins.skin.SkinPackSourceKind;

import java.util.Objects;

public record ExternalSkinDescriptor(String id, String name, String providerId, SkinPackSourceKind sourceKind) {

    public ExternalSkinDescriptor(String id, String name) {
        this(id, name, null, null);
    }

    public ExternalSkinDescriptor withProviderMetadata(String providerId, SkinPackSourceKind sourceKind) {
        if (Objects.equals(this.providerId, providerId) && this.sourceKind == sourceKind) return this;
        return new ExternalSkinDescriptor(id, name, providerId, sourceKind);
    }
}
