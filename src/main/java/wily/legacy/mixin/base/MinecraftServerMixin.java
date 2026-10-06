package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.world.SeedStart;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @WrapOperation(method = "setInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos seedStart(Climate.Sampler sampler, Operation<BlockPos> original, @Local(argsOnly = true) ServerLevelData levelData) {
        return levelData instanceof SeedStart start && start.getSeedStart() != null ? start.getSeedStart() : original.call(sampler);
    }

    @Inject(method = "onGameRuleChanged", at = @At("RETURN"))
    private <T> void onGameRuleChanged(GameRule<T> gameRule, T object, CallbackInfo ci) {
        LegacyGameRules.onGameRuleChanged((MinecraftServer) (Object)this, gameRule, object);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void legacy$stopServer(CallbackInfo ci) {
        GlobalDifficultyStatsStore.saveAll();
    }
}
