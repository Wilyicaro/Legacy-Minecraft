package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinIdUtil;

@Mixin(LivingEntityRenderer.class)
public abstract class UpsideDownRendererMixin {
    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void consoleskins$isEntityUpsideDown(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!ConsoleSkinsClientSettings.isSkinAnimations()) return;
        if (!(entity instanceof Player p)) return;
        String skinId = ClientSkinCache.get(p.getUUID());
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, skinId)) { cir.setReturnValue(true); }
    }
}
