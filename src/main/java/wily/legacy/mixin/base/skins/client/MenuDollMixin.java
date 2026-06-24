package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
//?} else {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.pose.ArmPoseSupport;
import wily.legacy.skins.pose.MenuDollPose;

@Mixin(PlayerModel.class)
public abstract class MenuDollMixin {
    //? if <1.21.2 {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void consoleskins$applySkinPoses(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        PlayerModel self = (PlayerModel) (Object) this;
        MenuDollPose.applySkinPoses(self, ArmPoseSupport.entityState(entity, ageInTicks, self.attackTime));
    }
    //?} else {
    /*@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$applySkinPoses(PlayerRenderState state, CallbackInfo callbackInfo) {
        MenuDollPose.applySkinPoses((PlayerModel) (Object) this, state);
    }
    *///?}
}
