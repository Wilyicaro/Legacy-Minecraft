package wily.legacy.CustomModelSkins.cpm.mixin.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    public HumanoidArmorLayerMixin(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Shadow
    @Final
    private ArmorModelSet<HumanoidModel<HumanoidRenderState>> modelSet;

    @Inject(method = "submit", at = @At("HEAD"))
    private void consoleskins$preSubmit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                        HumanoidRenderState state, float f1, float f2, CallbackInfo ci) {

        if (CustomPlayerModelsClient.INSTANCE != null && getParentModel() instanceof HumanoidModel) {
            CustomPlayerModelsClient.INSTANCE.renderArmor(modelSet, getParentModel());
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void consoleskins$postSubmit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                                         HumanoidRenderState state, float f1, float f2, CallbackInfo ci) {
        if (CustomPlayerModelsClient.INSTANCE != null && CustomPlayerModelsClient.INSTANCE.manager != null) {

            CustomPlayerModelsClient.INSTANCE.manager.unbind(modelSet.head());
            CustomPlayerModelsClient.INSTANCE.manager.unbind(modelSet.chest());
            CustomPlayerModelsClient.INSTANCE.manager.unbind(modelSet.legs());
            CustomPlayerModelsClient.INSTANCE.manager.unbind(modelSet.feet());
        }
    }
}
