package wily.legacy.mixin.base.client;

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
import wily.legacy.client.PackAlbum;

@Mixin(PrimaryLevelData.class)
public abstract class ClientPrimaryLevelDataMixin implements LegacyClientWorldSettings {

    @Shadow private boolean difficultyLocked;

    @Shadow private LevelSettings settings;

    public boolean trustPlayers() {
        return LegacyClientWorldSettings.of(settings).trustPlayers();
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        LegacyClientWorldSettings.of(settings).setTrustPlayers(trust);
    }

    @Override
    public boolean isDifficultyLocked() {
        return difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
        difficultyLocked = locked;
        LegacyClientWorldSettings.of(settings).setDifficultyLocked(locked);
    }

    @Override
    public void setAllowCommands(boolean allow) {
        LegacyClientWorldSettings.of(settings).setAllowCommands(allow);
    }
    @Inject(method = "setTagData",at = @At("TAIL"))
    private void setTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci) {
        compoundTag.putBoolean("TrustPlayers",trustPlayers());
        compoundTag.putString("SelectedResourceAssort", getSelectedResourceAlbum().id());
    }
    public long getDisplaySeed() {
        return LegacyClientWorldSettings.of(settings).getDisplaySeed();
    }

    @Override
    public void setDisplaySeed(long s) {
        LegacyClientWorldSettings.of(settings).setDisplaySeed(s);
    }

    @Override
    public PackAlbum getSelectedResourceAlbum() {
        return LegacyClientWorldSettings.of(settings).getSelectedResourceAlbum();
    }

    @Override
    public void setSelectedResourceAlbum(PackAlbum album) {
        LegacyClientWorldSettings.of(settings).setSelectedResourceAlbum(album);
    }
}
