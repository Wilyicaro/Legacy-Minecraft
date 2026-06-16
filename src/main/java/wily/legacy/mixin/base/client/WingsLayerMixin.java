//? if >=1.21.3 {
/*package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyHeadRenderState;

@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"))
    private void legacy$storeWingsRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, HumanoidRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyHeadRenderState.setWings(renderState);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("RETURN"))
    private void legacy$clearWingsRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, HumanoidRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyHeadRenderState.clearWings();
    }
}
*///?}
