package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4JClient;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.gameRules;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    protected ItemStack useItem;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z", ordinal = 0))
    public boolean travelFlight(LivingEntity instance) {
        return !isLegacyFlying() && onGround();
    }

    private boolean isLegacyFlying() {
        return ((LivingEntity) (Object) this instanceof Player p && p.getAbilities().flying && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT));
    }

    @Redirect(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 1))
    public void travelFlight(LivingEntity instance, double x, double y, double z) {
        setDeltaMovement((isLegacyFlying() ? 0.6 : 1) * x, (isLegacyFlying() ? 0.546 : 1) * y, (isLegacyFlying() ? 0.6 : 1) * z);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void hurt(ServerLevel level, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if (!level().isClientSide() && !level().getServer().isPvpAllowed() && damageSource.getDirectEntity() instanceof Player && (this instanceof OwnableEntity o && damageSource.getDirectEntity().equals(o.getOwner()) || ((Object) this) instanceof IronGolem i && i.isPlayerCreated() || ((Object) this) instanceof SnowGolem)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "calculateEntityAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance) {
        return true;
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
    protected void getDamageAfterArmorAbsorb(DamageSource damageSource, float f, CallbackInfoReturnable<Float> cir) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR) && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && useItem.is(ItemTags.SWORDS))
            cir.setReturnValue(cir.getReturnValue() / 2);
    }

    @ModifyArg(method = "jumpInLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"), index = 1)
    protected double jumpInLiquid(double y, @Local(argsOnly = true) TagKey<Fluid> tagKey) {
        return tagKey.equals(FluidTags.WATER) && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING) ? y * 2 : y;
    }
}
