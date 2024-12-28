package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.DrownedRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
 //?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;

@Mixin(DrownedRenderer.class)
public class DrownedRendererMixin {
    @Inject(method = /*? if <1.20.5 {*//*"setupRotations(Lnet/minecraft/world/entity/monster/Drowned;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"*//*?} else if <1.21.2 {*//*"setupRotations(Lnet/minecraft/world/entity/monster/Drowned;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"*//*?} else {*/"setupRotations(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V"/*?}*/, at = @At(value = "INVOKE", target = /*? if <=1.20.5 {*//*"Lnet/minecraft/client/renderer/entity/AbstractZombieRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"*//*?} else if <1.21.2 {*//*"Lnet/minecraft/client/renderer/entity/AbstractZombieRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"*//*?} else {*/"Lnet/minecraft/client/renderer/entity/AbstractZombieRenderer;setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V"/*?}*/, shift = At.Shift.AFTER), cancellable = true)
    protected void setupRotations(/*? if <1.21.2 {*//*Drowned drowned*//*?} else {*/ZombieRenderState renderState/*?}*/, PoseStack poseStack, float f, float g, /*? if <1.21.2 {*//*float h,*//*?}*/ /*? if >=1.20.5 && <1.21.2 {*/ /*float i,*//*?}*/ CallbackInfo ci) {
        if (!LegacyOption.legacyDrownedAnimation.get()) return;
        ci.cancel();
        float j = /*? if <1.21.2 {*//*drowned.getSwimAmount(h)*//*?} else {*/renderState.swimAmount/*?}*/;
        if (j <= 0) return;
        float k = /*? if <1.21.2 {*//*drowned.getViewXRot(h)*//*?} else {*/renderState.xRot/*?}*/;;
        float l = /*? if <1.21.2 {*//*drowned.isInWater()*//*?} else {*/renderState.isInWater/*?}*/ ? -90.0F - k : -90.0F;
        float m = Mth.lerp(j, 0.0F, l);
        poseStack.mulPose(Axis.XP.rotationDegrees(m));
        if (/*? if <1.21.2 {*//*drowned.isVisuallySwimming()*//*?} else {*/renderState.isVisuallySwimming/*?}*/) {
            poseStack.translate(0.0F, -1.0F, 0.3F);
        }
    }
}
