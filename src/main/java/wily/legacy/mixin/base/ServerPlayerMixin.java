package wily.legacy.mixin.base;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    @Shadow
    @Final
    public ServerPlayerGameMode gameMode;
    int position = -1;
    boolean classicCrafting = true;
    boolean classicTrading = true;
    boolean classicStonecutting = true;
    boolean classicLoom = true;
    boolean disableExhaustion = false;
    boolean mayFlySurvival = false;

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Shadow
    public abstract void onUpdateAbilities();

    @Shadow
    public abstract ServerStatsCounter getStats();

    @Shadow
    public abstract ServerLevel level();

    @Shadow @Final private MinecraftServer server;

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
    public boolean hasClassicTrading() {
        return classicTrading;
    }

    @Override
    public void setTrading(boolean classic) {
        classicTrading = classic;
    }

    @Override
    public boolean hasClassicStonecutting() {
        return classicStonecutting;
    }

    @Override
    public void setStonecutting(boolean classic) {
        classicStonecutting = classic;
    }

    @Override
    public boolean hasClassicLoom() {
        return classicLoom;
    }

    @Override
    public void setLoom(boolean classic) {
        classicLoom = classic;
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
        getStats().stats.clear();
        getStats().stats.putAll(statsMap);
    }

    @Override
    public void setVisibility(boolean visible) {
        super.setInvisible(!visible);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    public void addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putBoolean("DisableExhaustion", isExhaustionDisabled());
        valueOutput.putBoolean("MayFlySurvival", mayFlySurvival());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        setDisableExhaustion(input.getBooleanOr("DisableExhaustion", false));
        setMayFlySurvival(input.getBooleanOr("MayFlySurvival", false));
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"), cancellable = true)
    public void startSleepInBed(BlockPos blockPos, CallbackInfoReturnable<Either<BedSleepingProblem, Unit>> cir) {
        Either<BedSleepingProblem, Unit> either = cir.getReturnValue();
        if (level()./*? if <1.21.5 {*//*isDay*//*?} else {*/isBrightOutside/*?}*/() && either.left().isPresent() && either.left().get() == BedSleepingProblem.NOT_POSSIBLE_NOW && !this.isCreative()) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
            if (!this.level().getEntitiesOfClass(Monster.class, new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0), (argx) -> argx.isPreventingPlayerRest(level(), this)).isEmpty()) {
                cir.setReturnValue(Either.left(BedSleepingProblem.NOT_SAFE));
            }
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "RETURN"))
    public void drop(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        if (cir.getReturnValue() != null && !level().isClientSide() && bl2) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_PICKUP, this.getSoundSource(), 1.0f, 1.0f);
        }
    }
}
