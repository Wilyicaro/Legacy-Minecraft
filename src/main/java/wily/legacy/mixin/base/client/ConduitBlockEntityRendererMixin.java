package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.ConduitRotationCache;

@Mixin(ConduitRenderer.class)
public class ConduitBlockEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/CondiutRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void render(CondiutRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        poseStack.pushPose();
        poseStack.translate(0.5F, state.isActive ? -0.1125F : -0.3125F, 0.5F);
        ClientLevel level = Minecraft.getInstance().level;
        Integer rotation = level == null ? null : ConduitRotationCache.get(level, state.blockPos);
        if (rotation != null) poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * 22.5F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/CondiutRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    private void renderReturn(CondiutRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        poseStack.popPose();
    }
}
