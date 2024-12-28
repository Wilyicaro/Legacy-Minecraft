package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MapItem.class)
public class MapItemMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getMapColor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/MapColor;", ordinal = 3))
    public MapColor update(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
        return blockGetter instanceof CollisionGetter c && !c.getWorldBorder().isWithinBounds(blockPos) ? MapColor.NONE : instance.getMapColor(blockGetter,blockPos);
    }
}
