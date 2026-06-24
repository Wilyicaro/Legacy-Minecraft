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
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;

@Mixin(ModelPart.class)
public abstract class SkipRenderMixin implements ModelPartSkipRenderOverrideAccess {
    @Shadow
    public boolean visible;
    @Shadow
    private boolean skipDraw;
    @Unique
    private boolean consoleskins$forceRender;

    @Override
    public boolean consoleskins$getForceRender() {
        return consoleskins$forceRender;
    }

    @Override
    public void consoleskins$setForceRender(boolean value) {
        consoleskins$forceRender = value;
    }

    @Unique
    private void consoleskins$applyForceRender() {
        if (!consoleskins$forceRender) return;
        skipDraw = false;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", at = @At("HEAD"), require = 0)
    private void consoleskins$applyForceRender4(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, CallbackInfo callbackInfo) {
        consoleskins$applyForceRender();
    }

    /*? if <1.20.5 {*/
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", at = @At("HEAD"), require = 0)
    private void consoleskins$applyForceRender8(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        consoleskins$applyForceRender();
    }
    *//*?} else {*/
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), require = 0)
    private void consoleskins$applyForceRender5(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, CallbackInfo callbackInfo) {
        consoleskins$applyForceRender();
    }
    /*?}*/
}
