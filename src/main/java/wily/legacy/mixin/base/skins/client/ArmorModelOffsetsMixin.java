package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.ArmorOffsetRenderContext;

@Mixin(HumanoidModel.class)
public abstract class ArmorModelOffsetsMixin {
    @Unique
    private static void consoleskins$setRenderOffset(ModelPart part, float[] offset) {
        if (part != null) ((ArmorOffsetRenderContext.PartAccess) (Object) part).consoleskins$setRenderOffset(offset);
    }

    @Unique
    private static void consoleskins$applyScale(ModelPart part, float[] scale) {
        if (part == null || scale == null || scale.length < 3) return;
        part.xScale *= scale[0];
        part.yScale *= scale[1];
        part.zScale *= scale[2];
    }

    @Unique
    private static void consoleskins$resetScale(ModelPart part) {
        if (part == null) return;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }

    @Unique
    private static ModelPart[] consoleskins$parts(HumanoidModel<?> model) {
        return new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
    }

    @Unique
    private static void consoleskins$clear(ModelPart[] parts, ModelPart hat) {
        consoleskins$setRenderOffset(hat, null);
        for (ModelPart part : parts) {
            consoleskins$setRenderOffset(part, null);
            consoleskins$resetScale(part);
        }
    }

    @Unique
    private static void consoleskins$applyRenderOffsets(ModelPart[] parts, float[][] offsets) {
        if (offsets == null) return;
        for (int i = 0; i < parts.length; i++) consoleskins$setRenderOffset(parts[i], offsets[i]);
    }

    @Unique
    private static void consoleskins$applyScales(ModelPart[] parts, float[][] scales) {
        if (scales == null) return;
        for (int i = 0; i < parts.length; i++) consoleskins$applyScale(parts[i], scales[i]);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$applyArmorOffsets(HumanoidRenderState state, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        ModelPart[] parts = consoleskins$parts(model);
        ArmorOffsetRenderContext.Offsets offsets = ArmorOffsetRenderContext.renderOffsets();
        consoleskins$clear(parts, model.hat);
        if (offsets == null) return;
        consoleskins$applyScales(parts, offsets.scales());
        consoleskins$applyRenderOffsets(parts, offsets.renderOffsets());
    }
}
