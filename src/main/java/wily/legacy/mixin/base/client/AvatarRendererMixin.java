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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySubmitNodeCollector;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @WrapOperation(method = "submitNameTag*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V", ordinal = 1))
    private void submitNameTag(SubmitNodeCollector instance, PoseStack poseStack, Vec3 vec3, int color, Component component, boolean b, int i, double v, CameraRenderState cameraRenderState, Operation<Void> original, AvatarRenderState avatarRenderState) {
        if (LegacyOptions.displayNameTagBorder.get()) {
            Minecraft minecraft = Minecraft.getInstance();
            float[] nameTagColor = minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(avatarRenderState.nameTag.getString()) instanceof LegacyPlayerInfo info) || info.getIdentifierIndex() == 0 ?  new float[]{0,0,0} : Legacy4JClient.getVisualPlayerColor(info);
            ((LegacySubmitNodeCollector)instance).submitLegacyNameTag(poseStack, vec3, color, component, b, i, v, cameraRenderState, nameTagColor);
        } else original.call(instance, poseStack, vec3, color, component, b, i, v, cameraRenderState);
    }
}
