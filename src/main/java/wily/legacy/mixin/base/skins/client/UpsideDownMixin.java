package wily.legacy.mixin.base.skins.client;

//? if >=1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.pose.SkinPoseRegistry;

@Mixin(PlayerRenderer.class)
public abstract class UpsideDownMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), require = 0)
    private void consoleskins$setupFallFlyingRotations(PlayerRenderState state, PoseStack poseStack, float yRot, float xRot, CallbackInfo callbackInfo) {
        if (state.id != GuiDollRender.MENU_DOLL_ID && state.isFallFlying && consoleskins$isUpsideDownSkin(state)) {
            state.isUpsideDown = true;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"), require = 0)
    private void consoleskins$setupRotations(PlayerRenderState state, PoseStack poseStack, float yRot, float xRot, CallbackInfo callbackInfo) {
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

    private static boolean consoleskins$isUpsideDownSkin(PlayerRenderState state) {
        if (!LegacyOptions.customSkinAnimation.get()) return false;
        if (!(state instanceof RenderStateSkinIdAccess access)) return false;
        return SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, access.consoleskins$getSkinId());
    }
}
*///?}
