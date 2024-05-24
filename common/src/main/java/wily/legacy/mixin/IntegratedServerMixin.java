package wily.legacy.mixin;

import com.mojang.datafixers.DataFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyWorldSettings;

import java.io.IOException;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {
    @Shadow @Final private Minecraft minecraft;

    @Shadow public LanServerPinger lanPinger;

    @Shadow @Final private static Logger LOGGER;

    public IntegratedServerMixin(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, services, chunkProgressListenerFactory);
    }

    public IntegratedServer self(){
        return (IntegratedServer) (Object) this;
    }
    @Inject(method = "tickServer", at = @At("HEAD"))
    public void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (Legacy4JClient.manualSave){
            Legacy4JClient.manualSave = false;
            getProfiler().push("manualSave");
            LOGGER.info("Saving manually...");
            this.saveEverything(false, false, true);
            getProfiler().pop();
        }
    }
    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/server/IntegratedServer;paused:Z", opcode = Opcodes.GETFIELD, ordinal = 1))
    public boolean tickServer(IntegratedServer instance) {
        return instance.isPaused() && ((LegacyOptions) minecraft.options).autoSave().get();
    }
    @Inject(method = "stopServer", at = @At(value = "HEAD"), cancellable = true)
    public void stopServer(CallbackInfo ci) {
        if (!((LegacyOptions) minecraft.options).autoSave().get()){
            ci.cancel();
            if (Legacy4JClient.deleteLevelWhenExitWithoutSaving){
                try {
                    storageSource.deleteLevel();
                } catch (IOException e) {
                    Legacy4J.LOGGER.warn(e.getMessage());
                }
            }
            if (self().metricsRecorder.isRecording()) {
                self().cancelRecordingMetrics();
            }
            self().getConnection().stop();
            LOGGER.info("Stopping server");

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
        Legacy4JClient.deleteLevelWhenExitWithoutSaving = false;
    }

    @Override
    public boolean isUnderSpawnProtection(ServerLevel serverLevel, BlockPos blockPos, Player player) {
        if (!isSingleplayerOwner(player.getGameProfile()) && !((LegacyWorldSettings)worldData).trustPlayers()) return true;
        return super.isUnderSpawnProtection(serverLevel, blockPos, player);
    }
}
