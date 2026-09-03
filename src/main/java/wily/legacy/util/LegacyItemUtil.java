package wily.legacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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
import net.minecraft.world.level.material.Fluid;
import wily.factoryapi.ItemContainerPlatform;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

import java.util.*;
import java.util.function.Predicate;

public class LegacyItemUtil {
    public static final String DECAY_ARROW_NAME = "decay";
    public static final int DECAY_EFFECT_DURATION = 800;
    public static final int DECAY_EFFECT_AMPLIFIER = 1;
    public static final TagKey<Item> LCE_OFFHAND = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("legacy", "lce_offhand"));
    private static final Set<String> PEACEFUL_SPAWN_EGG_TIPS = Set.of("item.spawn_egg.peaceful", "item.minecraft.spawn_egg.peaceful.tip");

    public static Fluid getBucketFluid(BucketItem bucket) {
        return /*? if forge {*/ /*bucket.getFluid()*//*?} else {*/ItemContainerPlatform.getBucketFluid(bucket)/*?}*/;
    }

    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient) {
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }

    public static boolean isDyedItem(ItemStack itemStack) {
        return itemStack.has(DataComponents.DYED_COLOR);
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

    public static boolean isWaterBottle(ItemStack itemStack) {
        return getNonNullPotionContents(itemStack).is(Potions.WATER);
    }

    public static PotionContents getNonNullPotionContents(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    public static PotionContents getPotionContents(ItemStack itemStack) {
        PotionContents contents = getNonNullPotionContents(itemStack);
        return contents.potion().isPresent() || contents.hasEffects() ? contents : null;
    }

    public static ItemStack setItemStackPotion(ItemStack stack, Holder<Potion> potion) {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    public static ItemStack setItemStackPotion(ItemStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
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
        DyeColor color = getDyeColorOrNull(item);
        return color == null ? DyeColor.BLACK : color;
    }

    public static DyeColor getDyeColorOrNull(Item item) {
        if (item == Items.BONE_MEAL) return DyeColor.WHITE;
        if (item == Items.INK_SAC) return DyeColor.BLACK;
        if (item == Items.LAPIS_LAZULI) return DyeColor.BLUE;
        if (item == Items.COCOA_BEANS) return DyeColor.BROWN;
        if (item == Items./*? if >=26.2 {*/DYE.white()/*?} else {*//*WHITE_DYE*//*?}*/) return DyeColor.WHITE;
        if (item == Items./*? if >=26.2 {*/DYE.orange()/*?} else {*//*ORANGE_DYE*//*?}*/) return DyeColor.ORANGE;
        if (item == Items./*? if >=26.2 {*/DYE.magenta()/*?} else {*//*MAGENTA_DYE*//*?}*/) return DyeColor.MAGENTA;
        if (item == Items./*? if >=26.2 {*/DYE.lightBlue()/*?} else {*//*LIGHT_BLUE_DYE*//*?}*/) return DyeColor.LIGHT_BLUE;
        if (item == Items./*? if >=26.2 {*/DYE.yellow()/*?} else {*//*YELLOW_DYE*//*?}*/) return DyeColor.YELLOW;
        if (item == Items./*? if >=26.2 {*/DYE.lime()/*?} else {*//*LIME_DYE*//*?}*/) return DyeColor.LIME;
        if (item == Items./*? if >=26.2 {*/DYE.pink()/*?} else {*//*PINK_DYE*//*?}*/) return DyeColor.PINK;
        if (item == Items./*? if >=26.2 {*/DYE.gray()/*?} else {*//*GRAY_DYE*//*?}*/) return DyeColor.GRAY;
        if (item == Items./*? if >=26.2 {*/DYE.lightGray()/*?} else {*//*LIGHT_GRAY_DYE*//*?}*/) return DyeColor.LIGHT_GRAY;
        if (item == Items./*? if >=26.2 {*/DYE.cyan()/*?} else {*//*CYAN_DYE*//*?}*/) return DyeColor.CYAN;
        if (item == Items./*? if >=26.2 {*/DYE.purple()/*?} else {*//*PURPLE_DYE*//*?}*/) return DyeColor.PURPLE;
        if (item == Items./*? if >=26.2 {*/DYE.blue()/*?} else {*//*BLUE_DYE*//*?}*/) return DyeColor.BLUE;
        if (item == Items./*? if >=26.2 {*/DYE.brown()/*?} else {*//*BROWN_DYE*//*?}*/) return DyeColor.BROWN;
        if (item == Items./*? if >=26.2 {*/DYE.green()/*?} else {*//*GREEN_DYE*//*?}*/) return DyeColor.GREEN;
        if (item == Items./*? if >=26.2 {*/DYE.red()/*?} else {*//*RED_DYE*//*?}*/) return DyeColor.RED;
        if (item == Items./*? if >=26.2 {*/DYE.black()/*?} else {*//*BLACK_DYE*//*?}*/) return DyeColor.BLACK;
        return null;
    }

    public static Item getLegacyDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.BONE_MEAL;
            case BLACK -> Items.INK_SAC;
            case BLUE -> Items.LAPIS_LAZULI;
            case BROWN -> Items.COCOA_BEANS;
            default -> null;
        };
    }

    public static Item getDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items./*? if >=26.2 {*/DYE.white()/*?} else {*//*WHITE_DYE*//*?}*/;
            case ORANGE -> Items./*? if >=26.2 {*/DYE.orange()/*?} else {*//*ORANGE_DYE*//*?}*/;
            case MAGENTA -> Items./*? if >=26.2 {*/DYE.magenta()/*?} else {*//*MAGENTA_DYE*//*?}*/;
            case LIGHT_BLUE -> Items./*? if >=26.2 {*/DYE.lightBlue()/*?} else {*//*LIGHT_BLUE_DYE*//*?}*/;
            case YELLOW -> Items./*? if >=26.2 {*/DYE.yellow()/*?} else {*//*YELLOW_DYE*//*?}*/;
            case LIME -> Items./*? if >=26.2 {*/DYE.lime()/*?} else {*//*LIME_DYE*//*?}*/;
            case PINK -> Items./*? if >=26.2 {*/DYE.pink()/*?} else {*//*PINK_DYE*//*?}*/;
            case GRAY -> Items./*? if >=26.2 {*/DYE.gray()/*?} else {*//*GRAY_DYE*//*?}*/;
            case LIGHT_GRAY -> Items./*? if >=26.2 {*/DYE.lightGray()/*?} else {*//*LIGHT_GRAY_DYE*//*?}*/;
            case CYAN -> Items./*? if >=26.2 {*/DYE.cyan()/*?} else {*//*CYAN_DYE*//*?}*/;
            case PURPLE -> Items./*? if >=26.2 {*/DYE.purple()/*?} else {*//*PURPLE_DYE*//*?}*/;
            case BLUE -> Items./*? if >=26.2 {*/DYE.blue()/*?} else {*//*BLUE_DYE*//*?}*/;
            case BROWN -> Items./*? if >=26.2 {*/DYE.brown()/*?} else {*//*BROWN_DYE*//*?}*/;
            case GREEN -> Items./*? if >=26.2 {*/DYE.green()/*?} else {*//*GREEN_DYE*//*?}*/;
            case RED -> Items./*? if >=26.2 {*/DYE.red()/*?} else {*//*RED_DYE*//*?}*/;
            case BLACK -> Items./*? if >=26.2 {*/DYE.black()/*?} else {*//*BLACK_DYE*//*?}*/;
        };
    }

    public static Item getBannerItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items./*? if >=26.2 {*/BANNER.white()/*?} else {*//*WHITE_BANNER*//*?}*/;
            case ORANGE -> Items./*? if >=26.2 {*/BANNER.orange()/*?} else {*//*ORANGE_BANNER*//*?}*/;
            case MAGENTA -> Items./*? if >=26.2 {*/BANNER.magenta()/*?} else {*//*MAGENTA_BANNER*//*?}*/;
            case LIGHT_BLUE -> Items./*? if >=26.2 {*/BANNER.lightBlue()/*?} else {*//*LIGHT_BLUE_BANNER*//*?}*/;
            case YELLOW -> Items./*? if >=26.2 {*/BANNER.yellow()/*?} else {*//*YELLOW_BANNER*//*?}*/;
            case LIME -> Items./*? if >=26.2 {*/BANNER.lime()/*?} else {*//*LIME_BANNER*//*?}*/;
            case PINK -> Items./*? if >=26.2 {*/BANNER.pink()/*?} else {*//*PINK_BANNER*//*?}*/;
            case GRAY -> Items./*? if >=26.2 {*/BANNER.gray()/*?} else {*//*GRAY_BANNER*//*?}*/;
            case LIGHT_GRAY -> Items./*? if >=26.2 {*/BANNER.lightGray()/*?} else {*//*LIGHT_GRAY_BANNER*//*?}*/;
            case CYAN -> Items./*? if >=26.2 {*/BANNER.cyan()/*?} else {*//*CYAN_BANNER*//*?}*/;
            case PURPLE -> Items./*? if >=26.2 {*/BANNER.purple()/*?} else {*//*PURPLE_BANNER*//*?}*/;
            case BLUE -> Items./*? if >=26.2 {*/BANNER.blue()/*?} else {*//*BLUE_BANNER*//*?}*/;
            case BROWN -> Items./*? if >=26.2 {*/BANNER.brown()/*?} else {*//*BROWN_BANNER*//*?}*/;
            case GREEN -> Items./*? if >=26.2 {*/BANNER.green()/*?} else {*//*GREEN_BANNER*//*?}*/;
            case RED -> Items./*? if >=26.2 {*/BANNER.red()/*?} else {*//*RED_BANNER*//*?}*/;
            case BLACK -> Items./*? if >=26.2 {*/BANNER.black()/*?} else {*//*BLACK_BANNER*//*?}*/;
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
        if (tooltip.stream().anyMatch(LegacyItemUtil::isPeacefulSpawnEggTip)) {
            tooltip = tooltip.stream().filter(component -> !isPeacefulSpawnEggTip(component)).toList();
        }
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE) && tooltip.size() > 1) {
            tooltip = new ArrayList<>(tooltip);
            Component name = tooltip.get(0);
            tooltip.set(1, tooltip.get(1).copy().withStyle(style -> style.withColor(name.getStyle().getColor())));
        }
        return tooltip;
    }

    private static boolean isPeacefulSpawnEggTip(Component component) {
        if (component.getContents() instanceof TranslatableContents contents && PEACEFUL_SPAWN_EGG_TIPS.contains(contents.getKey())) {
            return true;
        }
        return component.getSiblings().stream().anyMatch(LegacyItemUtil::isPeacefulSpawnEggTip);
    }
}
