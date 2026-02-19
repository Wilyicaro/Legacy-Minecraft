package wily.legacy.mixin.base.cpm.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;
import wily.legacy.Skins.client.render.StiffArmsPose;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;

@Mixin(value = PlayerModel.class, priority = 999999)
public class PlayerModelMixin {
    @Inject(at = @At("RETURN"), method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V")
    public void onSetupAnim(AvatarRenderState state, CallbackInfo cbi) {
        ((PlayerRenderStateAccess) state).cpm$loadState((PlayerModel) (Object) this);
    }
}
