package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
//?} else {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyHeadRenderState;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    //? if <1.21.2 {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void legacy$storeHeadRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyHeadRenderState.set(entity);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("RETURN"))
    private void legacy$clearHeadRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyHeadRenderState.clear();
    }
    //?} else {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"))
    private void legacy$storeHeadRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntityRenderState renderState, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyHeadRenderState.set(renderState);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("RETURN"))
    private void legacy$clearHeadRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntityRenderState renderState, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyHeadRenderState.clear();
    }
    *///?}

    //? if >=1.21.2 && <1.21.4 {
    /*@ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"), index = 6)
    private int legacy$hurtOverlayOnHeadItems(int overlay) {
        return LegacyHeadRenderState.getHeadOverlay(overlay);
    }
    *///?}

    //? if >=1.21.4 {
    /*@ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), index = 3)
    private int legacy$hurtOverlayOnHeadItems(int overlay) {
        return LegacyHeadRenderState.getHeadOverlay(overlay);
    }
    *///?}
}
