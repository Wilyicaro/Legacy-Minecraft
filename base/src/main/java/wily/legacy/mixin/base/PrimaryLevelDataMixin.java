package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.world.LegacyWorldSettings;
import wily.legacy.world.SeedStart;

import java.util.UUID;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin implements LegacyWorldSettings, SeedStart {
    @Shadow
    private LevelSettings settings;
    private boolean trustPlayers;
    @Unique
    private BlockPos legacy$seedStart;

    @Override
    public BlockPos getSeedStart() {
        return legacy$seedStart;
    }

    @Override
    public void setSeedStart(BlockPos pos) {
        legacy$seedStart = pos == null ? null : pos.immutable();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        trustPlayers = LegacyWorldSettings.of(settings).trustPlayers();
    }

    @Override
    public boolean trustPlayers() {
        return trustPlayers;
    }

    @Override
    public void setTrustPlayers(boolean trust) {
        trustPlayers = trust;
    }

    @Inject(method = "setTagData", at = @At("TAIL"))
    private void setTagData(CompoundTag compoundTag, UUID uuid, CallbackInfo ci) {
        compoundTag.putBoolean("TrustPlayers", trustPlayers());
    }
}
