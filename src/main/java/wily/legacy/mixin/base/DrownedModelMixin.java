package wily.legacy.mixin.base;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;

@Mixin(DrownedModel.class)
public class DrownedModelMixin extends ZombieModel {
    public DrownedModelMixin(ModelPart modelPart) {
        super(modelPart);
    }

    @Inject(method = /*? if <1.21.2 {*//*"setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V"*//*?} else {*/"setupAnim(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;)V"/*?}*/, at = @At(value = "FIELD", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/model/DrownedModel;swimAmount:F"*//*?} else {*/"Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;swimAmount:F"/*?}*/, ordinal = 0), cancellable = true)
    public void setupAnim(/*? if >=1.21.2 {*/ZombieRenderState renderState/*?} else {*//*Zombie zombie, float f, float g, float h, float i, float j*//*?}*/, CallbackInfo ci) {
        if (LegacyOption.legacyDrownedAnimation.get()) {
            ci.cancel();
            if (/*? if >=1.21.2 {*/renderState./*?}*/ swimAmount <= 0) return;
            //? if >=1.21.2 {
            float attackTime = renderState.attackTime;
            //?}
            float a = Mth.sin(attackTime * Mth.PI);
            float t = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * Mth.PI);

            this.rightArm.xRot = (float) Math.PI - a * 1.2F + t * 0.4F;
            this.leftArm.xRot = (float) Math.PI - a * 1.2F + t * 0.4F;
            this.rightArm.yRot = (float) (Math.PI / 8);
            this.leftArm.yRot = (float) (Math.PI / 8);

            this.rightArm.zRot = -0.2f;
            this.leftArm.zRot = 0.2f;
            AnimationUtils.bobArms(rightArm, leftArm, /*? if >=1.21.2 {*/renderState.ageInTicks/*?} else {*//*h*//*?}*/);
        }
    }
}
