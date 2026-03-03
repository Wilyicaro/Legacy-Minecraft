package wily.legacy.api;

import net.minecraft.resources.ResourceLocation;

public record ContentPack(
    String id,
    String name,
    String description,
    String downloadURI,
    String imageUrl
) {}