package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EvokerFangsRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
//?} else {
/*import net.minecraft.world.entity.projectile.EvokerFangs;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(EvokerFangsRenderer.class)
public class EvokerFangsRendererMixin {
    //? if <1.21.2 {
    /*@Inject(method =  "render(Lnet/minecraft/world/entity/projectile/EvokerFangs;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    public void render(/^? if <1.21.2 {^//^EvokerFangs evokerFangs, float f, float g^//^?} else {^/EvokerFangsRenderState evokerFangsRenderState/^?}^/, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
      if (LegacyOptions.legacyEvokerFangs.get()) poseStack.scale(2f,2f,2f);
    }
    *///?} else {
    @Inject(method =  "render(Lnet/minecraft/client/renderer/entity/state/EvokerFangsRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    public void render(EvokerFangsRenderState evokerFangsRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (LegacyOptions.legacyEvokerFangs.get()) poseStack.scale(2f,2f,2f);
    }
    //?}
}
