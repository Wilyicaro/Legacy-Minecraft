package wily.legacy.mixin.base;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void legacy$stopServer(CallbackInfo ci) {
        GlobalDifficultyStatsStore.saveAll();
    }
}
