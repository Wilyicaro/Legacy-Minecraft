package wily.legacy.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.screen.Assort;

@Mixin(PrimaryLevelData.class)
public abstract class ClientPrimaryLevelDataMixin implements LegacyClientWorldSettings {

    @Shadow private boolean difficultyLocked;

    @Shadow private LevelSettings settings;

    public boolean trustPlayers() {
        return ((LegacyClientWorldSettings)(Object)settings).trustPlayers();
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        ((LegacyClientWorldSettings)(Object)settings).setTrustPlayers(trust);
    }

    @Override
    public boolean isDifficultyLocked() {
        return difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
        difficultyLocked = locked;
        ((LegacyClientWorldSettings)(Object)settings).setDifficultyLocked(locked);
    }

    @Override
    public void setAllowCommands(boolean allow) {
        ((LegacyClientWorldSettings)(Object)settings).setAllowCommands(allow);
    }
    @Inject(method = "setTagData",at = @At("TAIL"))
    private void setTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci) {
        compoundTag.putBoolean("TrustPlayers",trustPlayers());
        compoundTag.putString("SelectedResourceAssort",getSelectedResourceAssort().id());
    }
    public long getDisplaySeed() {
        return ((LegacyClientWorldSettings)(Object)settings).getDisplaySeed();
    }

    @Override
    public void setDisplaySeed(long s) {
        ((LegacyClientWorldSettings)(Object)settings).setDisplaySeed(s);
    }

    @Override
    public Assort getSelectedResourceAssort() {
        return ((LegacyClientWorldSettings)(Object)settings).getSelectedResourceAssort();
    }

    @Override
    public void setSelectedResourceAssort(Assort assort) {
        ((LegacyClientWorldSettings)(Object)settings).setSelectedResourceAssort(assort);
    }
}
