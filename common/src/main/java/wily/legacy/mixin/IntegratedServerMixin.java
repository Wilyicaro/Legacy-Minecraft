package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

import java.io.IOException;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Shadow public LanServerPinger lanPinger;

    @Shadow @Final private static Logger LOGGER;

    public IntegratedServer self(){
        return (IntegratedServer) (Object) this;
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;saveEverything(ZZZ)Z"))
    public boolean tickServer(IntegratedServer instance, boolean message, boolean bl, boolean bl1) {
        return ((LegacyOptions) minecraft.options).autoSaveWhenPause().get() && instance.saveEverything(message, bl, bl1);
    }
    @Inject(method = "stopServer", at = @At(value = "HEAD"), cancellable = true)
    public void tickServer(CallbackInfo ci) {
        if (!((LegacyOptions) minecraft.options).autoSaveWhenPause().get()){
            ci.cancel();
            if (self().metricsRecorder.isRecording()) {
                self().cancelRecordingMetrics();
            }
            self().getConnection().stop();
            LOGGER.info("Stopping server");
            self().resources.close();
            try {
                self().storageSource.close();
            } catch (IOException iOException2) {
                LOGGER.error("Failed to unlock level {}", self().storageSource.getLevelId(), iOException2);
            }
            if (this.lanPinger != null) {
                this.lanPinger.interrupt();
                this.lanPinger = null;
            }
        }
    }
}
