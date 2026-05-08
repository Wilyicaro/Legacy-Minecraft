package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.ArmorOffsetRenderContext;

@Mixin(ModelPart.class)
public abstract class PartRenderOffsetMixin implements ArmorOffsetRenderContext.PartAccess {
    @Shadow
    public float xScale;
    @Shadow
    public float yScale;
    @Shadow
    public float zScale;
    @Unique
    private float[] consoleskins$renderOffset;

    @Override
    public void consoleskins$setRenderOffset(float[] offset) {
        consoleskins$renderOffset = offset;
    }

    @Unique
    private static boolean consoleskins$isZero(float[] offset) {
        return offset == null || offset.length < 3 || offset[0] == 0.0F && offset[1] == 0.0F && offset[2] == 0.0F;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void consoleskins$applyRenderOffset(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        if (consoleskins$isZero(consoleskins$renderOffset)) return;
        float x = xScale == 0.0F ? consoleskins$renderOffset[0] : consoleskins$renderOffset[0] / xScale;
        float y = yScale == 0.0F ? consoleskins$renderOffset[1] : consoleskins$renderOffset[1] / yScale;
        float z = zScale == 0.0F ? consoleskins$renderOffset[2] : consoleskins$renderOffset[2] / zScale;
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
    }
}
