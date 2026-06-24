//? if <1.21.3 {
package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = /*? if <1.21 {*//*"Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"*//*?} else {*/"Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"/*?}*/), index = 3)
    private int legacy$hurtOverlayOnElytra(int overlay, @Local(argsOnly = true) LivingEntity entity) {
        return LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
    }

    //? if <1.21 {
    /*@ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 5)
    private float legacy$hurtGreenOnElytra(float green, @Local(argsOnly = true) LivingEntity entity) {
        return LivingEntityRenderer.getOverlayCoords(entity, 0.0f) == OverlayTexture.NO_OVERLAY ? green : green * 0.5f;
    }

    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 6)
    private float legacy$hurtBlueOnElytra(float blue, @Local(argsOnly = true) LivingEntity entity) {
        return LivingEntityRenderer.getOverlayCoords(entity, 0.0f) == OverlayTexture.NO_OVERLAY ? blue : blue * 0.5f;
    }
    *///?}
}
//?}
