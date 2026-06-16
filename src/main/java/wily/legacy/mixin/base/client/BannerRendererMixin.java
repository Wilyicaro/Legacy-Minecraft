package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.client.LegacyHeadRenderState;

@Mixin(BannerRenderer.class)
public class BannerRendererMixin {
    //? if <1.20.5 {
    /*@Redirect(method = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLjava/util/List;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private static void legacy$hurtTintBannerBase(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (LegacyHeadRenderState.getHeadOverlay(overlay) == overlay) part.render(poseStack, consumer, light, overlay);
        else part.render(poseStack, consumer, light, overlay, 1.0f, 0.5f, 0.5f, 1.0f);
    }

    @Redirect(method = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLjava/util/List;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;getTextureDiffuseColors()[F"))
    private static float[] legacy$hurtTintBannerPatterns(DyeColor color) {
        return LegacyHeadRenderState.getBannerTint(color.getTextureDiffuseColors());
    }
    *///?} else {
    @Redirect(method = /*? if <1.21.3 {*/"renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Z)V"/*?} else {*//*"renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;ZZ)V"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private static void legacy$hurtTintBannerBase(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        int color = LegacyHeadRenderState.getBannerTint(0xFFFFFFFF);
        if (color == 0xFFFFFFFF) part.render(poseStack, consumer, light, overlay);
        else part.render(poseStack, consumer, light, overlay, color);
    }

    @ModifyArg(method = "renderPatternLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), index = 4)
    private static int legacy$hurtTintBannerPatterns(int color) {
        return LegacyHeadRenderState.getBannerTint(color);
    }
    //?}
}
