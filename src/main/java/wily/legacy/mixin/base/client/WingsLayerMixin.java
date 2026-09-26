package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.client.LegacyLivingEntityRenderState;

@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void hideElytraForHostInvisiblePlayers(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, HumanoidRenderState renderState, float limbAngle, float limbDistance, CallbackInfo ci) {
        LegacyLivingEntityRenderState legacyState = FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LegacyLivingEntityRenderState.class);
        if (legacyState != null && legacyState.hostInvisible) {
            ci.cancel();
        }
    }
}
