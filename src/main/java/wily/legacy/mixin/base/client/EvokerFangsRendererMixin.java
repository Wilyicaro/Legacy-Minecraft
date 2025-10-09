package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EvokerFangsRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
//?} else {
/*import net.minecraft.world.entity.projectile.EvokerFangs;
*///?}
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(EvokerFangsRenderer.class)
public class EvokerFangsRendererMixin {

    @Inject(method =  "submit(Lnet/minecraft/client/renderer/entity/state/EvokerFangsRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    public void render(EvokerFangsRenderState evokerFangsRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (LegacyOptions.legacyEvokerFangs.get()) poseStack.scale(2f,2f,2f);
    }
}
