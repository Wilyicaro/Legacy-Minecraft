package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyVillagerRenderState;
import wily.legacy.client.LegacyLivingEntityRenderState;
*///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyOptions;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer<LivingEntity/*? if >=1.21.2 {*//*, LivingEntityRenderState*//*?}*/> {
    @Shadow public abstract EntityModel</*? if <1.21.2 {*/LivingEntity/*?} else {*//*LivingEntityRenderState*//*?}*/> getModel();


    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }


    /*? if <1.21.2 || >=1.21.4 {*/@Unique/*?} else {*//*@Shadow @Final protected*//*?}*/ ItemRenderer itemRenderer;
    //? if <1.21.2 || >=1.21.4 {
    @Inject(method = "<init>", at = @At("RETURN"))
    protected void init(EntityRendererProvider.Context context, EntityModel</*? if <1.21.2 {*/LivingEntity/*?} else {*//*LivingEntityRenderState*//*?}*/> entityModel, float f, CallbackInfo ci) {
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }
    //?}

    @Redirect(method = /*? if <1.21.2 {*/"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"/*?} else {*//*"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance){
        return true;
    }

    @Inject(method = /*? if <1.21.2 {*/"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"/*?} else {*//*"render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"*//*?}*/, at = @At("HEAD"))
    public void render(/*? if <1.21.2 {*/LivingEntity livingEntity, float f, float g/*?} else {*//*LivingEntityRenderState livingEntityRenderState*//*?}*/, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (/*? if <1.21.2 {*/livingEntity instanceof Merchant m && m.getTradingPlayer() != null/*?} else {*//*FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class) != null && FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class).isTrading*//*?}*/ && LegacyOptions.merchantTradingIndicator.get()) {
            poseStack.pushPose();
            poseStack.translate(0,/*? if <1.21.2 {*/livingEntity.getBbHeight()/*?} else {*//*livingEntityRenderState.boundingBoxHeight*//*?}*/ + 0.5f,0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.scale(1.0f,1.0f,0.0001f);
            this.itemRenderer.renderStatic(Items.EMERALD.getDefaultInstance(), ItemDisplayContext.GROUND, i, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, Minecraft.getInstance().level, 0);
            poseStack.popPose();
        }
    }
    //? if <1.21 {
    /*@Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    public void renderToBufferFireColor(EntityModel instance, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a, LivingEntity livingEntity, float f, float partialTick){
        boolean fireTint = LegacyOptions.legacyEntityFireTint.get() && livingEntity.isOnFire() && livingEntity.displayFireAnimation();
        instance.renderToBuffer(poseStack,consumer,light, overlay, r, fireTint ? g * getGreenFireOverlayDiff(livingEntity.tickCount +partialTick) : g, fireTint ? b/6 : b, a);
    }
    *///?} else {
    @Redirect(method = /*? if <1.21.2 {*/"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"/*?} else {*//*"render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    public void renderToBufferFireColor(EntityModel instance, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, /*? if <=1.20.6 {*//*float r, float g, float b, float a*//*?} else {*/ int color/*?}*/, /*? if <1.21.2 {*/LivingEntity livingEntity, float f, float g/*?} else {*//*LivingEntityRenderState livingEntityRenderState*//*?}*/){
        instance.renderToBuffer(poseStack,consumer,light, overlay, LegacyOptions.legacyEntityFireTint.get() && /*? if <1.21.2 {*/livingEntity.isOnFire() && livingEntity.displayFireAnimation() && !livingEntity.fireImmune()/*?} else {*//*livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune*//*?}*/ ? ColorUtil.colorFromFloat(ColorUtil.getRed(color),ColorUtil.getGreen(color) * getGreenFireOverlayDiff(/*? if <1.21.2 {*/livingEntity.tickCount + g/*?} else {*//*livingEntityRenderState.ageInTicks*//*?}*/), ColorUtil.getBlue(color)/6,ColorUtil.getAlpha(color)) : color);
    }
    //?}

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void getOverlayCoords(/*? if <1.21.2 {*/LivingEntity livingEntity/*?} else {*//*LivingEntityRenderState livingEntityRenderState*//*?}*/, float f, CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyEntityFireTint.get() && /*? if <1.21.2 {*/livingEntity.isOnFire() && livingEntity.displayFireAnimation() && !livingEntity.fireImmune()/*?} else {*//*livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune*//*?}*/) cir.setReturnValue(OverlayTexture.pack(0, OverlayTexture.v(true)));
    }

    @Unique
    private float getGreenFireOverlayDiff(float age){
        float range = (age / 10f) % 1f;
        return 0.6f + (range > 0.5f ? 1 - range : range) / 1.5f;
    }

    //? if <1.21.2 {
    @ModifyExpressionValue(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 1))
    private float modifyHeadRotation(float original, LivingEntity entity, float f, float g){
        return LegacyOptions.headFollowsTheCamera.get() ? entity.getViewYRot(g) : original;
    }
    //?} else {
    /*@ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float modifyHeadRotation(float original, LivingEntity entity, LivingEntityRenderState renderState, float f){
        return LegacyOptions.headFollowsTheCamera.get() ? entity.getViewYRot(f) : original;
    }
    *///?}
}
