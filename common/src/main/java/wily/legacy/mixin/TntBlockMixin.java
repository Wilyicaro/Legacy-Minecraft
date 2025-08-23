package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacyGameRules;

@Mixin(TntBlock.class)
public class TntBlockMixin {
    private static final AABB tntDetectBounding = new AABB(-50,-50,-50,50,50,50);

    @Inject(method = "explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void explode(Level level, BlockPos blockPos, LivingEntity livingEntity, CallbackInfo ci) {
        if (!level.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES) || level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() > 0 && level.getEntitiesOfClass(PrimedTnt.class,tntDetectBounding.move(blockPos)).size() >= level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get()) ci.cancel();
    }
    @Inject(method = "wasExploded", at = @At("HEAD"), cancellable = true)
    private void wasExploded(Level level, BlockPos blockPos, Explosion explosion, CallbackInfo ci) {
        if (!level.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES) || level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get() > 0 && level.getEntitiesOfClass(PrimedTnt.class,tntDetectBounding.move(blockPos)).size() >= level.getGameRules().getRule(LegacyGameRules.TNT_LIMIT).get()) ci.cancel();
    }
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl, CallbackInfo ci) {
        if (!level.getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES)) ci.cancel();
    }
}
