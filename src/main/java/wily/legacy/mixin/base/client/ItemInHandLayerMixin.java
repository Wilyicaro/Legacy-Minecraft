package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4JClient;
//?}
//? if >=1.21.2 && <1.21.4 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
*///?}
//? if >=1.21.4 {
/*import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    //? if <1.21.2 {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && Legacy4JClient.isHostInvisible(player)) {
            ci.cancel();
        }
    }
    //?}

    //? if >=1.21.2 && <1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, LivingEntityRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }
    *///?}

    //? if >=1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$hideHeldItemsForHostInvisiblePlayers(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, ArmedEntityRenderState renderState, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }
    *///?}
}
