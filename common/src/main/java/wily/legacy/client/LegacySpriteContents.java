package wily.legacy.client;

import net.minecraft.server.packs.resources.ResourceMetadata;

public interface LegacySpriteContents {
    ResourceMetadata metadata();
    void setMetadata(ResourceMetadata metadata);
}
