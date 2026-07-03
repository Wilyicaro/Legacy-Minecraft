package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
//? if >=1.20.5 {
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyGameRules;

@Mixin(EmptyMapItem.class)
public class EmptyMapItemMixin {
    //? if <1.20.5 {
    /*@Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/world/level/Level;IIBZZ)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack use(Level level, int arg, int i, byte j, boolean b, boolean bl, Level level1, Player player, InteractionHand interactionHand) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return MapItem.create(level, arg, i, j, b, bl);
        }
        ItemStack map = player.getItemInHand(interactionHand);
        Component name = FactoryItemUtil.hasCustomName(map) ? map.getHoverName() : null;
        byte newScale = map.getOrCreateTag().getByte(MapItem.MAP_SCALE_TAG);
        byte scale = newScale > 0 ? newScale : (byte) level.getGameRules().getInt(LegacyGameRules.DEFAULT_MAP_SIZE);
        ItemStack existingMap = legacy$getExistingMap(level, player, arg, i, scale, b, bl);
        ItemStack result = existingMap.isEmpty() ? MapItem.create(level, arg, i, scale, b, bl) : existingMap.copyWithCount(1);
        if (name != null) FactoryItemUtil.setCustomName(result, name);
        return result;
    }
    *///?} else {
    @Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    public void useConsume(ItemStack instance, int i, LivingEntity arg) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            instance.consume(i, arg);
        }
    }
    @WrapOperation(method = "use", at = @At(value = "INVOKE",target = /*? if <1.21.5 {*/"Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/world/level/Level;IIBZZ)Lnet/minecraft/world/item/ItemStack;"/*?} else {*//*"Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/server/level/ServerLevel;IIBZZ)Lnet/minecraft/world/item/ItemStack;"*//*?}*/))
    public ItemStack use(/*? if <1.21.5 {*/Level/*?} else {*//*ServerLevel*//*?}*/ level, int arg, int i, byte j, boolean b, boolean bl, Operation<ItemStack> original, Level level1, Player player, InteractionHand interactionHand) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return original.call(level, arg, i, j, b, bl);
        }
        ItemStack map = player.getItemInHand(interactionHand);
        Component name = FactoryItemUtil.hasCustomName(map) ? map.getHoverName() : null;
        CompoundTag custom = map.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        byte defaultScale = legacy$getDefaultMapSize(level);
        byte scale = custom.contains("map_scale") ? custom.getByte("map_scale")/*? if >=1.21.5 {*//*.orElse(defaultScale)*//*?}*/ : defaultScale;
        map.consume(1, player);
        ItemStack existingMap = legacy$getExistingMap(level, player, arg, i, scale, b, bl);
        ItemStack result = existingMap.isEmpty() ? original.call(level, arg, i, scale, b, bl) : existingMap.copyWithCount(1);
        if (name != null) FactoryItemUtil.setCustomName(result, name);
        return result;
    }

    //?}

    @Unique
    private byte legacy$getDefaultMapSize(Level level) {
        return (byte) /*? if <1.21.2 {*/level/*?} else {*//*((ServerLevel) level)*//*?}*/.getGameRules().getInt(LegacyGameRules.DEFAULT_MAP_SIZE);
    }

    @Unique
    private ItemStack legacy$getExistingMap(Level level, Player player, int x, int z, byte scale, boolean trackingPosition, boolean unlimitedTracking) {
        MapItemSavedData target = MapItemSavedData.createFresh(x, z, scale, trackingPosition, unlimitedTracking, level.dimension());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack itemStack = player.getInventory().getItem(slot);
            if (!itemStack.is(Items.FILLED_MAP)) continue;
            MapItemSavedData savedData = MapItem.getSavedData(itemStack, level);
            if (savedData == null) continue;
            if (savedData.scale == target.scale && savedData.centerX == target.centerX && savedData.centerZ == target.centerZ && savedData.dimension.equals(target.dimension)) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }
}
