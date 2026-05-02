package wily.legacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

import java.util.*;
import java.util.function.Predicate;

public class LegacyItemUtil {
    public static final String DECAY_ARROW_NAME = "decay";
    public static final int DECAY_EFFECT_DURATION = 800;
    public static final int DECAY_EFFECT_AMPLIFIER = 1;
    public static final TagKey<Item> LCE_OFFHAND = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("legacy", "lce_offhand"));

    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient) {
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }

    public static boolean isDyedItem(ItemStack itemStack) {
        return itemStack.get(DataComponents.DYED_COLOR) == null;
    }

    public static boolean isDyeableItem(Holder<Item> item) {
        return item.is(ItemTags.CAULDRON_CAN_REMOVE_DYE);
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

    public static boolean canGoInLceOffhand(ItemStack stack) {
        return stack.isEmpty() || stack.is(LCE_OFFHAND) || canGoInLocalOffhand(stack);
    }

    private static boolean canGoInLocalOffhand(ItemStack stack) {
        return stack.is(ItemTags.ARROWS)
                || stack.is(Items.FIREWORK_ROCKET)
                || stack.is(Items.FILLED_MAP)
                || stack.is(Items.NAUTILUS_SHELL)
                || stack.is(Items.SHIELD)
                || stack.is(Items.TOTEM_OF_UNDYING);
    }

    public static Holder<Potion> getPotionContent(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElse(null);
    }

    public static ItemStack setItemStackPotion(ItemStack stack, Holder<Potion> potion) {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    public static PotionContents createDecayPotionContents() {
        return new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(MobEffects.WITHER, DECAY_EFFECT_DURATION, DECAY_EFFECT_AMPLIFIER)), Optional.of(DECAY_ARROW_NAME));
    }

    public static ItemStack createDecayPotion(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, createDecayPotionContents());
        return stack;
    }

    public static ItemStack createDecayTippedArrow() {
        return createDecayPotion(Items.TIPPED_ARROW);
    }

    public static boolean isDecayPotionItem(ItemStack stack) {
        if (!stack.has(DataComponents.POTION_CONTENTS)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customName().filter(DECAY_ARROW_NAME::equals).isPresent();
    }

    public static void addPotionTooltip(Holder<Potion> potion, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/) {
        PotionContents.addPotionTooltip(potion.value().getEffects(), tooltipList::add, f, tickRate);
    }

    public static void addPotionTooltip(PotionContents potionContents, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/) {
        PotionContents.addPotionTooltip(potionContents.getAllEffects(), tooltipList::add, f, tickRate);
    }

    public static int getDyeColor(DyeColor dyeColor) {
        return dyeColor.getTextureDiffuseColor();
    }

    public static int getDyeColor(DyeItem dye) {
        return getDyeColor(getDyeColor(dye.asItem()));
    }

    public static DyeColor getDyeColor(Item item) {
        if (item == Items.WHITE_DYE) return DyeColor.WHITE;
        if (item == Items.ORANGE_DYE) return DyeColor.ORANGE;
        if (item == Items.MAGENTA_DYE) return DyeColor.MAGENTA;
        if (item == Items.LIGHT_BLUE_DYE) return DyeColor.LIGHT_BLUE;
        if (item == Items.YELLOW_DYE) return DyeColor.YELLOW;
        if (item == Items.LIME_DYE) return DyeColor.LIME;
        if (item == Items.PINK_DYE) return DyeColor.PINK;
        if (item == Items.GRAY_DYE) return DyeColor.GRAY;
        if (item == Items.LIGHT_GRAY_DYE) return DyeColor.LIGHT_GRAY;
        if (item == Items.CYAN_DYE) return DyeColor.CYAN;
        if (item == Items.PURPLE_DYE) return DyeColor.PURPLE;
        if (item == Items.BLUE_DYE) return DyeColor.BLUE;
        if (item == Items.BROWN_DYE) return DyeColor.BROWN;
        if (item == Items.GREEN_DYE) return DyeColor.GREEN;
        if (item == Items.RED_DYE) return DyeColor.RED;
        return DyeColor.BLACK;
    }

    public static Item getDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }

    public static Item getBannerItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_BANNER;
            case ORANGE -> Items.ORANGE_BANNER;
            case MAGENTA -> Items.MAGENTA_BANNER;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_BANNER;
            case YELLOW -> Items.YELLOW_BANNER;
            case LIME -> Items.LIME_BANNER;
            case PINK -> Items.PINK_BANNER;
            case GRAY -> Items.GRAY_BANNER;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_BANNER;
            case CYAN -> Items.CYAN_BANNER;
            case PURPLE -> Items.PURPLE_BANNER;
            case BLUE -> Items.BLUE_BANNER;
            case BROWN -> Items.BROWN_BANNER;
            case GREEN -> Items.GREEN_BANNER;
            case RED -> Items.RED_BANNER;
            case BLACK -> Items.BLACK_BANNER;
        };
    }

    public static int getPotionLevel(ItemStack stack) {
        PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Holder<Potion> potion = potionContents.potion().orElse(null);
        if (potion == null) {
            return potionContents.customEffects().stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(-1) + 1;
        }
        int level;
        if (potion instanceof Holder.Reference<Potion> reference && reference.key().identifier().getPath().startsWith("strong_")) {
            level = 2;
        } else if (potion instanceof Holder.Reference<Potion> reference && reference.key().identifier().getPath().startsWith("long_")) {
            level = 3;
        } else if (!potionContents.customEffects().isEmpty()) {
            level = potionContents.customEffects().stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(0) + 1;
        } else if (potion.value().getEffects().isEmpty()) {
            level = 0;
        } else {
            level = 1;
        }
        return level;
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

    public static boolean isSkullItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && (blockItem.getBlock() instanceof AbstractSkullBlock || blockItem.getBlock() == Blocks.PUMPKIN || blockItem.getBlock() == Blocks.CARVED_PUMPKIN || blockItem.getBlock() == Blocks.JACK_O_LANTERN);
    }

    public static List<Component> sanitizeTooltip(ItemStack stack, List<Component> tooltip) {
        if (!isSkullItem(stack) || tooltip.size() < 2) return tooltip;
        return List.of(tooltip.getFirst());
    }
}
