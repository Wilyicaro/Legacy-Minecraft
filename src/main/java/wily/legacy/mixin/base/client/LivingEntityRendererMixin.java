package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
    @Shadow
    @Final
    protected ItemModelResolver itemModelResolver;
    @Unique
    private ItemStack legacy$emerald;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void getOverlayCoords(LivingEntityRenderState livingEntityRenderState, float f, CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyEntityFireTint.get() && livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune)
            cir.setReturnValue(OverlayTexture.pack(0, OverlayTexture.v(true)));
    }

    @Shadow
    public abstract EntityModel<LivingEntityRenderState> getModel();

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance) {
        return true;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    public void render(LivingEntityRenderState livingEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null && minecraft.getCameraEntity() == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON && minecraft.player.isSleeping() && livingEntityRenderState instanceof AvatarRenderState avatarRenderState && avatarRenderState.id == minecraft.player.getId()) {
            ci.cancel();
            return;
        }
        if (FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class) != null && FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyVillagerRenderState.class).isTrading && LegacyOptions.merchantTradingIndicator.get()) {
            poseStack.pushPose();
            poseStack.translate(0, livingEntityRenderState.boundingBoxHeight + 0.5f, 0);
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.scale(1.0f, 1.0f, 0.0001f);
            ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(itemStackRenderState, legacy$emerald(), ItemDisplayContext.GROUND, Minecraft.getInstance().level, null, 0);
            itemStackRenderState.submit(poseStack, submitNodeCollector, livingEntityRenderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @ModifyReturnValue(method = "getModelTint", at = @At(value = "RETURN"))
    public int getModelTint(int color, LivingEntityRenderState livingEntityRenderState) {
        return LegacyOptions.legacyEntityFireTint.get() && livingEntityRenderState.displayFireAnimation && !FactoryRenderStateExtension.Accessor.of(livingEntityRenderState).getExtension(LegacyLivingEntityRenderState.class).fireImmune ? ColorUtil.colorFromFloat(ColorUtil.getRed(color), ColorUtil.getGreen(color) * getGreenFireOverlayDiff(livingEntityRenderState.ageInTicks), ColorUtil.getBlue(color) / 6, ColorUtil.getAlpha(color)) : color;
    }

    @Unique
    private float getGreenFireOverlayDiff(float age) {
        float range = (age / 10f) % 1f;
        return 0.6f + (range > 0.5f ? 1 - range : range) / 1.5f;
    }

    @Unique
    private ItemStack legacy$emerald() {
        if (legacy$emerald == null) legacy$emerald = Items.EMERALD.getDefaultInstance();
        return legacy$emerald;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float modifyHeadRotation(float original, LivingEntity entity, LivingEntityRenderState renderState, float f) {
        return LegacyOptions.headFollowsTheCamera.get() ? entity.getViewYRot(f) : original;
    }
}
