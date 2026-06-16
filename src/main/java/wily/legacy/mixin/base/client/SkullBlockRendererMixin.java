package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.client.LegacyHeadRenderState;

@Mixin(SkullBlockRenderer.class)
public class SkullBlockRendererMixin {
    @ModifyArg(method = "renderSkull", at = @At(value = "INVOKE", target = /*? if <1.21 {*//*"Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"*//*?} else {*/"Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"/*?}*/), index = 3)
    private static int legacy$hurtOverlayOnSkulls(int overlay) {
        return LegacyHeadRenderState.getHeadOverlay(overlay);
    }
}
