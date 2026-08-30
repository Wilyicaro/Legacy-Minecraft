package wily.legacy.mixin.base.compat.replaymod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.ReplayCompat;

@Pseudo
@Mixin(targets = "com.replaymod.render.rendering.VideoRenderer", remap = false)
public class VideoRendererMixin {
    @Inject(method = "renderVideo", at = @At("HEAD"))
    private void startRendering(CallbackInfoReturnable<Boolean> cir) {
        ReplayCompat.setRendering(true);
    }

    @Inject(method = "renderVideo", at = @At("RETURN"))
    private void stopRendering(CallbackInfoReturnable<Boolean> cir) {
        ReplayCompat.setRendering(false);
    }
}
