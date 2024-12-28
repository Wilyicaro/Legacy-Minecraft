package wily.legacy.mixin.base;

import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.init.LegacyGameRules;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract GameRules getGameRules();

    @Shadow public abstract ServerLevel overworld();

    @Inject(method = "isPvpAllowed", at = @At("HEAD"), cancellable = true)
    public void isPvpAllowed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getGameRules().getBoolean(LegacyGameRules.PLAYER_VS_PLAYER));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci){
        Legacy4J.currentServer = (MinecraftServer) (Object) this;
    }
}
