package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyLivingEntityRenderState;
import wily.legacy.client.LegacyNameTag;
import wily.legacy.client.LegacyOptions;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @WrapOperation(method = "submitNameTag*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V", ordinal = 1))
    private void submitNameTag(SubmitNodeCollector instance, PoseStack poseStack, Vec3 vec3, int color, Component component, boolean b, int i, double v, CameraRenderState cameraRenderState, Operation<Void> original, AvatarRenderState avatarRenderState) {
        if (!LegacyOptions.inGameOnlineIds.get()) return;
        if (LegacyOptions.displayNameTagBorder.get()) {
            Minecraft minecraft = Minecraft.getInstance();
            float[] nameTagColor = minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(FactoryRenderStateExtension.Accessor.of(avatarRenderState).getExtension(LegacyLivingEntityRenderState.class).uuid) instanceof LegacyPlayerInfo info) || info.getIdentifierIndex() == 0 ? new float[]{0, 0, 0} : Legacy4JClient.getVisualPlayerColor(info);
            LegacyNameTag.NEXT_SUBMIT.setNameTagColor(nameTagColor);
            original.call(instance, poseStack, vec3, color, component, b, i, v, cameraRenderState);
            LegacyNameTag.NEXT_SUBMIT.setNameTagColor(null);
        } else original.call(instance, poseStack, vec3, color, component, b, i, v, cameraRenderState);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void extractRenderState(Avatar avatar, AvatarRenderState avatarRenderState, float f, CallbackInfo ci) {
        if (!LegacyRenderUtil.suppressInventoryElytraPose) return;
        avatarRenderState.isFallFlying = false;
        avatarRenderState.fallFlyingTimeInTicks = 0;
        avatarRenderState.shouldApplyFlyingYRot = false;
        avatarRenderState.flyingYRot = 0;
        avatarRenderState.elytraRotX = 0.2617994F;
        avatarRenderState.elytraRotY = 0;
        avatarRenderState.elytraRotZ = -0.2617994F;
    }
}
