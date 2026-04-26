package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CloudRenderer.TextureData.class)
public interface CloudTextureDataAccessor {
    @Accessor("cells")
    long[] legacy$getCells();
    @Accessor("width")
    int legacy$getWidth();
    @Accessor("height")
    int legacy$getHeight();
}
