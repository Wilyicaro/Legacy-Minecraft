package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShulkerBulletRenderer.class)
public class ShulkerBulletRendererMixin {
    //? if <1.21 {
    /*@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ShulkerBulletModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", ordinal = 1), index = 7)
    private float legacy$outerGlowAlpha(float alpha) {
        return 0.5F;
    }
    *///?} else {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ShulkerBulletModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), index = 4)
    private int legacy$outerGlowColor(int color) {
        return 0x80FFFFFF;
    }
    //?}
}
