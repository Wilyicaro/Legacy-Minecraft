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
    public void consoleskins$setForceRender(boolean v) {
        consoleskins$forceRender = v;
    }

    @Unique
    private void consoleskins$applyForceRender() {
        if (!consoleskins$forceRender) return;
        skipDraw = false;
        visible = true;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            at = @At("HEAD"), cancellable = false, require = 0
    )
    private void consoleskins$applyForceRender4(PoseStack ps, VertexConsumer vc, int light, int overlay, CallbackInfo ci) {
        consoleskins$applyForceRender();
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"), cancellable = false, require = 0
    )
    private void consoleskins$applyForceRender5(PoseStack ps, VertexConsumer vc, int light, int overlay, int color, CallbackInfo ci) {
        consoleskins$applyForceRender();
    }
}
