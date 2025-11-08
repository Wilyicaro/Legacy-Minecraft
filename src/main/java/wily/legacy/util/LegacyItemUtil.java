package wily.legacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class LegacyItemUtil {
    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient) {
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }

    public static boolean isDyedItem(ItemStack itemStack) {
        return itemStack.get(DataComponents.DYED_COLOR) == null;
    }

    public static boolean isDyeableItem(Holder<Item> item) {
        return item.is(ItemTags.DYEABLE);
    }

    public static ItemStack dyeItem(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();
        DyedItemColor dyedItemColor = itemStack.get(DataComponents.DYED_COLOR);
        if (dyedItemColor != null) colors.add(color);
        colors.add(color);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(mixColors(colors.iterator())));
        return itemStack;
    }

    public static int mixColors(Iterator<Integer> colors) {
        int n;
        float h;
        int[] is = new int[3];
        int i = 0;
        int j = 0;

        for (Iterator<Integer> it = colors; it.hasNext(); ) {
            Integer color = it.next();
            float f = (float) (color >> 16 & 0xFF) / 255.0f;
            float g = (float) (color >> 8 & 0xFF) / 255.0f;
            h = (float) (color & 0xFF) / 255.0f;
            i += (int) (Math.max(f, Math.max(g, h)) * 255.0f);
            is[0] = is[0] + (int) (f * 255.0f);
            is[1] = is[1] + (int) (g * 255.0f);
            is[2] = is[2] + (int) (h * 255.0f);
            ++j;
        }
        int k = is[0] / j;
        int o = is[1] / j;
        int p = is[2] / j;
        h = (float) i / (float) j;
        float q = Math.max(k, Math.max(o, p));
        k = (int) ((float) k * h / q);
        o = (int) ((float) o * h / q);
        p = (int) ((float) p * h / q);
        n = k;
        n = (n << 8) + o;
        n = (n << 8) + p;
        return n;
    }

    public static boolean hasValidPatterns(ItemStack stack) {
        int count = getPatternsCount(stack);
        return count > 0 && count <= 6;
    }

    public static int getPatternsCount(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
    }

    public static boolean anyArmorSlotMatch(Inventory inventory, Predicate<ItemStack> predicate) {
        return Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream().anyMatch(e -> e.getValue() != EquipmentSlot.OFFHAND && predicate.test(inventory.getItem(e.getIntKey())));
    }

    public static Holder<Potion> getPotionContent(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElse(null);
    }

    public static ItemStack setItemStackPotion(ItemStack stack, Holder<Potion> potion) {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    public static void addPotionTooltip(Holder<Potion> potion, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/) {
        PotionContents.addPotionTooltip(potion.value().getEffects(), tooltipList::add, f, tickRate);
    }

    public static int getDyeColor(DyeColor dyeColor) {
        return dyeColor.getTextureDiffuseColor();
    }

    public static float getItemDamageModifier(ItemStack stack) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)) {
            if (stack.is(ItemTags.SWORDS)) return 1;
            else if (stack.getItem() instanceof ShovelItem) return -0.5f;
            else if (stack.is(ItemTags.PICKAXES)) return 1;
            else if (stack.getItem() instanceof net.minecraft.world.item.AxeItem) {
                if (stack.is(Items.STONE_AXE)) return -4;
                else if (stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)) return -2;
                else return -3;
            }
        }
        return 0;
    }
}
