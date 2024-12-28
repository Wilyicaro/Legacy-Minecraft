package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
//? if <1.21.2 {
/*import net.minecraft.world.item.UseAnim;
*///?} else {
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyHumanoidRenderState;
//?}
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;import wily.legacy.Legacy4JClient;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {


    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart leftArm;


    @Inject(method = /*? if <1.21.2 {*//*"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"*//*?} else {*/"setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"/*?}*/, at = @At("TAIL"))
    private void setupAnim(/*? if <1.21.2 {*/ /*LivingEntity livingEntity, float f, float g, float h, float i, float j*//*?} else {*/ HumanoidRenderState humanoidRenderState/*?}*/, CallbackInfo info){
        HumanoidArm mainArm = /*? if <1.21.2 {*//*livingEntity.getMainArm()*//*?} else {*/ humanoidRenderState.mainArm/*?}*/;
        float ageInTicks = /*? if <1.21.2 {*//*h*//*?} else {*/humanoidRenderState.ageInTicks/*?}*/;
        if (!/*? if <1.21.2 {*//*livingEntity.hasItemInSlot(EquipmentSlot.MAINHAND)*//*?} else {*/ humanoidRenderState.getMainHandItem().isEmpty()/*?}*/ && /*? if <1.21.2 {*//*livingEntity.isShiftKeyDown()*//*?} else {*/humanoidRenderState.isDiscrete/*?}*/ && /*? if <1.21.2 {*//*livingEntity.isFallFlying()*//*?} else {*/humanoidRenderState.isFallFlying/*?}*/) {
            (mainArm == HumanoidArm.RIGHT ? this.rightArm : leftArm).xRot = (float) (Math.PI) + (mainArm == HumanoidArm.RIGHT ? 1.0f : -1.0f) * Mth.sin(ageInTicks * 0.067F) * 0.05F;
        }
        HumanoidArm useArm = /*? if <1.21.2 {*//*livingEntity.getUsedItemHand()*//*?} else {*/ humanoidRenderState.useItemHand/*?}*/ == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        //? if <1.21.2
        /*ItemStack useItem = livingEntity.getUseItem();*/
        var useAnim = /*? if <1.21.2 {*//*useItem.getUseAnimation()*//*?} else {*/ FactoryRenderStateExtension.Accessor.of(humanoidRenderState).getExtension(LegacyHumanoidRenderState.class).useAnim/*?}*/;
        if(/*? if <1.21.2 {*//*livingEntity.getUseItemRemainingTicks() > 0*//*?} else {*/humanoidRenderState.isUsingItem/*?}*/ &&  (useAnim == /*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.EAT || useAnim == /*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.DRINK)){
            boolean isRightHand = useArm == HumanoidArm.RIGHT;
            ModelPart armModel = isRightHand ? rightArm : leftArm;
            float r = Math.min((/*? if <1.21.2 {*//*livingEntity.getTicksUsingItem()*//*?} else {*/humanoidRenderState.ticksUsingItem/*?}*/ + FactoryAPIClient.getGamePartialTick(/*? if <=1.20.2 {*//*false*//*?} else if <1.21.2 {*//*Minecraft.getInstance().level.tickRateManager().isEntityFrozen(livingEntity)*//*?} else {*/ humanoidRenderState.isFullyFrozen/*?}*/)) / /*? if <1.21.2 {*//*useItem.getUseDuration(/^? if >=1.20.5 {^/livingEntity/^?}^/)*//*?} else {*/FactoryRenderStateExtension.Accessor.of(humanoidRenderState).getExtension(LegacyHumanoidRenderState.class).itemUseDuration/*?}*/ * 6,1);
            armModel.xRot =  r * -1.4f + (r > 0.8f ? (Mth.cos(ageInTicks * 1.7f) * 0.08f) : 0);
            armModel.yRot = (isRightHand ? -0.45f : 0.45f) * r;
        }
    }
    @Redirect(method = /*? if <1.21.2 {*//*"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"*//*?} else {*/"setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;xRot:F", opcode = Opcodes.PUTFIELD, ordinal = /*? if <1.21.2 {*//*0*//*?} else {*/1/*?}*/))
    public void setupAnim(ModelPart instance, float value, /*? if <1.21.2 {*/ /*LivingEntity livingEntity, float f, float g, float h, float i, float j*//*?} else {*/ HumanoidRenderState humanoidRenderState/*?}*/) {
        //? if <1.21.2 {
        /*instance.xRot = livingEntity.getPose() == Pose.FALL_FLYING ? value : j * (float) (Math.PI / 180.0);
        *///?} else {
        if (humanoidRenderState.hasPose(Pose.FALL_FLYING)) instance.xRot = value;
        //?}
    }
}
