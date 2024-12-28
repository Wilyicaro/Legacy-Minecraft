package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;
import wily.legacy.util.LegacyTags;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {
    @Redirect(method = "isPushable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
    private static boolean isPushable(BlockState instance) {
        return instance.hasBlockEntity() && !instance.is(LegacyTags.PUSHABLE_BLOCK);
    }
    @Redirect(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;", ordinal = 0))
    private BlockEntity moveBlocksPistonHead(BlockPos arg, BlockState arg2, BlockState arg3, Direction arg4, boolean bl, boolean bl2, Level level) {
        PistonMovingBlockEntity movingBe = new PistonMovingBlockEntity(arg,arg2,arg3,arg4,bl,bl2);
        if (movingBe instanceof LegacyPistonMovingBlockEntity e && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(arg.relative(bl ? arg4.getOpposite() : arg4));
            e.setMovedBlockEntityTag(be == null ? null : be./*? if <1.20.5 {*//*saveWithoutMetadata()*//*?} else {*/saveCustomOnly(level.registryAccess())/*?}*/);
            if (be instanceof RandomizableContainerBlockEntity r) r.unpackLootTable(null);
            Clearable.tryClear(be);
        }
        return movingBe;
    }
}
