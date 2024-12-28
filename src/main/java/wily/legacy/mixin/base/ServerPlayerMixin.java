package wily.legacy.mixin.base;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements LegacyPlayer, LegacyPlayerInfo {
    @Shadow @Final public ServerPlayerGameMode gameMode;

    @Shadow public abstract void onUpdateAbilities();

    @Shadow public abstract ServerStatsCounter getStats();

    @Shadow public abstract ServerLevel serverLevel();

    int position = -1;
    boolean classicCrafting = true;
    boolean disableExhaustion = false;
    boolean mayFlySurvival = false;


    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }


    @Override
    public GameProfile legacyMinecraft$getProfile() {
        return getGameProfile();
    }

    @Override
    public int getIdentifierIndex() {
        return position;
    }

    @Override
    public void setIdentifierIndex(int i) {
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
        if (getAbilities().mayfly != mayFlySurvival && gameMode.isSurvival()){
            getAbilities().mayfly = mayFlySurvival;
            if (!getAbilities().mayfly && getAbilities().flying) getAbilities().flying = false;
            onUpdateAbilities();
        }
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
        getStats().stats.clear();
        getStats().stats.putAll(statsMap);
    }

    @Override
    public void setVisibility(boolean visible) {
        super.setInvisible(!visible);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean("DisableExhaustion", isExhaustionDisabled());
        compoundTag.putBoolean("MayFlySurvival", mayFlySurvival());

    }
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        setDisableExhaustion(compoundTag.getBoolean("DisableExhaustion"));
        setMayFlySurvival(compoundTag.getBoolean("MayFlySurvival"));
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"), cancellable = true)
    public void startSleepInBed(BlockPos blockPos, CallbackInfoReturnable<Either<BedSleepingProblem, Unit>> cir) {
        Either<BedSleepingProblem,Unit> either = cir.getReturnValue();
        if (level().isDay() && either.left().isPresent() && either.left().get() == BedSleepingProblem.NOT_POSSIBLE_NOW && !this.isCreative()) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
            if (!this.level().getEntitiesOfClass(Monster.class, new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0), (argx) -> argx.isPreventingPlayerRest(/*? if >=1.21.2 {*/this.serverLevel(), /*?}*/this)).isEmpty()) {
                cir.setReturnValue(Either.left(BedSleepingProblem.NOT_SAFE));
            }
        }
    }
}
