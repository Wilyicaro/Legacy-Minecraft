package wily.legacy.mixin.base;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;

@Mixin(/*? if <1.21.2 {*//*Level*//*?} else {*/ServerLevel/*?}*/.class)
public abstract class LevelMixin {
    @Shadow public abstract GameRules getGameRules();

    //? if <1.20.5 {
    /*@Inject(method = /^? if >1.20.2 {^/"explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/world/level/Explosion;"/^?} else {^//^"explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Z)Lnet/minecraft/world/level/Explosion;"^//^?}^/, at = @At("HEAD"), cancellable = true)
    public void explode(Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, Level.ExplosionInteraction explosionInteraction, boolean bl2, /^? if >1.20.2 {^/ParticleOptions particleOptions, ParticleOptions particleOptions2, SoundEvent soundEvent,/^?}^/ CallbackInfoReturnable<Explosion> cir) {
        if (explosionInteraction != Level.ExplosionInteraction.MOB && !getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) cir.setReturnValue(new Explosion((Level) (Object)this, entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, Explosion.BlockInteraction.KEEP/^? if >1.20.2 {^/, particleOptions, particleOptions2, soundEvent/^?}^/));
    }
    *///?} else if <1.21.2 {
    /*@Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/Explosion;", at = @At("HEAD"), cancellable = true)
    public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, Level.ExplosionInteraction explosionInteraction, boolean bl2, ParticleOptions particleOptions, ParticleOptions particleOptions2, Holder<SoundEvent> holder, CallbackInfoReturnable<Explosion> cir) {
        if (explosionInteraction != Level.ExplosionInteraction.MOB && !getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) cir.setReturnValue(new Explosion((Level) (Object)this, entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, Explosion.BlockInteraction.KEEP, particleOptions, particleOptions2, holder));
    }
    *///?} else {
    @Inject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;explode()V"), cancellable = true)
    public void explode(Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, Level.ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions2, Holder<SoundEvent> holder, CallbackInfo ci) {
        if (explosionInteraction != Level.ExplosionInteraction.MOB && !getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) ci.cancel();
    }
    //?}
}
