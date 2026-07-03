package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
    private static final AABB DETECT_BOX = new AABB(-50, -50, -50, 50, 50, 50);

    @Inject(method = "fall", at = @At("HEAD"), cancellable = true)
    private static void fall(Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<FallingBlockEntity> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int limit = serverLevel.getGameRules().getRule(LegacyGameRules.FALLING_BLOCK_LIMIT).get();
        if (limit <= 0 || level.getEntitiesOfClass(FallingBlockEntity.class, DETECT_BOX.move(blockPos)).size() < limit) return;
        serverLevel.scheduleTick(blockPos, blockState.getBlock(), 2);
        cir.setReturnValue(null);
    }
}
