package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.SkinPoseRegistry;

@Mixin(PlayerModel.class)
public abstract class PlayerModelSyncArmsPoseMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$syncArmsPose(AvatarRenderState state, CallbackInfo ci) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return;

        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return;

        PlayerModel self = (PlayerModel) (Object) this;
        self.rightArm.xRot = self.leftArm.xRot;
        self.rightSleeve.xRot = self.rightArm.xRot;
    }
}
