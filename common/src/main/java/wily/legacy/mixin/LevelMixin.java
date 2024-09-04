package wily.legacy.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow public abstract GameRules getGameRules();

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/Explosion;", at = @At("HEAD"), cancellable = true)
    public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double d, double e, double f, float g, boolean bl, Level.ExplosionInteraction explosionInteraction, boolean bl2, ParticleOptions particleOptions, ParticleOptions particleOptions2, Holder<SoundEvent> holder, CallbackInfoReturnable<Explosion> cir) {
        if (explosionInteraction != Level.ExplosionInteraction.MOB && !getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) cir.setReturnValue(new Explosion((Level) (Object)this, entity, damageSource, explosionDamageCalculator, d, e, f, g, bl, Explosion.BlockInteraction.KEEP, particleOptions, particleOptions2, holder));
    }
}
