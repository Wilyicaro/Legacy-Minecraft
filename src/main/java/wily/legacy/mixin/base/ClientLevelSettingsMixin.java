package wily.legacy.mixin.base;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.screen.Assort;

@Mixin(LevelSettings.class)
public class ClientLevelSettingsMixin implements LegacyClientWorldSettings {
    @Mutable
    @Shadow @Final private boolean allowCommands;
    long seed;
    boolean difficultyLocked = false;
    boolean trustPlayers = true;

    String selectedResourceAssort = Assort.MINECRAFT.id();
    @Inject(method = "parse", at = @At("RETURN"))
    private static void parse(Dynamic<?> dynamic, WorldDataConfiguration worldDataConfiguration, CallbackInfoReturnable<LevelSettings> cir) {
        LegacyClientWorldSettings.of(cir.getReturnValue()).setDifficultyLocked(dynamic.get("DifficultyLocked").asBoolean(false));
        LegacyClientWorldSettings.of(cir.getReturnValue()).setTrustPlayers(dynamic.get("TrustPlayers").asBoolean(true));
        LegacyClientWorldSettings.of(cir.getReturnValue()).setDisplaySeed(dynamic.get("WorldGenSettings").orElseEmptyMap().get("seed").asLong(0));
        LegacyClientWorldSettings.of(cir.getReturnValue()).setSelectedResourceAssort(Assort.resourceById(dynamic.get("SelectedResourceAssort").asString(Assort.MINECRAFT.id())));
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void copy(CallbackInfoReturnable<LevelSettings> cir) {
        LegacyClientWorldSettings settings = LegacyClientWorldSettings.of(cir.getReturnValue());
        settings.setDifficultyLocked(isDifficultyLocked());
        settings.setTrustPlayers(trustPlayers());
        settings.setDisplaySeed(getDisplaySeed());
        settings.setSelectedResourceAssort(getSelectedResourceAssort());
    }

    public long getDisplaySeed() {
        return seed;
    }

    @Override
    public void setDisplaySeed(long s) {
        seed = s;
    }

    public boolean trustPlayers() {
        return trustPlayers;
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        trustPlayers = trust;
    }

    @Override
    public boolean isDifficultyLocked() {
        return difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
        difficultyLocked = locked;

    }
    @Override
    public void setAllowCommands(boolean allow) {
        allowCommands = allow;
    }

    @Override
    public void setSelectedResourceAssort(Assort assort) {
        selectedResourceAssort = assort.id();
    }

    @Override
    public Assort getSelectedResourceAssort() {
        return Assort.resourceById(selectedResourceAssort);
    }
}
