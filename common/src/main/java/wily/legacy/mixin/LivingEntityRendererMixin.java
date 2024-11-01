package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.LegacyResourceManager;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer<LivingEntity> {
    @Shadow public abstract EntityModel<LivingEntity> getModel();

    ItemRenderer itemRenderer;
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    protected void init(EntityRendererProvider.Context context, EntityModel<LivingEntity> entityModel, float f, CallbackInfo ci) {
        itemRenderer = context.getItemRenderer();
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance){
        return true;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    public void render(LivingEntity livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (livingEntity instanceof Merchant m && m.getTradingPlayer() != null && LegacyOption.merchantTradingIndicator.get()) {
            poseStack.pushPose();
            poseStack.translate(0,livingEntity.getBbHeight() + 0.5f,0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.scale(1.0f,1.0f,0.0001f);
            this.itemRenderer.renderStatic(Items.EMERALD.getDefaultInstance(), ItemDisplayContext.GROUND, i, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, livingEntity.level(), livingEntity.getId());
            poseStack.popPose();
        }
    }
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", shift = At.Shift.AFTER))
    public void renderGlowLayer(LivingEntity livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        LegacyResourceManager.GLOW_LAYERS.getUnchecked(getTextureLocation(livingEntity)).ifPresent(r-> getModel().renderToBuffer(poseStack, multiBufferSource.getBuffer(r), 15728640, OverlayTexture.NO_OVERLAY));
    }
}
