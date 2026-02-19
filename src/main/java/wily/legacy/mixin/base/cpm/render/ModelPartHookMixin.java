package wily.legacy.mixin.base.cpm.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.CustomModelSkins.cpl.math.MatrixStack;
import wily.legacy.CustomModelSkins.cpl.render.RecordBuffer;
import wily.legacy.CustomModelSkins.cpm.client.ModelPartHooks;
import wily.legacy.CustomModelSkins.cpm.client.VBuffer;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.ModelRenderManager.RedirectRenderer;

@Mixin(ModelPart.class)
public class ModelPartHookMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
    private void cpm$render(PoseStack pose, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        RecordBuffer rb = ModelPartHooks.getRecordBuffer((ModelPart) (Object) this);
        if (rb != null) {
            rb.replay(new VBuffer(consumer, light, overlay, 0f, pose));
            ci.cancel();
        }
    }

    @Inject(method = "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"), cancellable = true)
    private void cpm$translateAndRotate(PoseStack stack, CallbackInfo ci) {
        RedirectRenderer<ModelPart> rr = ModelPartHooks.getRedirectRenderer((ModelPart) (Object) this);
        if (rr != null && rr.getHolder() != null && rr.getHolder().def != null) {
            MatrixStack.Entry e = rr.getPartTransform();
            if (e != null) {
                stack.last().pose().mul(new Matrix4f().setTransposed(e.getMatrixArray()));
                stack.last().normal().mul(new Matrix3f().set(e.getNormalArray3()).transpose());
                ci.cancel();
            }
        }
    }

    @Inject(method = "isEmpty()Z", at = @At("HEAD"), cancellable = true)
    private void cpm$isEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (ModelPartHooks.getSelfRenderer((ModelPart) (Object) this) != null || ModelPartHooks.getRedirectRenderer((ModelPart) (Object) this) != null || ModelPartHooks.getRecordBuffer((ModelPart) (Object) this) != null) {
            cir.setReturnValue(false);
        }
    }
}
