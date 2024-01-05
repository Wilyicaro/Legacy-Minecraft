package wily.legacy.mixin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerModelInjector {

    @Shadow public HumanoidModel.ArmPose rightArmPose;

    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart head;
    @Shadow public boolean crouching;
    @Inject(method = ("setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"), at = @At("HEAD"))
    private void setupAnimHead(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if (this.rightArmPose != HumanoidModel.ArmPose.SPYGLASS && livingEntity.isCrouching() && livingEntity.isFallFlying()) {
            crouching = false;
        }
    }
    @Inject(method = ("setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"), at = @At("TAIL"))
    private void setupAnim(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if (this.rightArmPose != HumanoidModel.ArmPose.SPYGLASS && livingEntity.isCrouching() && livingEntity.isFallFlying()) {
            this.rightArm.xRot = Mth.clamp(this.head.xRot - 2.5F, -2.4F, 2.0F);
            AnimationUtils.bobModelPart(this.rightArm, h + 0.2F, 1.0F);
        }
    }
}
