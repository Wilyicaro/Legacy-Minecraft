package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PufferfishRenderState;
//?} else {
/*import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
*///?}
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CodRenderer.class, SalmonRenderer.class, TropicalFishRenderer.class, PufferfishRenderer.class})
public class FishRendererMixin {
    @Inject(method = /*? if <1.20.5 {*//*"setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"*//*?} else if <1.21.2 {*//*"setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"*//*?} else {*/"setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V"/*?}*/, at = @At("TAIL"))
    private void legacy$flop(/*? if <1.21.2 {*//*LivingEntity entity*//*?} else {*/LivingEntityRenderState state/*?}*/, PoseStack poseStack, float ageInTicks, float bodyYaw, /*? if <1.21.2 {*/ /*float partialTick,*//*?}*/ /*? if >=1.20.5 && <1.21.2 {*/ /*float scale,*//*?}*/ CallbackInfo ci) {
        if (/*? if <1.21.2 {*//*entity.isInWater()*//*?} else {*/state.isInWater/*?}*/) return;
        boolean pufferfish = /*? if <1.21.2 {*//*entity instanceof Pufferfish*//*?} else {*/state instanceof PufferfishRenderState/*?}*/;
        float animationTime = /*? if <1.21.2 {*//*ageInTicks*//*?} else {*/state.ageInTicks/*?}*/;
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.cos(animationTime * 0.25F) * 90.0F - (pufferfish ? 0.0F : 90.0F)));
    }
}
