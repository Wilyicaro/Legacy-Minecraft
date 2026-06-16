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
//? if <1.21 {
/*import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
*///?}

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    //? if <1.21 {
    /*@ModifyArgs(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void legacy$hurtTintOnElytra(Args args, @Local(argsOnly = true) LivingEntity entity) {
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
        args.set(3, overlay);
        if (overlay == OverlayTexture.NO_OVERLAY) return;
        args.set(5, (float) args.get(5) * 0.5f);
        args.set(6, (float) args.get(6) * 0.5f);
    }
    *///?} else {
    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = /*? if <1.21 {*//*"Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"*//*?} else {*/"Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"/*?}*/), index = 3)
    private int legacy$hurtOverlayOnElytra(int overlay, @Local(argsOnly = true) LivingEntity entity) {
        return LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
    }
    //?}
}
//?}
