package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
//? if <1.21.2 {
import net.minecraft.world.entity.item.PrimedTnt;
//?} else {
/*import net.minecraft.client.renderer.entity.state.TntRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyTntFlash;

@Mixin(TntRenderer.class)
public class TntRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/TntMinecartRenderer;renderWhiteSolidBlock(Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IZ)V", shift = At.Shift.AFTER))
    private void legacy$renderFlash(/*? if <1.21.2 {*/PrimedTnt entity, float yaw, float partialTick/*?} else {*//*TntRenderState state*//*?}*/, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        float fuse = /*? if <1.21.2 {*/entity.getFuse() - partialTick + 1.0F/*?} else {*//*state.fuseRemainingInTicks*//*?}*/;
        LegacyTntFlash.render(poseStack, bufferSource, fuse, true);
    }
}
