package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyNameTag;
import wily.legacy.client.LegacyOptions;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @WrapOperation(method = "submitNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V"))
    private void submitNameTag(SubmitNodeCollector instance, PoseStack poseStack, Vec3 vec3, int color, Component component, boolean b, int i, double v, CameraRenderState cameraRenderState, Operation<Void> original) {
        if (LegacyOptions.displayNameTagBorder.get()) {
            LegacyNameTag.NEXT_SUBMIT.setNameTagColor(new float[]{0, 0, 0});
            original.call(instance, poseStack, vec3, color, component, b, i, v, cameraRenderState);
            LegacyNameTag.NEXT_SUBMIT.setNameTagColor(null);
        } else original.call(instance, poseStack, vec3, color, component, b, i, v, cameraRenderState);
    }
}
