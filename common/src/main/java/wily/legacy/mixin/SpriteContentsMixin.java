package wily.legacy.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.LegacySpriteContents;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements LegacySpriteContents {
    ResourceMetadata metadata = ResourceMetadata.EMPTY;
    @Override
    public ResourceMetadata metadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ResourceMetadata metadata) {
        this.metadata = metadata;
    }
}
