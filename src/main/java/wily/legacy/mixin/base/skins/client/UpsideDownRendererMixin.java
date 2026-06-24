package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

@Mixin(LivingEntityRenderer.class)
public abstract class UpsideDownRendererMixin {
    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true, require = 0)
    private static void consoleskins$isEntityUpsideDown(LivingEntity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!LegacyOptions.customSkinAnimation.get()) return;
        if (!(entity instanceof Player player)) return;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, skinId)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
