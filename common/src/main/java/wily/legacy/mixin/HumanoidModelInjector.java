package wily.legacy.mixin;

import net.minecraft.client.model.AnimationUtils;
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
public abstract class HumanoidModelInjector {


    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart head;

    @Shadow @Final public ModelPart leftArm;
    float lastXRot = 0;

    @Inject(method = ("setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"), at = @At("TAIL"))
    private void setupAnim(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if (!livingEntity.hasItemInSlot(EquipmentSlot.MAINHAND) && livingEntity.isShiftKeyDown() && livingEntity.isFallFlying()) {
            (livingEntity.getMainArm() == HumanoidArm.RIGHT ? this.rightArm : leftArm).xRot = (float) (Math.PI);
            AnimationUtils.bobModelPart(this.rightArm, h + 0.2F, 1.0F);
        }
        applyEatTransform(livingEntity, InteractionHand.MAIN_HAND, livingEntity.getMainHandItem(), h, livingEntity.getMainArm());
        applyEatTransform(livingEntity, InteractionHand.OFF_HAND, livingEntity.getOffhandItem(), h, livingEntity.getMainArm());
    }

    private void applyEatTransform(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack, float partialTicks, HumanoidArm arm){
        if(isEatingWithHand(livingEntity,hand,itemStack)){
            ModelPart armModel = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
            if(livingEntity.getUseItemRemainingTicks() == 1){
                lastXRot = 0;
            }
            lastXRot = Mth.lerp(0.15f, lastXRot, 2.8f);
            armModel.xRot = Mth.lerp(0.35f,armModel.xRot, -lastXRot + (Mth.cos(partialTicks*1.5f) *0.15f));
            armModel.yRot = Mth.lerp(0.18f,armModel.yRot,  -2f);
            armModel.zRot =  Mth.lerp(0.12f,armModel.zRot, 0.6f);
        }
    }
    private boolean isEatingWithHand(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack){
        return livingEntity.getUseItemRemainingTicks() > 0 && livingEntity.getUsedItemHand() == hand && (itemStack.getUseAnimation() == UseAnim.EAT || itemStack.getUseAnimation() == UseAnim.DRINK);
    }
}
