package wily.legacy.mixin;

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
import wily.legacy.client.LegacyWorldSettings;

import java.util.Collections;
import java.util.List;

@Mixin(LevelSettings.class)
public class ClientLevelSettingsMixin implements LegacyWorldSettings {
    @Mutable
    @Shadow @Final private boolean allowCommands;
    long seed;
    boolean difficultyLocked = false;
    boolean trustPlayers = true;

    List<String> selectedResourcePacks = Collections.emptyList();
    @Inject(method = "parse", at = @At("RETURN"))
    private static void parse(Dynamic<?> dynamic, WorldDataConfiguration worldDataConfiguration, CallbackInfoReturnable<LevelSettings> cir) {
        ((LegacyWorldSettings) (Object)cir.getReturnValue()).setDifficultyLocked(dynamic.get("DifficultyLocked").asBoolean(false));
        ((LegacyWorldSettings) (Object)cir.getReturnValue()).setTrustPlayers(dynamic.get("TrustPlayers").asBoolean(true));
        ((LegacyWorldSettings) (Object)cir.getReturnValue()).setDisplaySeed(dynamic.get("WorldGenSettings").orElseEmptyMap().get("seed").asLong(0));
        ((LegacyWorldSettings) (Object)cir.getReturnValue()).setSelectedResourcePacks(dynamic.get("SelectedResourcePacks").asStream().flatMap(r->r.asString().result().stream()).toList());
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void copy(CallbackInfoReturnable<LevelSettings> cir) {
        LegacyWorldSettings settings = ((LegacyWorldSettings) (Object)cir.getReturnValue());
        settings.setDifficultyLocked(isDifficultyLocked());
        settings.setTrustPlayers(trustPlayers());
        settings.setDisplaySeed(getDisplaySeed());
        settings.setSelectedResourcePacks(getSelectedResourcePacks());
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
    public void setSelectedResourcePacks(List<String> packs) {
        selectedResourcePacks = packs;
    }

    @Override
    public List<String> getSelectedResourcePacks() {
        return selectedResourcePacks;
    }
}
