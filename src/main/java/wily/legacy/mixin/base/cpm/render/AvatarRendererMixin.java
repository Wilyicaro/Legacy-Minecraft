package wily.legacy.mixin.base.cpm.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.CustomModelSkins.cpm.client.CPMOrderedSubmitNodeCollector.CPMSubmitNodeCollector;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.client.ModelTexture;
import wily.legacy.CustomModelSkins.cpm.client.PlayerProfile;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine.AnimationMode;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.model.TextureSheetType;
import wily.legacy.CustomModelSkins.cpm.shared.util.Msg;
import wily.legacy.CustomModelSkins.cpm.util.IdUtil;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    private static @Unique
    final ResourceLocation CPM$EMPTY_TEX = IdUtil.parse("cpm:textures/template/empty.png");

    public AvatarRendererMixin(Context context, PlayerModel entityModel, float f) {
        super(context, entityModel, f);
    }

    @Inject(at = @At("RETURN"), method = {"getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/ResourceLocation;"}, cancellable = true)
    public void onGetEntityTexture(AvatarRenderState entity, CallbackInfoReturnable<ResourceLocation> cbi) {
        if (CustomPlayerModelsClient.mc == null) return;
        CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(getModel(), new ModelTexture(cbi), TextureSheetType.SKIN);
    }

    @Inject(at = @At("HEAD"), method = "submitNameTag", cancellable = true)
    public void onSubmitNameTag(AvatarRenderState avatarRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo cbi) {
        if (!Player.isEnableNames()) {
            cbi.cancel();
            return;
        }
        if (Player.isEnableLoadingInfo()) {
            PlayerRenderStateAccess sa = (PlayerRenderStateAccess) avatarRenderState;
            if (sa.cpm$getModelStatus() != null) {
                poseStack.pushPose();
                poseStack.translate(0.0D, 1.3F, 0.0D);
                poseStack.scale(0.5f, 0.5f, 0.5f);
                submitNodeCollector.submitNameTag(poseStack, avatarRenderState.nameTagAttachment, 0, sa.cpm$getModelStatus(), !avatarRenderState.isDiscrete, avatarRenderState.lightCoords, avatarRenderState.distanceToCameraSq, cameraRenderState);
                poseStack.popPose();
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "extractRenderState")
    public void onExtractRenderState(final Avatar abstractClientPlayer, final AvatarRenderState playerRenderState, final float f, CallbackInfo cbi) {
        if (CustomPlayerModelsClient.INSTANCE == null) return;
        PlayerRenderStateAccess sa = (PlayerRenderStateAccess) playerRenderState;
        GameProfile profile = PlayerProfile.getPlayerProfile(abstractClientPlayer);
        if (profile == null) return;
        String unique;
        if (abstractClientPlayer instanceof AbstractClientPlayer) {
            unique = ModelDefinitionLoader.PLAYER_UNIQUE;
        } else if (abstractClientPlayer instanceof ClientMannequin) {
            unique = "man:" + abstractClientPlayer.getStringUUID();
        } else {
            return;
        }
        Msg st = CustomPlayerModelsClient.INSTANCE.manager.getStatus(profile, unique);
        sa.cpm$setModelStatus(st != null ? st.toComponent() : null);
        var pl = CustomPlayerModelsClient.INSTANCE.manager.loadPlayerState(profile, abstractClientPlayer, unique, AnimationMode.PLAYER);
        sa.cpm$setPlayer((Player) pl);
        if (pl != null) {
            ((PlayerProfile) pl).updateFromState(playerRenderState);
        }
    }

    @Inject(at = @At("HEAD"), method = "renderRightHand")
    public void onRenderRightArmPre(final PoseStack poseStack, final SubmitNodeCollector vertexConsumers, final int i, final ResourceLocation skinTexIn, final boolean sleeve, CallbackInfo cbi, @Local LocalRef<SubmitNodeCollector> snc) {
        CPMSubmitNodeCollector.injectSNC(snc);
        if (CustomPlayerModelsClient.INSTANCE == null) return;
        try {
            CustomPlayerModelsClient.INSTANCE.renderHand(getModel());
            ResourceLocation texIn = skinTexIn != null ? skinTexIn : CPM$EMPTY_TEX;
            ModelTexture tex = new ModelTexture(texIn);
            CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(getModel(), tex, TextureSheetType.SKIN);
        } catch (Throwable ignored) {
        }
    }

    @Inject(at = @At("HEAD"), method = "renderLeftHand")
    public void onRenderLeftArmPre(final PoseStack poseStack, final SubmitNodeCollector vertexConsumers, final int i, final ResourceLocation skinTexIn, final boolean sleeve, CallbackInfo cbi, @Local LocalRef<SubmitNodeCollector> snc) {
        CPMSubmitNodeCollector.injectSNC(snc);
        if (CustomPlayerModelsClient.INSTANCE == null) return;
        try {
            CustomPlayerModelsClient.INSTANCE.renderHand(getModel());
            ResourceLocation texIn = skinTexIn != null ? skinTexIn : CPM$EMPTY_TEX;
            ModelTexture tex = new ModelTexture(texIn);
            CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(getModel(), tex, TextureSheetType.SKIN);
        } catch (Throwable ignored) {
        }
    }

    @Inject(at = @At("RETURN"), method = "renderRightHand")
    public void onRenderRightArmPost(final PoseStack poseStack, final SubmitNodeCollector vertexConsumers, final int i, final ResourceLocation ResourceLocation, final boolean sleeve, CallbackInfo cbi) {
        if (CustomPlayerModelsClient.INSTANCE == null) return;
        try {
            CustomPlayerModelsClient.INSTANCE.renderHandPost(getModel());
        } catch (Throwable ignored) {
        }
    }

    @Inject(at = @At("RETURN"), method = "renderLeftHand")
    public void onRenderLeftArmPost(final PoseStack poseStack, final SubmitNodeCollector vertexConsumers, final int i, final ResourceLocation ResourceLocation, final boolean sleeve, CallbackInfo cbi) {
        if (CustomPlayerModelsClient.INSTANCE == null) return;
        try {
            CustomPlayerModelsClient.INSTANCE.renderHandPost(getModel());
        } catch (Throwable ignored) {
        }
    }

    @Inject(at = @At("HEAD"), method = "submit", remap = false, require = 0)
    public void cpm$onSubmitPre(final AvatarRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState cameraRenderState, final CallbackInfo ci, @Local(argsOnly = true) LocalRef<SubmitNodeCollector> snc) {
        CPMSubmitNodeCollector.injectSNC(snc);
        try {
            if (CustomPlayerModelsClient.INSTANCE == null) return;
            CustomPlayerModelsClient.INSTANCE.playerRenderPre((PlayerRenderStateAccess) state, getModel(), state);
        } catch (Throwable ignored) {
        }
    }

    @Inject(at = @At("RETURN"), method = "submit", remap = false, require = 0)
    public void cpm$onSubmitPost(final AvatarRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState cameraRenderState, final CallbackInfo ci) {
        try {
            CustomPlayerModelsClient.INSTANCE.playerRenderPost(getModel());
        } catch (Throwable ignored) {
        }
    }

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(" + "Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"), method = "renderHand")
    public ResourceLocation getSkinTex(ResourceLocation arg) {
        try {
            if (arg == null) arg = CPM$EMPTY_TEX;
            if (CustomPlayerModelsClient.INSTANCE == null) return arg;
            if (CustomPlayerModelsClient.mc == null) return arg;
            ModelTexture tex = new ModelTexture(arg);
            CustomPlayerModelsClient.mc.getPlayerRenderManager().bindSkin(getModel(), tex, TextureSheetType.SKIN);
            return tex.getTexture();
        } catch (Throwable t) {
            return arg;
        }
    }
}
