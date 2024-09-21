package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {


    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart head;

    @Shadow @Final public ModelPart leftArm;


    @Inject(method = ("setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"), at = @At("TAIL"))
    private void setupAnim(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if (!livingEntity.hasItemInSlot(EquipmentSlot.MAINHAND) && livingEntity.isShiftKeyDown() && livingEntity.isFallFlying()) {
            (livingEntity.getMainArm() == HumanoidArm.RIGHT ? this.rightArm : leftArm).xRot = (float) (Math.PI) + (livingEntity.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f) * Mth.sin(f * 0.067F) * 0.05F;
        }
        applyEatTransform(livingEntity, InteractionHand.MAIN_HAND, livingEntity.getMainHandItem(), h, livingEntity.getMainArm());
        applyEatTransform(livingEntity, InteractionHand.OFF_HAND, livingEntity.getOffhandItem(), h, livingEntity.getMainArm().getOpposite());
    }

    private boolean applyEatTransform(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack, float bob, HumanoidArm arm){
        if(isEatingWithHand(livingEntity,hand,itemStack)){
            boolean isRightHand = arm == HumanoidArm.RIGHT;
            ModelPart armModel = isRightHand ? rightArm : leftArm;
            float r = Math.min((livingEntity.getTicksUsingItem() + (Minecraft.getInstance().isPaused() ? Minecraft.getInstance().pausePartialTick : Minecraft.getInstance().getFrameTime())) / itemStack.getUseDuration() * 6,1);
            armModel.xRot =  r * -1.4f + (r > 0.8f ? (Mth.cos(bob * 1.7f) * 0.08f) : 0);
            armModel.yRot =  (isRightHand ? -0.45f : 0.45f) * r;
            return true;
        }
        return false;
    }
    private boolean isEatingWithHand(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack){
        return livingEntity.getUseItemRemainingTicks() > 0 && livingEntity.getUsedItemHand() == hand && (itemStack.getUseAnimation() == UseAnim.EAT || itemStack.getUseAnimation() == UseAnim.DRINK);
    }
}
