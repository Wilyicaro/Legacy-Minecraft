package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.2 {
import net.minecraft.world.level.redstone.Orientation;
//?}
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacyGameRules;

@Mixin(TntBlock.class)
public class TntBlockMixin {
    private static final AABB tntDetectBounding = new AABB(-50, -50, -50, 50, 50, 50);

    //? if <1.21.5 {
    /*@Inject(method = "explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void explode(Level level, BlockPos blockPos, LivingEntity livingEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && (!serverLevel.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES) || serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() > 0 && level.getEntitiesOfClass(PrimedTnt.class,tntDetectBounding.move(blockPos)).size() >= serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get())) ci.cancel();
    }
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block,/^? if >=1.21.2 {^/Orientation orientation/^?} else {^//^BlockPos blockPos2^//^?}^/, boolean bl, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && !serverLevel.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) ci.cancel();
    }
    *///?} else if <1.21.11 {
    /*
    @ModifyExpressionValue(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private static boolean explode(boolean original, Level level, BlockPos pos) {
        return original && level instanceof ServerLevel serverLevel && (serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() == 0 || level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(pos)).size() < serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get());
    }
    *///?} else {
    @ModifyExpressionValue(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private static Object explode(Object original, Level level, BlockPos pos) {
        if (!(original instanceof Boolean))
            return original;
        boolean originalValue = (Boolean) original;

        if (!originalValue)
            return false;

        if (level instanceof ServerLevel serverLevel) {
            Integer tntLimit = serverLevel.getGameRules().get(LegacyGameRules.TNT_LIMIT);
            if (tntLimit > 0
                    && level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(pos)).size() >= tntLimit) {
                return false;
            }
        }
        
        return true;
    }

    //?}
    @Inject(method = "wasExploded", at = @At("HEAD"), cancellable = true)
    private void wasExploded(/*? if <1.21.2 {*//*Level*//*?} else {*/ServerLevel/*?}*/ level, BlockPos blockPos, Explosion explosion, CallbackInfo ci) {
        //? <1.21.5 {
        /*
        if (!level.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES) || level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() > 0 && level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(blockPos)).size() >= level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get())
        *///?} else if <1.21.11{
        /*
        if (level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() > 0 && level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(blockPos)).size() >= level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get())
        *///?} else {
        if (level.getGameRules().get(LegacyGameRules.TNT_LIMIT) > 0 && level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(blockPos)).size() >= level.getGameRules().get(LegacyGameRules.TNT_LIMIT))
        //?}
            ci.cancel();
    }
}
