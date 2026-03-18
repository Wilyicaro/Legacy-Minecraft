package wily.legacy.Skins.client.compat;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ExternalSkinPackDescriptor(String id,
                                         String name,
                                         String type,
                                         ResourceLocation icon,
                                         List<ExternalSkinDescriptor> skins,
                                         Integer nativeOrder) {
}
