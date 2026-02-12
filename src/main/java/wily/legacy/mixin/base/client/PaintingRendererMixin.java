package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer./*? if <1.21.11 {*//*RenderType*//*?} else {*/rendertype.*/*?}*/;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PaintingRenderer.class)
public class PaintingRendererMixin {
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = /*? if <1.21.11 {*//*"Lnet/minecraft/client/renderer/RenderType;entitySolidZOffsetForward(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"*//*?} else {*/"Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolidZOffsetForward(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"/*?}*/))
    private RenderType init(/*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ resourceLocation) {
        return /*? if <1.21.11 {*//*RenderType*//*?} else {*/RenderTypes/*?}*/.entityTranslucent(resourceLocation);
    }
}
