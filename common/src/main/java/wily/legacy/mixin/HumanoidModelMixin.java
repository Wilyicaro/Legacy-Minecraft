package wily.legacy.mixin;

import net.minecraft.Util;
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
public abstract class HumanoidModelMixin {


    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart head;

    @Shadow @Final public ModelPart leftArm;
    private long lastEatTime = -1;


    @Inject(method = ("setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"), at = @At("TAIL"))
    private void setupAnim(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if (!livingEntity.hasItemInSlot(EquipmentSlot.MAINHAND) && livingEntity.isShiftKeyDown() && livingEntity.isFallFlying()) {
            (livingEntity.getMainArm() == HumanoidArm.RIGHT ? this.rightArm : leftArm).xRot = (float) (Math.PI);
            AnimationUtils.bobModelPart((livingEntity.getMainArm() == HumanoidArm.RIGHT ? this.rightArm : leftArm), h + 0.2F, 1.0F);
        }
        applyEatTransform(livingEntity, InteractionHand.MAIN_HAND, livingEntity.getMainHandItem(), h, livingEntity.getMainArm());
        applyEatTransform(livingEntity, InteractionHand.OFF_HAND, livingEntity.getOffhandItem(), h, livingEntity.getMainArm());
    }

    private boolean applyEatTransform(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack, float partialTicks, HumanoidArm mainArm){
        if(isEatingWithHand(livingEntity,hand,itemStack)){
            if (livingEntity.getUseItemRemainingTicks() >= itemStack.getUseDuration(livingEntity) - 1) lastEatTime = Util.getMillis();

            boolean isRightHand = mainArm == HumanoidArm.RIGHT && hand == InteractionHand.MAIN_HAND;
            ModelPart armModel = isRightHand ? rightArm : leftArm;
            float r = Math.min(1,(Util.getMillis() - lastEatTime) / 200f);
            armModel.xRot =  r * -1.4f + (r > 0.8f ? (Mth.cos(partialTicks*1.5f) *0.15f) : 0);
            armModel.yRot =  (isRightHand ? -0.45f : 0.45f) * r;

            return true;
        }
        return false;
    }
    private boolean isEatingWithHand(LivingEntity livingEntity, InteractionHand hand, ItemStack itemStack){
        return livingEntity.getUseItemRemainingTicks() > 0 && livingEntity.getUsedItemHand() == hand && (itemStack.getUseAnimation() == UseAnim.EAT || itemStack.getUseAnimation() == UseAnim.DRINK);
    }
}
