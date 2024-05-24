package wily.legacy.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    @Shadow public abstract Abilities getAbilities();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "RETURN"))
    public void drop(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        if (cir.getReturnValue() != null && !level().isClientSide && bl2) super.playSound(SoundEvents.ITEM_PICKUP,1.0f,1.0f);
    }
    @Inject(method = "getFlyingSpeed", at = @At(value = "RETURN"), cancellable = true)
    protected void getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if (level().isClientSide && !Legacy4JClient.isModEnabledOnServer()) return;
        cir.setReturnValue(cir.getReturnValueF() * (getAbilities().flying ? (isSprinting() ? 6 : 2) : 1));
    }
}
