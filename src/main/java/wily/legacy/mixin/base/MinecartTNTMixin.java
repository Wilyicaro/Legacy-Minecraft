package wily.legacy.mixin.base;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.PlayerTrustOwnedSource;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.world.PlayerTrustAdmin;

import java.util.UUID;

@Mixin(MinecartTNT.class)
public class MinecartTNTMixin implements PlayerTrustOwnedSource {
    @Unique
    private UUID legacy$responsiblePlayer;

    @Inject(method = "primeFuse", at = @At("HEAD"))
    private void primeFuse(@Nullable DamageSource source, CallbackInfo ci) {
        capture(source);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void hurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.isOnFire()) capture(level, source);
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void explode(@Nullable DamageSource source, double speedSqr, CallbackInfo ci) {
        capture(source);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        if (legacy$responsiblePlayer != null) output.store("LegacyTrustOwner", UUIDUtil.CODEC, legacy$responsiblePlayer);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        setResponsiblePlayer(input.read("LegacyTrustOwner", UUIDUtil.CODEC).orElse(null));
    }

    @Unique
    private void capture(@Nullable DamageSource source) {
        if (((MinecartTNT) (Object) this).level() instanceof ServerLevel level) capture(level, source);
    }

    @Unique
    private void capture(ServerLevel level, @Nullable DamageSource source) {
        if (source == null) return;
        PlayerTrustPolicy player = PlayerTrustAdmin.getResponsiblePlayer(level, source);
        if (player != null) setResponsiblePlayer(player.playerId());
    }

    @Override
    public @Nullable UUID getResponsiblePlayer() {
        return legacy$responsiblePlayer;
    }

    @Override
    public void setResponsiblePlayer(@Nullable UUID player) {
        legacy$responsiblePlayer = player;
    }
}
