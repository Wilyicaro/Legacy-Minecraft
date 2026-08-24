package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyOptions;

@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {
    @ModifyExpressionValue(method = "getStyledHoverName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getRarity()Lnet/minecraft/world/item/Rarity;"))
    private Rarity getLegacyItemRarity(Rarity original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!LegacyOptions.legacyItemRarity.get() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")) return original;
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return Rarity.EPIC;
        if (stack.is(Items.GOLDEN_APPLE) || stack.has(DataComponents.JUKEBOX_PLAYABLE)) return Rarity.RARE;
        if (stack.is(Items.ENCHANTED_BOOK)) return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() ? Rarity.UNCOMMON : Rarity.RARE;
        return stack.isEnchanted() ? Rarity.RARE : Rarity.COMMON;
    }
}
