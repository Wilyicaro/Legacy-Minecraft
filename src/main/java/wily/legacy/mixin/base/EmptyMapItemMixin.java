package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
//? if >=1.20.5 {
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.init.LegacyGameRules;

@Mixin(EmptyMapItem.class)
public class EmptyMapItemMixin {
    //? if <1.20.5 {
    /*@Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/world/level/Level;IIBZZ)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack use(Level level, int arg, int i, byte j, boolean b, boolean bl, Level level1, Player player, InteractionHand interactionHand) {
        ItemStack map = player.getItemInHand(interactionHand);
        ItemStack filledMap = MapItem.create(level, arg, i, (byte) level.getGameRules().getInt(LegacyGameRules.DEFAULT_MAP_SIZE), b, bl);
        if (map.getOrCreateTag().getInt(MapItem.MAP_SCALE_TAG) != 0){
            filledMap.getOrCreateTag().putInt(MapItem.MAP_SCALE_TAG,map.getOrCreateTag().getInt(MapItem.MAP_SCALE_TAG));
            filledMap.onCraftedBy(level,player,1);
        }
        return filledMap;
    }
    *///?} else {
    @Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    public void useConsume(ItemStack instance, int i, LivingEntity arg) {

    }
    @Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/world/level/Level;IIBZZ)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack use(Level level, int arg, int i, byte j, boolean b, boolean bl, Level level1, Player player, InteractionHand interactionHand) {
        ItemStack map = player.getItemInHand(interactionHand);
        CompoundTag custom = map.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        map.consume(1,player);
        return MapItem.create(level, arg, i, custom.contains("map_scale") ? custom.getByte("map_scale") : (byte) ((ServerLevel)level).getGameRules().getInt(LegacyGameRules.DEFAULT_MAP_SIZE), b, bl);
    }
    //?}
}
