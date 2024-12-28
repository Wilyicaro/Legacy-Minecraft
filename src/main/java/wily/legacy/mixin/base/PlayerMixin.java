package wily.legacy.mixin.base;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4JClient;
import wily.legacy.entity.PlayerYBobbing;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerYBobbing {
    float oYBob;
    float yBob;

    @Shadow public abstract Abilities getAbilities();

    @Override
    public float oYBob() {
        return oYBob;
    }

    @Override
    public void setOYBob(float bob) {
        oYBob = bob;
    }

    @Override
    public float yBob() {
        return yBob;
    }

    @Override
    public void setYBob(float bob) {
        yBob = bob;
    }

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "RETURN"))
    public void drop(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        if (cir.getReturnValue() != null && !level().isClientSide && bl2) super.playSound(SoundEvents.ITEM_PICKUP,1.0f,1.0f);
    }
    @Inject(method = "getFlyingSpeed", at = @At(value = "RETURN"), cancellable = true)
    protected void getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if (level().isClientSide && !FactoryAPIClient.hasModOnServer) return;
        cir.setReturnValue(cir.getReturnValueF() * ( getAbilities().flying ? (isSprinting() ? level().isClientSide ? Math.max(10,Math.min(Legacy4JClient.getEffectiveRenderDistance(),18)) * 0.6f : 6 : 2) : 1));
    }
    @Inject(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;bob:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    public void aiStep(CallbackInfo ci) {
        handleYBobbing();
    }
}
