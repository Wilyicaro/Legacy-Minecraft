package wily.legacy.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EmptyMapItem.class)
public class EmptyMapItemMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/world/level/Level;IIBZZ)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack use(Level level, int arg, int i, byte j, boolean b, boolean bl, Level level1, Player player, InteractionHand interactionHand) {
        ItemStack map = player.getItemInHand(interactionHand);
        ItemStack filledMap = MapItem.create(level, arg, i, j, b, bl);
        if (map.get(DataComponents.MAP_POST_PROCESSING) != null){
            for (int k = 0; k < 3; k++) {
                filledMap.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
                filledMap.onCraftedBy(level,player,1);
            }
        }
        return filledMap;
    }
}
