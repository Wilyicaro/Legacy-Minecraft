package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.CommonColor;

@Mixin(LightmapRenderStateExtractor.class)
public class LightTextureMixin {

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 0))
    public Object updateLightTexture(Object original) {
        return CommonColor.BLOCK_LIGHT.get() & 0xFFFFFF;
    }
}
