package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyVillagerRenderState;
import wily.legacy.client.LegacyLivingEntityRenderState;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyOptions;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer<LivingEntity, LivingEntityRenderState> {
    @Shadow public abstract EntityModel<LivingEntityRenderState> getModel();


    @Shadow @Final protected ItemModelResolver itemModelResolver;

    @Unique
    ItemStack emerald = Items.EMERALD.getDefaultInstance();

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance){
        return true;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    public void render(LivingEntityRenderState livingEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class) != null && FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class).isTrading && LegacyOptions.merchantTradingIndicator.get()) {
            poseStack.pushPose();
            poseStack.translate(0, livingEntityRenderState.boundingBoxHeight + 0.5f,0);
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.scale(1.0f,1.0f,0.0001f);
            ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(itemStackRenderState, emerald, ItemDisplayContext.GROUND, Minecraft.getInstance().level, null, 0);
            itemStackRenderState.submit(poseStack, submitNodeCollector, livingEntityRenderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @ModifyReturnValue(method = "getModelTint", at = @At(value = "RETURN"))
    public int getModelTint(int color, LivingEntityRenderState livingEntityRenderState) {
        return LegacyOptions.legacyEntityFireTint.get() && livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune ? ColorUtil.colorFromFloat(ColorUtil.getRed(color),ColorUtil.getGreen(color) * getGreenFireOverlayDiff(livingEntityRenderState.ageInTicks), ColorUtil.getBlue(color)/6,ColorUtil.getAlpha(color)) : color;
    }

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void getOverlayCoords(LivingEntityRenderState livingEntityRenderState, float f, CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyEntityFireTint.get() && livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune) cir.setReturnValue(OverlayTexture.pack(0, OverlayTexture.v(true)));
    }

    @Unique
    private float getGreenFireOverlayDiff(float age){
        float range = (age / 10f) % 1f;
        return 0.6f + (range > 0.5f ? 1 - range : range) / 1.5f;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float modifyHeadRotation(float original, LivingEntity entity, LivingEntityRenderState renderState, float f){
        return LegacyOptions.headFollowsTheCamera.get() ? entity.getViewYRot(f) : original;
    }
}
