package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    private static final AABB fallingBlockDetectBounding = new AABB(-50, -50, -50, 50, 50, 50);
    @Shadow public int time;

    @Inject(method = "fall", at = @At("HEAD"), cancellable = true)
    private static void fall(Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<FallingBlockEntity> cir) {
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().getRule(LegacyGameRules.FALLING_BLOCK_LIMIT).get() > 0 && level.getEntitiesOfClass(FallingBlockEntity.class, fallingBlockDetectBounding.move(blockPos)).size() >= serverLevel.getGameRules().getRule(LegacyGameRules.FALLING_BLOCK_LIMIT).get()) {
            serverLevel.scheduleTick(blockPos, blockState.getBlock(), 2);
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z"), cancellable = true)
    private void tick(CallbackInfo ci) {
        FallingBlockEntity fallingBlock = (FallingBlockEntity) (Object) this;
        Entity entity = (Entity) (Object) this;
        if (!entity.onGround()) return;
        BlockPos blockPos = entity.blockPosition().below();
        Level level = entity.level();
        if (fallingBlock.getBlockState().getBlock() instanceof ConcretePowderBlock && (level.getFluidState(entity.blockPosition()).is(FluidTags.WATER) || level.getFluidState(blockPos).is(FluidTags.WATER))) return;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir() || Block.isFaceFull(blockState.getCollisionShape(level, blockPos), Direction.UP)) return;
        time = 0;
        entity.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }
}
