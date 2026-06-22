package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
//? if >=1.21.5 {
/*import net.minecraft.world.phys.Vec3;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.ConduitRotationCache;

@Mixin(ConduitRenderer.class)
public class ConduitBlockEntityRendererMixin {
    @Unique
    private boolean legacy$renderingPlacedConduit;

    @Inject(method = /*? if <1.21.5 {*/"render(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"/*?} else {*//*"render(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V"*//*?}*/, at = @At("HEAD"))
    private void render(ConduitBlockEntity conduit, float tickDelta, PoseStack poseStack, MultiBufferSource source, int light, int overlay, /*? if >=1.21.5 {*//*Vec3 cameraPos, *//*?}*/ CallbackInfo ci) {
        if (!(conduit.getLevel() instanceof ClientLevel level)) {
            legacy$renderingPlacedConduit = false;
            return;
        }
        legacy$renderingPlacedConduit = true;
        poseStack.pushPose();
        poseStack.translate(0.5F, conduit.isActive() ? -0.1125F : -0.3125F, 0.5F);
        Integer rotation = ConduitRotationCache.get(level, conduit.getBlockPos());
        if (rotation != null) poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * 22.5F));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    @Inject(method = /*? if <1.21.5 {*/"render(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"/*?} else {*//*"render(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V"*//*?}*/, at = @At("RETURN"))
    private void renderReturn(ConduitBlockEntity conduit, float tickDelta, PoseStack poseStack, MultiBufferSource source, int light, int overlay, /*? if >=1.21.5 {*//*Vec3 cameraPos, *//*?}*/ CallbackInfo ci) {
        if (!legacy$renderingPlacedConduit) return;
        legacy$renderingPlacedConduit = false;
        poseStack.popPose();
    }
}
