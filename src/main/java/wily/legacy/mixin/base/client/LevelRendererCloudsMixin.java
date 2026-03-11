package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.LegacyOptions;

@Mixin(LevelRenderer.class)
public class LevelRendererCloudsMixin {
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addCloudsPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/CloudStatus;Lnet/minecraft/world/phys/Vec3;FIF)V"), index = 5)
    private float lowerLegacyCloudHeight(float cloudHeight) {
        return LegacyOptions.legacyCloudHeight.get() ? 128.0f : cloudHeight;
    }
}
