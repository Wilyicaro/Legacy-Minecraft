package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer {

    public PlayerRendererMixin(EntityRendererProvider.Context context, EntityModel entityModel, float f) {
        super(context, entityModel, f);
    }

    //? if <1.21.2 {
    @Redirect(method = "renderHand", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/PlayerModel;swimAmount:F", opcode = Opcodes.PUTFIELD))
    private void renderHand(PlayerModel instance, float value, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer) {
        instance.swimAmount = abstractClientPlayer.getSwimAmount(FactoryAPIClient.getGamePartialTick(false));
        ((PlayerModel)getModel()).rightArmPose = HumanoidModel.ArmPose.EMPTY;
        ((PlayerModel)getModel()).leftArmPose = HumanoidModel.ArmPose.EMPTY;
    }
    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), cancellable = true, require = 0)
    private void consoleskins$renderBoxHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo callbackInfo) {
        consoleskins$renderSkinHand(poseStack, bufferSource, packedLight, player, null, arm, sleeve, callbackInfo);
    }
    @Redirect(method = /*? if <1.20.5 {*//*"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"*//*?} else {*/"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean render(AbstractClientPlayer instance) {
        return instance.isFallFlying() && instance.getPose() == Pose.FALL_FLYING;
    }
    //?} else {
    /*@Shadow public abstract PlayerRenderState createRenderState();
    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), cancellable = true, require = 0)
    private void renderHand(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ResourceLocation resourceLocation, ModelPart modelPart, boolean bl, CallbackInfo ci) {
        PlayerRenderState state = createRenderState();
        state.swimAmount = Minecraft.getInstance().player.getSwimAmount(FactoryAPIClient.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        getModel().setupAnim(state);
        consoleskins$renderSkinHand(poseStack, multiBufferSource, i, Minecraft.getInstance().player, resourceLocation, modelPart, ci);
    }
    @Redirect(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;isFallFlying:Z"))
    private boolean render(PlayerRenderState instance) {
        return instance.isFallFlying && instance.hasPose(Pose.FALL_FLYING);
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
    private void extractRenderState(AbstractClientPlayer abstractClientPlayer, PlayerRenderState state, float f, CallbackInfo ci) {
        if (!ScreenUtil.suppressInventoryElytraPose) return;
        state.isFallFlying = false;
        state.fallFlyingTimeInTicks = 0;
        state.shouldApplyFlyingYRot = false;
        state.flyingYRot = 0;
        state.elytraRotX = 0.2617994F;
        state.elytraRotY = 0;
        state.elytraRotZ = -0.2617994F;
    }
    *///?}

    private void consoleskins$renderSkinHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, ResourceLocation fallbackTexture, ModelPart arm, ModelPart sleeve, CallbackInfo callbackInfo) {
        if (player == null || arm == null) return;
        HandSkin handSkin = consoleskins$resolveHandSkin(player, fallbackTexture, callbackInfo);
        if (handSkin == null || !(getModel() instanceof PlayerModel playerModel)) return;
        AttachSlot armSlot = consoleskins$handSlot(playerModel, arm);
        AttachSlot sleeveSlot = consoleskins$handSlot(playerModel, sleeve);
        if (armSlot == null) return;
        boolean hideArm = handSkin.built().hides(armSlot);
        boolean hideSleeve = hideArm || sleeveSlot != null && handSkin.built().hides(sleeveSlot);
        if (!hideArm && !hideSleeve) return;
        if (hideArm) {
            consoleskins$renderHandParts(arm, handSkin.built().get(armSlot), poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(handSkin.boxTexture())), packedLight, handSkin.built().partScale());
        } else {
            arm.render(poseStack, bufferSource.getBuffer(RenderType.entitySolid(handSkin.texture())), packedLight, OverlayTexture.NO_OVERLAY);
        }
        if (sleeve != null) {
            sleeve.xRot = 0.0F;
            if (hideSleeve) {
                if (sleeveSlot != null)
                    consoleskins$renderHandParts(sleeve, handSkin.built().get(sleeveSlot), poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(handSkin.boxTexture())), packedLight, handSkin.built().partScale());
            } else {
                sleeve.render(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(handSkin.texture())), packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
        callbackInfo.cancel();
    }

    private void consoleskins$renderSkinHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, ResourceLocation fallbackTexture, ModelPart modelPart, CallbackInfo callbackInfo) {
        if (player == null || modelPart == null) return;
        HandSkin handSkin = consoleskins$resolveHandSkin(player, fallbackTexture, callbackInfo);
        if (handSkin == null || !(getModel() instanceof PlayerModel playerModel)) return;
        AttachSlot slot = consoleskins$handSlot(playerModel, modelPart);
        if (slot == null || !consoleskins$hidesHandSlot(handSkin.built(), slot)) return;
        consoleskins$renderHandParts(modelPart, handSkin.built().get(slot), poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(handSkin.boxTexture())), packedLight, handSkin.built().partScale());
        callbackInfo.cancel();
    }

    private HandSkin consoleskins$resolveHandSkin(AbstractClientPlayer player, ResourceLocation fallbackTexture, CallbackInfo callbackInfo) {
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.HIDE_HAND, skinId)) {
            callbackInfo.cancel();
            return null;
        }
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
        ResourceLocation texture = resolved == null ? fallbackTexture : resolved.texture();
        if (texture == null) return null;
        BuiltBoxModel built = resolved == null ? null : resolved.boxModel();
        if (built == null) return null;
        ResourceLocation boxTexture = resolved.boxTexture() == null ? texture : resolved.boxTexture();
        return new HandSkin(texture, boxTexture, built);
    }

    private static AttachSlot consoleskins$handSlot(PlayerModel playerModel, ModelPart modelPart) {
        if (modelPart == playerModel.rightArm) return AttachSlot.RIGHT_ARM;
        if (modelPart == playerModel.leftArm) return AttachSlot.LEFT_ARM;
        if (modelPart == playerModel.rightSleeve) return AttachSlot.RIGHT_SLEEVE;
        if (modelPart == playerModel.leftSleeve) return AttachSlot.LEFT_SLEEVE;
        return null;
    }

    private static boolean consoleskins$hidesHandSlot(BuiltBoxModel built, AttachSlot slot) {
        if (built == null || slot == null) return false;
        if (built.hides(slot)) return true;
        return slot == AttachSlot.RIGHT_SLEEVE && built.hides(AttachSlot.RIGHT_ARM)
                || slot == AttachSlot.LEFT_SLEEVE && built.hides(AttachSlot.LEFT_ARM);
    }

    private static void consoleskins$renderHandParts(ModelPart arm, List<ModelPart> parts, PoseStack poseStack, VertexConsumer consumer, int packedLight, float partScale) {
        if (arm == null || parts == null || parts.isEmpty()) return;
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        if (partScale != 1.0F) poseStack.scale(partScale, partScale, partScale);
        for (ModelPart part : parts) {
            ModelPartSkipRenderOverrideAccess access = (ModelPartSkipRenderOverrideAccess) (Object) part;
            boolean previousForceRender = access.consoleskins$getForceRender();
            access.consoleskins$setForceRender(true);
            part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            access.consoleskins$setForceRender(previousForceRender);
        }
        poseStack.popPose();
    }

    private record HandSkin(ResourceLocation texture, ResourceLocation boxTexture, BuiltBoxModel built) {
    }
}
