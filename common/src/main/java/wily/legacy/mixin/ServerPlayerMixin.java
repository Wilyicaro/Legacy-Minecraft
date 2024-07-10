package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.player.LegacyPlayer;
import wily.legacy.player.LegacyPlayerInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements LegacyPlayer, LegacyPlayerInfo {
    @Shadow @Final public ServerPlayerGameMode gameMode;

    @Shadow public abstract void onUpdateAbilities();

    @Shadow public abstract ServerStatsCounter getStats();

    int position = -1;
    boolean classicCrafting = true;
    boolean disableExhaustion = false;
    boolean mayFlySurvival = false;

    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (getAbilities().mayfly != mayFlySurvival && gameMode.isSurvival()){
            getAbilities().mayfly = mayFlySurvival;
            if (!getAbilities().mayfly && getAbilities().flying) getAbilities().flying = false;
            onUpdateAbilities();
        }
    }

    @Override
    public GameProfile legacyMinecraft$getProfile() {
        return getGameProfile();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int i) {
        position = i;
    }

    @Override
    public boolean hasClassicCrafting() {
        return classicCrafting;
    }

    @Override
    public void setCrafting(boolean classic) {
        classicCrafting = classic;
    }

    @Override
    public boolean isExhaustionDisabled() {
        return disableExhaustion;
    }

    @Override
    public void setDisableExhaustion(boolean exhaustion) {
        this.disableExhaustion = exhaustion;
    }

    @Override
    public void setMayFlySurvival(boolean mayFlySurvival) {
        this.mayFlySurvival = mayFlySurvival;
    }

    @Override
    public boolean mayFlySurvival() {
        return mayFlySurvival;
    }

    @Override
    public boolean isVisible() {
        return !super.isInvisible();
    }

    @Override
    public Object2IntMap<Stat<?>> getStatsMap() {
        return getStats().stats;
    }

    @Override
    public void setStatsMap(Object2IntMap<Stat<?>> statsMap) {

    }

    @Override
    public void setVisibility(boolean visible) {
        super.setInvisible(!visible);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean("DisableExhaustion", isExhaustionDisabled());
        compoundTag.putBoolean("MayFly", mayFlySurvival());

    }
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        setDisableExhaustion(compoundTag.getBoolean("DisableExhaustion"));
        setMayFlySurvival(compoundTag.getBoolean("MayFlySurvival"));
    }
}
