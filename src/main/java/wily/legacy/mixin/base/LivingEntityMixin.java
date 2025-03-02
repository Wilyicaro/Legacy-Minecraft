package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow protected ItemStack useItem;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = /*? if <1.21.2 {*/"travel"/*?} else {*//*"travelInAir"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z", ordinal = /*? if <1.21.2 {*/2/*?} else {*//*0*//*?}*/))
    public boolean travelFlight(LivingEntity instance) {
        return !isLegacyFlying() && onGround();
    }

    private boolean isLegacyFlying(){
        return ((LivingEntity)(Object)this instanceof Player p && p.getAbilities().flying && (!level().isClientSide || FactoryAPIClient.hasModOnServer));
    }
    @Redirect(method = /*? if <1.21.2 {*/"travel"/*?} else {*//*"travelInAir"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = /*? if <1.21.2 {*/3/*?} else {*//*1*//*?}*/))
    public void travelFlight(LivingEntity instance, double x, double y, double z) {
        setDeltaMovement((isLegacyFlying() ? 0.6 : 1) * x,(isLegacyFlying() ? 0.546 : 1) * y,(isLegacyFlying() ? 0.6 : 1) * z);
    }
    @Inject(method =  /*? if <1.21.2 {*/"hurt"/*?} else {*//*"hurtServer"*//*?}*/, at = @At("HEAD"), cancellable = true)
    public void hurt(/*? if >=1.21.2 {*//*ServerLevel level, *//*?}*/ DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if (!level().isClientSide && !level().getServer().isPvpAllowed() && damageSource.getDirectEntity() instanceof Player && (this instanceof OwnableEntity o && damageSource.getDirectEntity().equals(o.getOwner()) || ((Object)this) instanceof IronGolem i && i.isPlayerCreated() || ((Object)this) instanceof SnowGolem)){
            cir.setReturnValue(false);
        }
    }
    //? if >=1.21.2 {
    /*@Redirect(method = "calculateEntityAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    public boolean render(LivingEntity instance){
        return true;
    }
    *///?}

    //? if <1.21.2 {
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    public void isBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.legacySwordBlocking.get() && useItem.getItem() instanceof SwordItem) cir.setReturnValue(false);
    }
    //?}
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
    protected void getDamageAfterArmorAbsorb(DamageSource damageSource, float f, CallbackInfoReturnable<Float> cir) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR) && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && useItem.getItem() instanceof SwordItem) cir.setReturnValue(cir.getReturnValue()/2);
    }
}
