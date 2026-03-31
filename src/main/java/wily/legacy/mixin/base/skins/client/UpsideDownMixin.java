package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.gui.GuiDollRender;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.client.LegacyOptions;

@Mixin(AvatarRenderer.class)
public abstract class UpsideDownMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"), require = 0)
    private void consoleskins$setupRotations(AvatarRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (!LegacyOptions.customSkinAnimation.get()) return;
        if (!(state instanceof RenderStateSkinIdAccess a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, skinId)) return;
        if (state.id == GuiDollRender.MENU_DOLL_ID) {
            poseStack.translate(0.0F, 2.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            return;
        }
        poseStack.translate(0.0F, state.boundingBoxHeight + 0.1F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }
}
