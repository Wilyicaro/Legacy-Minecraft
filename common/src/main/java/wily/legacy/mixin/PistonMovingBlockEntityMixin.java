package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin implements LegacyPistonMovingBlockEntity {
    @Unique
    CompoundTag movedBeTag;
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER))
    private static void tick(Level level, BlockPos blockPos, BlockState blockState, PistonMovingBlockEntity pistonMovingBlockEntity, CallbackInfo ci) {
        if (pistonMovingBlockEntity instanceof LegacyPistonMovingBlockEntity be) be.load();
    }

    @Override
    public CompoundTag getMovedBlockEntityTag() {
        return movedBeTag;
    }

    @Override
    public void setMovedBlockEntityTag(CompoundTag tag) {
        this.movedBeTag = tag;
    }
}
