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
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;

@Mixin(ModelPart.class)
public abstract class ModelPartSkipRenderOverrideMixin implements ModelPartSkipRenderOverrideAccess {

    @Shadow private boolean skipDraw;
    @Shadow public boolean visible;

    @Unique private boolean consoleskins$skipRenderOverride;
    @Unique private boolean consoleskins$forceRender;

    @Override public boolean consoleskins$getSkipRenderOverride() { return consoleskins$skipRenderOverride; }
    @Override public void consoleskins$setSkipRenderOverride(boolean v) { consoleskins$skipRenderOverride = v; }
    @Override public boolean consoleskins$getForceRender() { return consoleskins$forceRender; }
    @Override public void consoleskins$setForceRender(boolean v) { consoleskins$forceRender = v; }

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
        at = @At("HEAD"), cancellable = false, require = 0
    )
    private void consoleskins$applyForceRender4(PoseStack ps, VertexConsumer vc, int light, int overlay, CallbackInfo ci) {
        if (consoleskins$forceRender) {
            DebugLog.debug("[ArmorFix] forceRender(4): skipDraw={} visible={}", skipDraw, visible);
            skipDraw = false;
            visible  = true;
        }
    }

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"), cancellable = false, require = 0
    )
    private void consoleskins$applyForceRender5(PoseStack ps, VertexConsumer vc, int light, int overlay, int color, CallbackInfo ci) {
        if (consoleskins$forceRender) {
            DebugLog.debug("[ArmorFix] forceRender(5): skipDraw={} visible={}", skipDraw, visible);
            skipDraw = false;
            visible  = true;
        }
    }
}
