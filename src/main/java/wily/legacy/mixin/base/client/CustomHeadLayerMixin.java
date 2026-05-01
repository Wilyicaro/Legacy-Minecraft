package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.util.client.LegacyHeadRenderState;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void legacy$storeHeadRenderState(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, LivingEntityRenderState renderState, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
            return;
        }
        if (renderState instanceof RenderStateSkinIdAccess access && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.HIDE_HEAD_LAYER, access.consoleskins$getSkinId())) {
            ci.cancel();
            return;
        }
        LegacyHeadRenderState.set(renderState);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("RETURN"))
    private void legacy$clearHeadRenderState(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, LivingEntityRenderState renderState, float headYaw, float headPitch, CallbackInfo ci) {
        LegacyHeadRenderState.clear();
    }

    @Redirect(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void hurtOverlayOnHeadItems(ItemStackRenderState itemStackRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int outlineColor, PoseStack ignoredPoseStack, SubmitNodeCollector ignoredCollector, int ignoredLight, LivingEntityRenderState renderState, float headYaw, float headPitch) {
        itemStackRenderState.submit(poseStack, submitNodeCollector, light, legacy$getOverlay(renderState), outlineColor);
    }

    @Redirect(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void hurtOverlayOnSkulls(Direction direction, float yRot, float animationPos, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, SkullModelBase skullModel, RenderType renderType, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, PoseStack ignoredPoseStack, SubmitNodeCollector ignoredCollector, int ignoredLight, LivingEntityRenderState renderState, float headYaw, float headPitch) {
        poseStack.pushPose();
        if (direction == null) poseStack.translate(0.5f, 0.0f, 0.5f);
        else poseStack.translate(0.5f - direction.getStepX() * 0.25f, 0.25f, 0.5f - direction.getStepZ() * 0.25f);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = animationPos;
        state.yRot = yRot;
        submitNodeCollector.submitModel(skullModel, state, poseStack, renderType, light, legacy$getOverlay(renderState), outlineColor, crumblingOverlay);
        poseStack.popPose();
    }

    @Unique
    private static int legacy$getOverlay(LivingEntityRenderState renderState) {
        return LivingEntityRenderer.getOverlayCoords(renderState, 0.0f);
    }
}
