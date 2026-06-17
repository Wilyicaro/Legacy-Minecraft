package wily.legacy.mixin.base;

//? if <1.20.5 {
/*import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MapItem.class)
public class MapItemMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getMapColor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/MapColor;", ordinal = 3))
    public MapColor update(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
        return blockGetter instanceof CollisionGetter c && !c.getWorldBorder().isWithinBounds(blockPos) ? MapColor.NONE : instance.getMapColor(blockGetter,blockPos);
    }

    //? if <1.20.5 {
    /*@Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag, CallbackInfo ci) {
        MapItemSavedData data = level == null ? null : MapItem.getSavedData(itemStack, level);
        if (data == null) {
            list.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
            ci.cancel();
            return;
        }
        if (data.locked) {
            list.add(Component.translatable("filled_map.locked").withStyle(ChatFormatting.GRAY));
        }
        list.add(Component.translatable("legacy.map.level", data.scale, 4).withStyle(ChatFormatting.GRAY));
        ci.cancel();
    }
    *///?}
}
