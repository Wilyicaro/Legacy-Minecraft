package wily.legacy.mixin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
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

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/DrownedModel;swimAmount:F", ordinal = 0), cancellable = true)
    public void setupAnim(Zombie zombie, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (LegacyOption.legacyDrownedAnimation.get()) {
            ci.cancel();
            if (swimAmount <= 0) return;
            this.rightArm.xRot = (float) Math.PI;
            this.leftArm.xRot = (float) Math.PI;
            this.rightArm.yRot = (float) (Math.PI / 8);
            this.leftArm.yRot = (float) (Math.PI / 8);

            this.rightArm.zRot = -0.2f;
            this.leftArm.zRot = 0.2f;

            AnimationUtils.bobArms(rightArm, leftArm, h);
        }
    }
}
