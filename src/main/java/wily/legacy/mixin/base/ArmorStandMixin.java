package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.ArmorStandPose;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin extends LivingEntity {
    @Shadow protected abstract byte setBit(byte b, int i, boolean bl);

    protected int lastSignal = 0;

    protected ArmorStandMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    public void interactAt(Player player, Vec3 vec3, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isShiftKeyDown()){
            cir.setReturnValue(InteractionResult.SUCCESS);
            if (level().isClientSide) return;
            ArmorStandPose.getNextPose(((ArmorStand)(Object)this)).applyPose((ArmorStand)(Object)this);
        }
    }
    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        if (level().isClientSide) return;

        int signal;
        if (!level().isClientSide){
            BlockPos onPos = getOnPos();
            for (Direction dir : Direction.values()) {
                if ((signal = level().getSignal(onPos.relative(dir),dir)) > 0){
                    if (lastSignal != signal) ArmorStandPose.getActualPose(lastSignal = signal).applyPose((ArmorStand)(Object)this);
                    return;
                }
            }
            lastSignal = 0;
        }
    }
    //? if <1.20.5 {
    /*@Redirect(method = "defineSynchedData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", ordinal = 0))
    public void defineSynchedData(SynchedEntityData instance, EntityDataAccessor<Object> entityDataAccessor, Object object) {
        instance.define(entityDataAccessor,this.setBit((byte)object, 4, !(level() instanceof ServerLevel l) || l.getGameRules().getBoolean(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS)));
    }
    *///?} else {
    @Redirect(method = "defineSynchedData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData$Builder;define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)Lnet/minecraft/network/syncher/SynchedEntityData$Builder;", ordinal = 0))
    public SynchedEntityData.Builder defineSynchedData(SynchedEntityData.Builder instance, EntityDataAccessor<Object> i, Object arg) {
        return instance.define(i,this.setBit((byte)arg, 4, !(level() instanceof ServerLevel l) || l.getGameRules().getBoolean(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS)));
    }
    //?}
}
