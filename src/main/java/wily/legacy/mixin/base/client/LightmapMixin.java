package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyLightmapRenderState;

@Mixin(Lightmap.class)
public class LightmapMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", remap = false, ordinal = 5))
    private Std140Builder legacy$writeUnderwaterVisionFactor(Std140Builder original, @Local(argsOnly = true) LightmapRenderState renderState) {
        return original.putFloat(LegacyLightmapRenderState.of(renderState).getUnderwaterVisionFactor());
    }
}
