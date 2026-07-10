package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;
import wily.legacy.mobcaps.LegacyMobCaps;
import wily.legacy.network.ClientAnimalInLoveSyncPayload;

@Mixin(Animal.class)
public abstract class AnimalMixin extends AgeableMob {
    @Unique
    private static final int legacy$maxWanderDistance = 20;
    @Unique
    private static final int legacy$despawnCheckInterval = 20;
    @Unique
    int lastInlove = 0;
    @Unique
    private BlockPos legacy$protectedPos;
    @Unique
    private boolean legacy$despawnable;
    @Shadow
    private int inLove;

    protected AnimalMixin(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract void setInLoveTime(int i);
    @Shadow
    public abstract boolean isFood(ItemStack itemStack);

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/animal/Animal;inLove:I", opcode = Opcodes.PUTFIELD))
    public void aiStep(Animal instance, int value) {
        if (!level().isClientSide()) setInLoveTime(value);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void aiStep(CallbackInfo ci) {
        if (lastInlove != inLove) ClientAnimalInLoveSyncPayload.sync((Animal) (Object) this);
        lastInlove = inLove;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    public void aiStepTail(CallbackInfo ci) {
        legacy$updateDespawnProtection();
    }

    @Inject(method = "canFallInLove", at = @At("HEAD"), cancellable = true)
    public void aiStep(CallbackInfoReturnable<Boolean> cir) {
        if (age != 0) cir.setReturnValue(false);
    }

    @Inject(method = "setInLove", at = @At("HEAD"), cancellable = true)
    public void setInLove(Player player, CallbackInfo ci) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        String failure = ConsoleMobCaps.breedingFailure(serverLevel, getType());
        if (failure == null) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, failure);
        ci.cancel();
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    public void mobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) legacy$setDespawnProtected();
    }

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    public void removeWhenFarAway(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (level() instanceof ServerLevel serverLevel && LegacyMobCaps.isEnabled(serverLevel) && legacy$despawnable && !legacy$isOwned()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void legacy$updateDespawnProtection() {
        if (!(level() instanceof ServerLevel serverLevel) || !LegacyMobCaps.isEnabled(serverLevel)) return;
        if (legacy$protectedPos != null && (tickCount + getId()) % legacy$despawnCheckInterval != 0) return;
        if (legacy$protectedPos == null || isPersistenceRequired() || requiresCustomPersistence() || legacy$isOwned()) {
            legacy$setDespawnProtected();
            return;
        }
        legacy$protectIfTempted(serverLevel);
        if (legacy$despawnable) return;

        BlockPos pos = blockPosition();
        if (Math.abs(pos.getX() - legacy$protectedPos.getX()) > legacy$maxWanderDistance || Math.abs(pos.getZ() - legacy$protectedPos.getZ()) > legacy$maxWanderDistance) {
            legacy$despawnable = true;
        }
    }

    @Unique
    private void legacy$setDespawnProtected() {
        if (level().isClientSide()) return;
        legacy$protectedPos = blockPosition();
        legacy$despawnable = false;
    }

    @Unique
    private boolean legacy$isOwned() {
        return this instanceof OwnableEntity owned && owned.getOwnerReference() != null;
    }

    @Unique
    private void legacy$protectIfTempted(ServerLevel level) {
        Player player = level.getNearestPlayer(this, 10.0D);
        if (player != null && (isFood(player.getMainHandItem()) || isFood(player.getOffhandItem()))) {
            legacy$setDespawnProtected();
        }
    }
}
