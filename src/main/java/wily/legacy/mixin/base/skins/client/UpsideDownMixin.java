package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

@Mixin(AvatarRenderer.class)
public abstract class UpsideDownMixin {
    @Inject(method = "isEntityUpsideDown(Lnet/minecraft/world/entity/Avatar;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void consoleskins$isEntityUpsideDown(Avatar avatar, CallbackInfoReturnable<Boolean> cir) {
        if (!LegacyOptions.customSkinAnimation.get()) return;
        if (!(avatar instanceof Player player)) return;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, skinId)) cir.setReturnValue(true);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), require = 0)
    private void consoleskins$setupFallFlyingRotations(AvatarRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (state.id != GuiDollRender.MENU_DOLL_ID && state.isFallFlying && consoleskins$isUpsideDownSkin(state)) {
            state.isUpsideDown = true;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"), require = 0)
    private void consoleskins$setupRotations(AvatarRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (!consoleskins$isUpsideDownSkin(state)) return;
        if (state.id == GuiDollRender.MENU_DOLL_ID) {
            poseStack.translate(0.0F, 2.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            return;
        }
        if (state.isUpsideDown) return;
        if (state.isFallFlying) return;
        poseStack.translate(0.0F, state.boundingBoxHeight + 0.1F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }

    private static boolean consoleskins$isUpsideDownSkin(AvatarRenderState state) {
        if (!LegacyOptions.customSkinAnimation.get()) return false;
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        return SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, a.consoleskins$getSkinId());
    }
}
