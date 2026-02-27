package wily.legacy.Skins.skin;

import net.minecraft.resources.ResourceLocation;

public record SkinEntry(String id, String name, ResourceLocation texture, ResourceLocation cape, boolean slimArms, int order) {
}
