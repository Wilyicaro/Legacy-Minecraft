package wily.legacy.mixin.base;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.world.LegacyWorldSettings;

@Mixin(LevelSettings.class)
public class LevelSettingsMixin implements LegacyWorldSettings {
    private boolean trustPlayers = true;

    @Inject(method = "parse", at = @At("RETURN"))
    private static void parse(Dynamic<?> dynamic, WorldDataConfiguration worldDataConfiguration, CallbackInfoReturnable<LevelSettings> cir) {
        LegacyWorldSettings.of(cir.getReturnValue()).setTrustPlayers(dynamic.get("TrustPlayers").asBoolean(true));
    }

    @Inject(method = {"withGameType", "withDifficulty", "withDifficultyLock", "withDataConfiguration", "copy", "withLifecycle"}, at = @At("RETURN"))
    private void copyTrustPlayers(CallbackInfoReturnable<LevelSettings> cir) {
        LegacyWorldSettings.of(cir.getReturnValue()).setTrustPlayers(trustPlayers);
    }

    @Override
    public boolean trustPlayers() {
        return trustPlayers;
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        trustPlayers = trust;
    }
}
