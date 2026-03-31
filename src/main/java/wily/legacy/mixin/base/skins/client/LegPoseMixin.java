package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.pose.SyncLegsPose;
import wily.legacy.client.LegacyOptions;

@Mixin(PlayerModel.class)
public abstract class LegPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$syncLegsPose(AvatarRenderState state, CallbackInfo ci) {
        if (!LegacyOptions.customSkinAnimation.get()) return;
        if (!SyncLegsPose.shouldApply(state)) return;
        SyncLegsPose.apply((PlayerModel) (Object) this);
    }
}
