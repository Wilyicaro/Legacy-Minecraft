package wily.legacy.util;

import net.minecraft.core.Holder;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
//? if >=1.20.5 {
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
//? if <1.21.4 {
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomModelData;
//?}
//?} else {
/*import net.minecraft.world.item.alchemy.PotionUtils;
import wily.factoryapi.util.ColorUtil;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.util.ItemAccessor;
*///?}
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import wily.factoryapi.ItemContainerPlatform;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;

import java.util.*;
import java.util.function.Predicate;

public class LegacyItemUtil {
    public static final String DECAY_POTION_NAME = "decay";
    public static final int DECAY_EFFECT_DURATION = 800;
    public static final int DECAY_EFFECT_AMPLIFIER = 1;
    public static final TagKey<Item> LCE_OFFHAND = TagKey.create(Registries.ITEM, Legacy4J.createModLocation("lce_offhand"));
    private static final Set<String> PEACEFUL_SPAWN_EGG_TIPS = Set.of("item.spawn_egg.peaceful", "item.minecraft.spawn_egg.peaceful.tip");
    public static final String MUSHROOM_PORE_NAME = "block.legacy.mushroom_pore";
    public static final int MUSHROOM_PORE_MODEL_DATA = 421001;

    public static Fluid getBucketFluid(BucketItem bucket) {
        return /*? if forge {*/ /*bucket.getFluid()*//*?} else {*/ItemContainerPlatform.getBucketFluid(bucket)/*?}*/;
    }

    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient) {
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && /*? if <1.20.5 {*//*repairItem.getItem().canBeDepleted()*//*?} else {*/repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted()/*?}*/ && !ingredient.isEnchanted();
    }

    public static boolean isDyedItem(ItemStack itemStack) {
        return /*? if <1.20.5 {*//*((DyeableLeatherItem)itemStack.getItem()).hasCustomColor(itemStack) *//*?} else {*/itemStack.has(DataComponents.DYED_COLOR)/*?}*/;
    }

    public static boolean isDyeableItem(Holder<Item> item) {
        return /*? if <1.20.5 {*//*(item.value() instanceof DyeableLeatherItem)*//*?} else {*/item.is(ItemTags.DYEABLE)/*?}*/;
    }

    public static ItemStack dyeItem(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();
        //? if <1.20.5 {
        /*DyeableLeatherItem dyeableLeatherItem = null;
        Item item = itemStack.getItem();
        if (item instanceof DyeableLeatherItem) {
            dyeableLeatherItem = (DyeableLeatherItem) item;
            if (dyeableLeatherItem.hasCustomColor(itemStack)) colors.add(dyeableLeatherItem.getColor(itemStack));
            colors.add(color);
        }
        if (dyeableLeatherItem != null) dyeableLeatherItem.setColor(itemStack, mixColors(colors.iterator()));
        *///?} else {
        DyedItemColor dyedItemColor = itemStack.get(DataComponents.DYED_COLOR);
        /*? if <1.21.5 {*/boolean bl = dyedItemColor == null || dyedItemColor.showInTooltip();/*?}*/
        if (dyedItemColor != null) colors.add(color);
        colors.add(color);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(mixColors(colors.iterator())/*? if <1.21.5 {*/, bl/*?}*/));
        //?}
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
        //? if <1.20.5 {
        /*CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        return beTag == null ? 0 : beTag.contains("Patterns") ? beTag.getList("Patterns",10).size() : -1;
        *///?} else
        return stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
    }

    public static boolean anyArmorSlotMatch(Inventory inventory, Predicate<ItemStack> predicate){
        //? if <1.21.5 {
        return inventory.armor.stream().anyMatch(predicate);
        //?} else {
        /*return Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream().anyMatch(e-> e.getValue() != EquipmentSlot.OFFHAND && predicate.test(inventory.getItem(e.getIntKey())));
         *///?}
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

    public static Holder<Potion> getPotionContent(ItemStack itemStack){
        //? if <1.20.5 {
        /*Holder<Potion> potion = BuiltInRegistries.POTION.wrapAsHolder(PotionUtils.getPotion(itemStack));
        return potion.value() == Potions.EMPTY ? null : potion;
        *///?} else {
        return itemStack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().orElse(null);
        //?}
    }

    public static ItemStack setItemStackPotion(ItemStack stack, Holder<Potion> potion){
        //? if <1.20.5 {
        /*return PotionUtils.setPotion(stack, potion.value());
         *///?} else {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
        //?}
    }

    //? if >=1.20.5 {
    public static PotionContents createDecayPotionContents() {
        return new PotionContents(Optional.empty(), Optional.empty(), createDecayEffects()/*? if >=1.21.3 {*//*, Optional.of(DECAY_POTION_NAME)*//*?}*/);
    }
    //?}

    public static ItemStack createDecayPotion(Item item) {
        return createDecayPotion(new ItemStack(item));
    }

    public static ItemStack createDecayPotion(ItemStack stack) {
        //? if <1.20.5 {
        /*List<MobEffectInstance> effects = createDecayEffects();
        PotionUtils.setCustomEffects(stack, effects);
        stack.getOrCreateTag().putInt("CustomPotionColor", PotionUtils.getColor(effects));
        return stack;
        *///?} else {
        stack.set(DataComponents.POTION_CONTENTS, createDecayPotionContents());
        return stack;
        //?}
    }

    public static List<MobEffectInstance> createDecayEffects() {
        return List.of(new MobEffectInstance(MobEffects.WITHER, DECAY_EFFECT_DURATION, DECAY_EFFECT_AMPLIFIER));
    }

    public static ItemStack createDecayTippedArrow() {
        return createDecayTippedArrow(1);
    }

    public static ItemStack createDecayTippedArrow(int count) {
        return createDecayPotion(new ItemStack(Items.TIPPED_ARROW, count));
    }

    public static String getDecayPotionDescriptionId(ItemStack stack) {
        if (!isDecayPotionItem(stack)) return null;
        if (stack.is(Items.TIPPED_ARROW)) return "item.minecraft.tipped_arrow.effect." + DECAY_POTION_NAME;
        if (stack.is(Items.SPLASH_POTION)) return "item.minecraft.splash_potion.effect." + DECAY_POTION_NAME;
        if (stack.is(Items.LINGERING_POTION)) return "item.minecraft.lingering_potion.effect." + DECAY_POTION_NAME;
        if (stack.is(Items.POTION)) return "item.minecraft.potion.effect." + DECAY_POTION_NAME;
        return null;
    }

    public static boolean isDecayPotionItem(ItemStack stack) {
        //? if <1.20.5 {
        /*return PotionUtils.getPotion(stack) == Potions.EMPTY && isDecayEffectList(PotionUtils.getCustomEffects(stack));
         *///?} else {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.potion().isPresent()) return false;
        //? if >=1.21.3 {
        /*if (contents.customName().filter(DECAY_POTION_NAME::equals).isPresent()) return true;
         *///?}
        return isDecayEffectList(contents.customEffects());
        //?}
    }

    private static boolean isDecayEffectList(List<MobEffectInstance> effects) {
        return effects.size() == 1 && isDecayEffect(effects.get(0));
    }

    private static boolean isDecayEffect(MobEffectInstance effect) {
        return effect.getEffect().equals(MobEffects.WITHER) && effect.getDuration() == DECAY_EFFECT_DURATION && effect.getAmplifier() == DECAY_EFFECT_AMPLIFIER;
    }

    public static void addPotionTooltip(Holder<Potion> potion, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/){
        //? if <1.20.5 {
        /*PotionUtils.addPotionTooltip(potion.value().getEffects(), tooltipList, f/^? if >=1.20.3 {^/, tickRate/^?}^/);
         *///?} else {
        PotionContents.addPotionTooltip(potion.value().getEffects(), tooltipList::add, f, tickRate);
        //?}
    }

    public static void addPotionTooltip(List<MobEffectInstance> effects, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/){
        //? if <1.20.5 {
        /*PotionUtils.addPotionTooltip(effects, tooltipList, f/^? if >=1.20.3 {^/, tickRate/^?}^/);
         *///?} else {
        PotionContents.addPotionTooltip(effects, tooltipList::add, f, tickRate);
        //?}
    }

    public static int getDyeColor(DyeColor dyeColor){
        return /*? if <1.20.5 {*//*ColorUtil.colorFromFloat(dyeColor.getTextureDiffuseColors())*//*?} else {*/dyeColor.getTextureDiffuseColor()/*?}*/;
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
        if (item == Items.BLACK_DYE) return DyeColor.BLACK;
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

    //? if <1.21.4 {
    public static ItemStack createMushroomPore() {
        ItemStack stack = new ItemStack(Items.BROWN_MUSHROOM_BLOCK);
        //? if >=1.20.5 {
        stack.set(DataComponents.ITEM_NAME, Component.translatable(MUSHROOM_PORE_NAME));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(MUSHROOM_PORE_MODEL_DATA));
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("north", "false", "east", "false", "south", "false", "west", "false", "up", "false", "down", "false")));
        //?}
        //? if <1.20.5 {

        /*CompoundTag tag = stack.getOrCreateTag();
        CompoundTag blockState = new CompoundTag();
        blockState.putString("north", "false");
        blockState.putString("east", "false");
        blockState.putString("south", "false");
        blockState.putString("west", "false");
        blockState.putString("up", "false");
        blockState.putString("down", "false");
        tag.putInt("CustomModelData", MUSHROOM_PORE_MODEL_DATA);
        tag.put("BlockStateTag", blockState);
        *///?}
        return stack;
    }

    public static boolean isMushroomPore(ItemStack stack) {
        //? if <1.20.5 {

        /*CompoundTag tag = stack.getTag();
        return stack.is(Items.BROWN_MUSHROOM_BLOCK) && tag != null && tag.getInt("CustomModelData") == MUSHROOM_PORE_MODEL_DATA;
        *///?} else {
        return stack.is(Items.BROWN_MUSHROOM_BLOCK) && stack.get(DataComponents.CUSTOM_MODEL_DATA) instanceof CustomModelData modelData && modelData.value() == MUSHROOM_PORE_MODEL_DATA;
        //?}
    }
    //?}

    public static int getPotionLevel(ItemStack stack) {
        //? if <1.20.5 {
        /*Holder<Potion> potion = BuiltInRegistries.POTION.wrapAsHolder(PotionUtils.getPotion(stack));
        List<MobEffectInstance> customEffects = PotionUtils.getCustomEffects(stack);
        if (potion.value() == Potions.EMPTY) return customEffects.stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(-1) + 1;
        if (potion instanceof Holder.Reference<Potion> reference && reference.key().location().getPath().startsWith("strong_")) return 2;
        if (potion instanceof Holder.Reference<Potion> reference && reference.key().location().getPath().startsWith("long_")) return 3;
        if (!customEffects.isEmpty()) return customEffects.stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(0) + 1;
        return potion.value().getEffects().isEmpty() ? 0 : 1;
        *///?} else {
        PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Holder<Potion> potion = potionContents.potion().orElse(null);
        if (potion == null) return potionContents.customEffects().stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(-1) + 1;
        if (potion instanceof Holder.Reference<Potion> reference && reference.key().location().getPath().startsWith("strong_")) return 2;
        if (potion instanceof Holder.Reference<Potion> reference && reference.key().location().getPath().startsWith("long_")) return 3;
        if (!potionContents.customEffects().isEmpty()) return potionContents.customEffects().stream().mapToInt(MobEffectInstance::getAmplifier).max().orElse(0) + 1;
        return potion.value().getEffects().isEmpty() ? 0 : 1;
        //?}
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
        if (/*? if <1.20.5 {*//*stack.getItem() instanceof RecordItem*//*?} else {*/stack.has(DataComponents.JUKEBOX_PLAYABLE)/*?}*/ && tooltip.size() > 1) {
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
