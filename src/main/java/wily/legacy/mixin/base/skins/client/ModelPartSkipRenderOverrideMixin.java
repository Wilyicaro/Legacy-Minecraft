package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;

@Mixin(ModelPart.class)
public abstract class ModelPartSkipRenderOverrideMixin implements ModelPartSkipRenderOverrideAccess {

    @Unique
    private boolean consoleskins$skipRenderOverride;

    @Override
    public boolean consoleskins$getSkipRenderOverride() {
        return consoleskins$skipRenderOverride;
    }

    @Override
    public void consoleskins$setSkipRenderOverride(boolean value) {
        consoleskins$skipRenderOverride = value;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void consoleskins$skipRender(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, CallbackInfo ci) {
        if (consoleskins$skipRenderOverride) ci.cancel();
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void consoleskins$skipRenderColor(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color, CallbackInfo ci) {
        if (consoleskins$skipRenderOverride) ci.cancel();
    }
}
