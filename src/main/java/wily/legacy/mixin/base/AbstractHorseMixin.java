package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
*///?} else {
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(AbstractHorse.class)
public class AbstractHorseMixin {
    @Inject(method = "isTamed", at = @At("HEAD"), cancellable = true)
    private void legacy$isTamed(CallbackInfoReturnable<Boolean> cir) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) && (Object)this instanceof SkeletonHorse) cir.setReturnValue(true);
    }

    @ModifyExpressionValue(method = {"getControllingPassenger", "canJump", "onPlayerJump"}, at = @At(value = "INVOKE", target = /*? if >=1.21.11 {*//*"Lnet/minecraft/world/entity/animal/equine/AbstractHorse;isSaddled()Z"*//*?} else {*/"Lnet/minecraft/world/entity/animal/horse/AbstractHorse;isSaddled()Z"/*?}*/))
    private boolean legacy$skeletonHorseRidesWithoutSaddle(boolean saddled) {
        return saddled || FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) && (Object)this instanceof SkeletonHorse;
    }

    @Inject(method = "handleEating", at = @At("RETURN"))
    private void legacy$handleEating(Player player, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) || !cir.getReturnValue()) return;
        AbstractHorse horse = (AbstractHorse) (Object) this;
        horse.level().playSound(null, horse.getX(), horse.getY(), horse.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);
        if (!(horse.level() instanceof ServerLevel level)) return;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, itemStack), horse.getX(), horse.getEyeY(), horse.getZ(), 8, horse.getBbWidth() * 0.25, 0.1, horse.getBbWidth() * 0.25, 0.03);
    }
}
