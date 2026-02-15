package wily.legacy.CustomModelSkins.cpm.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.CustomModelSkins.cpl.render.RenderTypeBuilder.TextureHandler;

import java.util.function.Function;

public class ModelTexture implements TextureHandler<ResourceLocation, RenderType> {
    private CallbackInfoReturnable<ResourceLocation> texture;
    private Function<ResourceLocation, RenderType> renderType;

    public ModelTexture(CallbackInfoReturnable<ResourceLocation> texture, Function<ResourceLocation, RenderType> renderType) {
        this.texture = texture;
        this.renderType = renderType;
    }

    public ModelTexture(Function<ResourceLocation, RenderType> renderType) {
        this(new CallbackInfoReturnable<>(null, true), renderType);
    }

    public ModelTexture(ResourceLocation tex, Function<ResourceLocation, RenderType> renderType) {
        this(new CallbackInfoReturnable<>(null, true, tex), renderType);
    }

    public ModelTexture(CallbackInfoReturnable<ResourceLocation> texture) {
        this(texture, PlayerRenderManager.entity);
    }

    public ModelTexture(ResourceLocation tex) {
        this(tex, PlayerRenderManager.entity);
    }

    @Override
    public ResourceLocation getTexture() {
        return texture.getReturnValue();
    }

    @Override
    public void setTexture(ResourceLocation texture) {
        this.texture.setReturnValue(texture);
    }

    @Override
    public RenderType getRenderType() {
        return renderType.apply(getTexture());
    }
}
