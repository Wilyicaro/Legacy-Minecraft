package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.TntBlock;
//? if >=1.21.2 {
//?}
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
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
    *///?} else {
    //? if >=1.21.11 {
    @ModifyExpressionValue(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private static Object explode(Object original, @Local BlockPos pos, @Local ServerLevel serverLevel) {
        return (Boolean) original && (serverLevel.getGameRules().get(LegacyGameRules.TNT_LIMIT) == 0 || serverLevel.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(pos)).size() < serverLevel.getGameRules().get(LegacyGameRules.TNT_LIMIT));
    }
    //?} else {
    /*@ModifyExpressionValue(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private static boolean explode(boolean original) {
        return original && level instanceof ServerLevel serverLevel && (serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() == 0 || level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(pos)).size() < serverLevel.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get());
    }
    *///?}

    //?}
    @Inject(method = "wasExploded", at = @At("HEAD"), cancellable = true)
    private void wasExploded(/*? if <1.21.2 {*//*Level*//*?} else {*/ServerLevel/*?}*/ level, BlockPos blockPos, Explosion explosion, CallbackInfo ci) {
        int tntLimit = level.getGameRules()./*? if >=1.21.11 {*/get(LegacyGameRules.TNT_LIMIT)/*?} else {*//*getRule(LegacyGameRules.TNT_LIMIT).get()*//*?}*/;
        if (/*? if <1.21.5 {*//*!level.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES) ||*//*?}*/tntLimit > 0 && level.getEntitiesOfClass(PrimedTnt.class, tntDetectBounding.move(blockPos)).size() >= tntLimit)
            ci.cancel();
    }
}
