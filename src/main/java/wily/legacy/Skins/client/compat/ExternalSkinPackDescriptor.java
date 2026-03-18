package wily.legacy.Skins.client.compat;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.skin.SkinPackSourceKind;

import java.util.List;
import java.util.Objects;

public record ExternalSkinPackDescriptor(String id,
                                         String name,
                                         String type,
                                         ResourceLocation icon,
                                         List<ExternalSkinDescriptor> skins,
                                         Integer nativeOrder,
                                         String providerId,
                                         SkinPackSourceKind sourceKind) {

    public ExternalSkinPackDescriptor(String id,
                                      String name,
                                      String type,
                                      ResourceLocation icon,
                                      List<ExternalSkinDescriptor> skins,
                                      Integer nativeOrder) {
        this(id, name, type, icon, skins, nativeOrder, null, null);
    }

    public ExternalSkinPackDescriptor withProviderMetadata(String providerId, SkinPackSourceKind sourceKind) {
        if (Objects.equals(this.providerId, providerId) && this.sourceKind == sourceKind) return this;
        return new ExternalSkinPackDescriptor(id, name, type, icon, skins, nativeOrder, providerId, sourceKind);
    }
}
