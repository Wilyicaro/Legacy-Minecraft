package wily.legacy.mixin.base.client.beacon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class BeamMixin {
    @Shadow
    private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer, int color, int startY, int endY, float x1, float z1, float x2, float z2, float u1, float u2, float v1, float v2) {
    }

    @Inject(method = "renderPart", at = @At("TAIL"))
    private static void renderPart(PoseStack.Pose pose, VertexConsumer consumer, int color, int startY, int endY, float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4, float u1, float u2, float v1, float v2, CallbackInfo ci) {
        renderQuad(pose, consumer, color, startY, endY, x2, z2, x1, z1, u1, u2, v1, v2);
        renderQuad(pose, consumer, color, startY, endY, x3, z3, x4, z4, u1, u2, v1, v2);
        renderQuad(pose, consumer, color, startY, endY, x4, z4, x2, z2, u1, u2, v1, v2);
        renderQuad(pose, consumer, color, startY, endY, x1, z1, x3, z3, u1, u2, v1, v2);
    }
}
