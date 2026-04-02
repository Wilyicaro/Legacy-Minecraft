package wily.legacy.Skins.skin;

import net.minecraft.resources.ResourceLocation;

public record SkinEntry(String id, String sourceId, String name, ResourceLocation texture, ResourceLocation modelId,
                        ResourceLocation cape, boolean slimArms, int order) { }
