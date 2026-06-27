package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.mobcaps.LegacyMobCaps;
import wily.legacy.util.LegacyTags;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private Entity self(){
        return (Entity) (Object)this;
    }

    @Inject(method = "setCustomName", at = @At("RETURN"))
    public void setCustomName(Component component, CallbackInfo ci) {
        if (self() instanceof Mob m) m.setPersistenceRequired();
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    public void setRemoved(Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (self().level() instanceof ServerLevel && !self().isRemoved()) LegacyMobCaps.handleEntityRemoved(self());
    }

    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    public void isInvisible(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && self() instanceof LegacyPlayerInfo info && !info.legacy$isVisible()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "doWaterSplashEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getSwimSplashSound()Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent getSwimSplashSound(SoundEvent original) {
        return legacy$getSplashSound(original);
    }

    @ModifyExpressionValue(method = "doWaterSplashEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getSwimHighSpeedSplashSound()Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent getSwimHighSpeedSplashSound(SoundEvent original) {
        return legacy$getSplashSound(original);
    }

    @ModifyArg(method = "doWaterSplashEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"), index = 1)
    private float increaseWaterSplashVolume(float volume) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio) ? volume * 3.0f : volume;
    }

    @Unique
    private SoundEvent legacy$getSplashSound(SoundEvent original) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio) && self().getType().builtInRegistryHolder().is(LegacyTags.OLD_SPLASH_SOUND) ? LegacyRegistries.ENTITY_GENERIC_OLD_SPLASH.get() : original;
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    protected boolean updateSwimming(boolean original) {
        return ((!self().level().isClientSide && self().level().getServer().getGameRules().getBoolean(LegacyGameRules.LEGACY_SWIMMING)) && (self().isInWater() && self().getXRot() > 0) || original);
    }
}
