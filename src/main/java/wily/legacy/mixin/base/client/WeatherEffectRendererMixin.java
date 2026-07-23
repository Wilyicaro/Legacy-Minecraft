//? if >=1.21.2 {
/*package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @ModifyConstant(method = "render", constant = @Constant(intValue = 10))
    private int legacy$weatherRadius(int radius) {
        return 9;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
    private float legacy$rainLevel(float rainLevel) {
        return rainLevel * rainLevel;
    }

    @Redirect(method = "createRainColumnInstance", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float legacy$rainRandom(RandomSource randomSource) {
        return (float) randomSource.nextDouble();
    }

    @ModifyVariable(method = "createRainColumnInstance", at = @At("STORE"), index = 13)
    private float legacy$rainAnimation(float animation, @Local(argsOnly = true, ordinal = 0) int tick, @Local(argsOnly = true, ordinal = 1) int x, @Local(argsOnly = true, ordinal = 4) int z, @Local(ordinal = 1) float speed, @Local(argsOnly = true, ordinal = 0) float partialTick) {
        int phase = tick + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761;
        return ((phase & 31) + partialTick) / 32.0F * speed;
    }

    @WrapOperation(method = "renderInstances", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer$ColumnInstance;bottomY:I", ordinal = 1))
    private int legacy$alignRainTopUv(@Coerce WeatherColumnInstanceAccessor instance, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? instance.legacy$getTopY() : original.call(instance);
    }

    @WrapOperation(method = "renderInstances", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer$ColumnInstance;topY:I", ordinal = 1))
    private int legacy$alignRainBottomUv(@Coerce WeatherColumnInstanceAccessor instance, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? instance.legacy$getBottomY() : original.call(instance);
    }

    @ModifyArg(method = "renderInstances", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
    private int legacy$fadeFirstRainTopVertex(int color, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? color & 0x00FFFFFF : color;
    }

    @ModifyArg(method = "renderInstances", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1))
    private int legacy$fadeSecondRainTopVertex(int color, @Local(argsOnly = true, ordinal = 0) float opacity) {
        return opacity == 1.0F ? color & 0x00FFFFFF : color;
    }
}
*///?}
