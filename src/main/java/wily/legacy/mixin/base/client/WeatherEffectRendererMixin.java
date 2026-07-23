package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I"))
    private int changeWeatherRadius(int radius) {
        return radius == 10 ? 9 : radius;
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
    private float squareRainLevel(float rainLevel) {
        return rainLevel * rainLevel;
    }

    @Inject(method = "createRainColumnInstance", at = @At("HEAD"), cancellable = true)
    private void createRainColumnInstance(RandomSource randomSource, int tick, int x, int bottomY, int topY, int z, int light, float partialTick, CallbackInfoReturnable<WeatherEffectRenderer.ColumnInstance> cir) {
        int phase = tick + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761;
        float animation = (float)((((phase & 31) + partialTick) / 32.0) * (3.0 + randomSource.nextDouble()));
        cir.setReturnValue(new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, 0.0F, animation, light));
    }

    @WrapOperation(method = "renderInstances", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer$ColumnInstance;bottomY:I", ordinal = 1))
    private int alignRainTopUv(WeatherEffectRenderer.ColumnInstance instance, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? instance.topY() : original.call(instance);
    }

    @WrapOperation(method = "renderInstances", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer$ColumnInstance;topY:I", ordinal = 1))
    private int alignRainBottomUv(WeatherEffectRenderer.ColumnInstance instance, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? instance.bottomY() : original.call(instance);
    }

    @ModifyArg(method = "renderInstances", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
    private int fadeFirstRainTopVertex(int color, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? color & 0x00FFFFFF : color;
    }

    @ModifyArg(method = "renderInstances", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1))
    private int fadeSecondRainTopVertex(int color, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? color & 0x00FFFFFF : color;
    }
}
