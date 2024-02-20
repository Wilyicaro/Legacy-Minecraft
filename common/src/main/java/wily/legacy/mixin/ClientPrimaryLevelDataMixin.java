package wily.legacy.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyWorldSettings;

import java.util.List;

@Mixin(PrimaryLevelData.class)
public abstract class ClientPrimaryLevelDataMixin implements LegacyWorldSettings {

    @Shadow private boolean difficultyLocked;

    @Shadow private LevelSettings settings;

    public boolean trustPlayers() {
        return ((LegacyWorldSettings)(Object)settings).trustPlayers();
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        ((LegacyWorldSettings)(Object)settings).setTrustPlayers(trust);
    }

    @Override
    public boolean isDifficultyLocked() {
        return difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
        difficultyLocked = locked;
        ((LegacyWorldSettings)(Object)settings).setDifficultyLocked(locked);
    }

    @Override
    public void setAllowCommands(boolean allow) {
        ((LegacyWorldSettings)(Object)settings).setAllowCommands(allow);
    }
    @Inject(method = "setTagData",at = @At("TAIL"))
    private void setTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci) {
        compoundTag.putBoolean("TrustPlayers",trustPlayers());
        ListTag packs = new ListTag();
        getSelectedResourcePacks().forEach(p-> packs.add(StringTag.valueOf(p)));
        compoundTag.put("SelectedResourcePacks",packs);
    }
    public long getDisplaySeed() {
        return ((LegacyWorldSettings)(Object)settings).getDisplaySeed();
    }

    @Override
    public void setDisplaySeed(long s) {
        ((LegacyWorldSettings)(Object)settings).setDisplaySeed(s);
    }

    @Override
    public List<String> getSelectedResourcePacks() {
        return ((LegacyWorldSettings)(Object)settings).getSelectedResourcePacks();
    }

    @Override
    public void setSelectedResourcePacks(List<String> packs) {
        ((LegacyWorldSettings)(Object)settings).setSelectedResourcePacks(packs);
    }
}
