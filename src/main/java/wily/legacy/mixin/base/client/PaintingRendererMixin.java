package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PaintingRenderer.class)
public class PaintingRendererMixin {
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolidZOffsetForward(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType init(Identifier resourceLocation) {
        return RenderType.entityTranslucent(resourceLocation);
    }
}
