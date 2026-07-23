package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyTntFlash;

@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {
    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void legacy$registerTntFlashBuffers(int size, CallbackInfo ci) {
        LegacyTntFlash.registerBuffers(((BufferSourceAccessor) bufferSource).fixedBuffers());
    }
}
