package wily.legacy.client.control.tooltip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
//?}
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.FactoryItemUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.mixin.base.*;
import wily.legacy.util.IOUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;

public interface ControlTooltip {
    String CONTROL_TOOLTIPS = "control_tooltips";

    BiFunction<String, Style, ComponentIcon> CONTROL_ICON_FUNCTION = Util.memoize((s, style) -> ComponentIcon.of(Component.literal(s).withStyle(style)));
    Function<Icon[], Icon> COMPOUND_ICON_FUNCTION = Util.memoize(icons -> (CompoundIcon) () -> icons);
    Function<String, MutableComponent> CONTROL_ACTION_CACHE = Util.memoize(s -> Component.translatable(s));

    String MOUSE_BASE_CHAR = "\uC002";
    String MOUSE_BASE_FOCUSED_CHAR = "\uC003";
    String KEY_CHAR = "\uC000";
    String KEY_PRESSED_CHAR = "\uC001";
    Component MORE = Component.literal("...").withStyle(ChatFormatting.GRAY);
    Component SPACE = Component.literal("  ");
    Component PLUS = Component.literal("+");
    ComponentIcon SPACE_ICON = ComponentIcon.of(SPACE);
    ComponentIcon PLUS_ICON = ComponentIcon.of(PLUS);
    Function<ComponentIcon[], ComponentIcon> COMPOUND_COMPONENT_ICON_FUNCTION = Util.memoize(CompoundComponentIcon::new);
    Map<String, ArbitrarySupplier<ComponentIcon>> commonIcons = new HashMap<>();
    ArbitrarySupplier<ComponentIcon> PLAYER_MOVEMENT = registerCommonComponentIcon("player_movement", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_W), getKeyIcon(InputConstants.KEY_A), getKeyIcon(InputConstants.KEY_S), getKeyIcon(InputConstants.KEY_D)) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_QUICK_ACTION = registerCommonComponentIcon("menu_quick_action", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT), PLUS_ICON, getKeyIcon(InputConstants.KEY_LSHIFT)) : ControllerBinding.UP_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> NAVIGATION = registerCommonComponentIcon("navigation", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_UP), getKeyIcon(InputConstants.KEY_LEFT), getKeyIcon(InputConstants.KEY_DOWN), getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> HORIZONTAL_NAVIGATION = registerCommonComponentIcon("horizontal_navigation", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_LEFT), getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> VERTICAL_NAVIGATION = registerCommonComponentIcon("vertical_navigation", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_UP), getKeyIcon(InputConstants.KEY_DOWN)) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> CONTROL_PAGE = registerCommonComponentIcon("control_page", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT), ControlTooltip.PLUS_ICON, CompoundComponentIcon.of(getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuPageLeft).getKey().getValue()), ControlTooltip.SPACE_ICON, getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuPageRight).getKey().getValue()))) : ControllerBinding.RIGHT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> CONTROL_TAB = registerCommonComponentIcon("control_tab", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabLeft).getKey().getValue()), ControlTooltip.SPACE_ICON, getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabRight).getKey().getValue())) : CompoundComponentIcon.of(ControllerBinding.LEFT_BUMPER.getIcon(), ControlTooltip.SPACE_ICON, ControllerBinding.RIGHT_BUMPER.getIcon()));
    ArbitrarySupplier<ComponentIcon> CONTROL_TYPE = registerCommonComponentIcon("control_type", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT), ControlTooltip.PLUS_ICON, CompoundComponentIcon.of(getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabLeft).getKey().getValue()), ControlTooltip.SPACE_ICON, getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabRight).getKey().getValue()))) : CompoundComponentIcon.of(ControllerBinding.LEFT_TRIGGER.getIcon(), ControlTooltip.SPACE_ICON, ControllerBinding.RIGHT_TRIGGER.getIcon()));
    ArbitrarySupplier<ComponentIcon> LEFT_CRAFTING_TYPE = registerCommonComponentIcon("left_crafting_type", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON, getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabLeft).getKey().getValue())) : ControllerBinding.LEFT_TRIGGER.getIcon());
    ArbitrarySupplier<ComponentIcon> RIGHT_CRAFTING_TYPE = registerCommonComponentIcon("right_crafting_type", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON, getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabRight).getKey().getValue())) : ControllerBinding.RIGHT_TRIGGER.getIcon());
    ArbitrarySupplier<ComponentIcon> BUNDLE_SELECTION = registerCommonComponentIcon("bundle_selection", () -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_1), SPACE_ICON, getKeyIcon(InputConstants.KEY_9), SPACE_ICON, getKbmIcon(MOUSE_BASE_CHAR)) : ControllerBinding.RIGHT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> PRESS = registerCommonComponentIcon("press", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> OPTION = registerCommonComponentIcon("option", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> EXTRA = registerCommonComponentIcon("extra", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> POINTER_MOVEMENT = registerCommonComponentIcon("pointer_movement", () -> ControlType.getActiveType().isKbm() ? getKbmIcon(MOUSE_BASE_CHAR) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> CAMERA_MOVEMENT = registerCommonComponentIcon("camera_movement", () -> ControlType.getActiveType().isKbm() ? getKbmIcon(MOUSE_BASE_CHAR) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_MAIN_ACTION = registerCommonComponentIcon("menu_main_action", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.DOWN_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_OFF_ACTION = registerCommonComponentIcon("menu_off_action", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_RIGHT) : ControllerBinding.LEFT_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> LEFT_TAB = registerCommonComponentIcon("left_tab", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabLeft).getKey().getValue()) : ControllerBinding.LEFT_BUMPER.getIcon());
    ArbitrarySupplier<ComponentIcon> RIGHT_TAB = registerCommonComponentIcon("right_tab", () -> ControlType.getActiveType().isKbm() ? getKeyIcon(LegacyKeyMapping.of(Legacy4JClient.keyMenuTabRight).getKey().getValue()) : ControllerBinding.RIGHT_BUMPER.getIcon());
    ArbitrarySupplier<ComponentIcon> CANCEL_BINDING = registerCommonComponentIcon("cancel_binding", () -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.BACK.getIcon());

    static ComponentIcon getControlIcon(String s, ControlType type) {
        return CONTROL_ICON_FUNCTION.apply(s, type.styleOrEmpty());
    }

    static MutableComponent getAction(String key) {
        return CONTROL_ACTION_CACHE.apply(key);
    }

    static <T> Component getSelectAction(GuiEventListener listener, T context) {
        return listener.isFocused() && context instanceof ActionHolder.KeyContext c && c.key() == InputConstants.KEY_RETURN ? LegacyComponents.SELECT : null;
    }

    static ArbitrarySupplier<ComponentIcon> registerCommonComponentIcon(String key, ArbitrarySupplier<ComponentIcon> supplier) {
        commonIcons.put(key, supplier);
        return supplier;
    }

    static Component getKeyMessage(int key, Screen screen) {
        for (GuiEventListener child : screen.children()) {
            Component component;
            if (child instanceof ActionHolder accessor && (component = accessor.getAction(new ActionHolder.KeyContext(new KeyEvent(key, 0, 0), screen))) != null)
                return component;
        }
        return null;
    }

    static Component getKeyboardAction(ActionHolder.KeyContext keyContext) {
        return keyContext.key() == InputConstants.KEY_NUMPADENTER && ControlType.getActiveType().isKbm() || keyContext.key() == InputConstants.KEY_RETURN && !ControlType.getActiveType().isKbm() ? LegacyComponents.SHOW_KEYBOARD : null;
    }

    static ControlTooltipList setupDefaultButtons(ControlTooltipList list, Screen screen) {
        return list.add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_NUMPADENTER) : ControllerBinding.DOWN_BUTTON.getIcon(), () -> getKeyMessage(InputConstants.KEY_NUMPADENTER, screen)).add(PRESS::get, () -> getKeyMessage(InputConstants.KEY_RETURN, screen));
    }

    static ControlTooltipList setupDefaultScreen(ControlTooltipList list, Screen screen) {
        return setupDefaultButtons(list, screen).add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon(), () -> getKeyMessage(InputConstants.KEY_RETURN, screen)).add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(), () -> screen.shouldCloseOnEsc() ? CommonComponents.GUI_BACK : null);
    }

    static ControlTooltipList setupDefaultContainerScreen(ControlTooltipList list, LegacyMenuAccess<?> a) {
        return list.
                add(MENU_MAIN_ACTION::get, () -> getMenuMainAction(a)).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(), () -> LegacyComponents.EXIT).
                add(MENU_OFF_ACTION::get, () -> getMenuOffAction(a)).
                add(MENU_QUICK_ACTION::get, () -> getMenuQuickAction(a)).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_W) : ControllerBinding.RIGHT_TRIGGER.getIcon(), () -> a.getHoveredSlot() != null && a.getHoveredSlot().hasItem() && !a.isMouseDragging() && LegacyTipManager.hasTip(a.getHoveredSlot().getItem()) ? LegacyComponents.WHATS_THIS : null).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.LEFT_TRIGGER.getIcon(), () -> a.getMenu().getCarried().getCount() > 1 && !a.isOutsideClick(0) ? LegacyComponents.DISTRIBUTE : null);
    }

    static void setupGui(ControlTooltipList list, Minecraft minecraft) {
        list.add(minecraft.options.keyJump, () -> minecraft.player.isUnderWater() ? LegacyComponents.SWIM_UP : null).add(minecraft.options.keyInventory, () -> !minecraft.gameMode.isServerControlledInventory() || !(minecraft.player.getVehicle() instanceof AbstractHorse h) || h.isTamed()).add(Legacy4JClient.keyCrafting).add(minecraft.options.keyUse, () -> getUseAction(minecraft)).add(minecraft.options.keyAttack, () -> getAttackAction(minecraft));
        list.tooltips.addAll(GuiManager.controlTooltips);
        list.add(minecraft.options.keyShift, () -> {
            if (minecraft.player.isPassenger()) {
                return minecraft.player.getVehicle() instanceof LivingEntity ? LegacyComponents.DISMOUNT : LegacyComponents.EXIT;
            }
            BlockPos playerPos = minecraft.player.blockPosition();
            boolean inOrOnScaffolding = minecraft.level.getBlockState(playerPos).is(Blocks.SCAFFOLDING) || minecraft.level.getBlockState(playerPos.below()).is(Blocks.SCAFFOLDING);
            boolean scaffoldBelow = minecraft.level.getBlockState(playerPos.below()).is(Blocks.SCAFFOLDING);
            boolean notOnGroundFloor = !minecraft.player.onGround() || scaffoldBelow;
            return (inOrOnScaffolding && notOnGroundFloor) ? LegacyComponents.HOLD_TO_DESCEND : null;
        }).add(minecraft.options.keyPickItem, () -> getPickAction(minecraft));
    }

    static Component getIconComponentFromKeyMapping(LegacyKeyMapping mapping) {
        ComponentIcon icon = getIconFromKeyMapping(mapping);
        return icon == null ? LegacyComponents.NONE : icon.getComponent();
    }

    static ComponentIcon getIconFromKeyMapping(LegacyKeyMapping mapping) {
        return ControlType.getActiveType().isKbm() ? getKeyIcon(mapping.getKey().getValue()) : mapping.getBinding() == null ? null : mapping.getBinding().getIcon();
    }

    static Component getMenuMainAction(LegacyMenuAccess<?> a) {
        if (a.isOutsideClick(0) && !a.getMenu().getCarried().isEmpty())
            return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ALL : LegacyComponents.DROP;
        if (a.getHoveredSlot() != null && !a.isMouseDragging() && (a.getHoveredSlot().hasItem() || !a.getMenu().getCarried().isEmpty())) {
            if (a.getHoveredSlot().hasItem() && !FactoryItemUtil.equalItems(a.getHoveredSlot().getItem(), a.getMenu().getCarried()) && !isBundleAndAcceptItem(a.getHoveredSlot().getItem(), a.getMenu().getCarried())) {
                return a.getMenu().getCarried().isEmpty() ? LegacyComponents.TAKE : isBundleAndAcceptItem(a.getMenu().getCarried(), a.getHoveredSlot().getItem()) ? LegacyComponents.PICK_UP : LegacyComponents.SWAP;
            } else if (!a.getMenu().getCarried().isEmpty() && a.getHoveredSlot().mayPlace(a.getMenu().getCarried()))
                return a.getHoveredSlot().getMaxStackSize() == 1 ? LegacyComponents.PLACE_ONE : a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.PLACE_ALL : LegacyComponents.PLACE;
        }
        return null;
    }

    static Component getMenuOffAction(LegacyMenuAccess<?> a) {
        if (a.isOutsideClick(1) && !a.getMenu().getCarried().isEmpty() && !a.isMouseDragging())
            return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ONE : LegacyComponents.DROP;
        if (a.getHoveredSlot() != null && !a.isMouseDragging()) {
            if (a.getMenu().getCarried().isEmpty()) {
                if (isBundle(a.getHoveredSlot().getItem()) && BundleItem.getFullnessDisplay(a.getHoveredSlot().getItem()) > 0)
                    return LegacyComponents.PICK_UP;
                else if (a.getHoveredSlot().getItem().getCount() > 1) return LegacyComponents.TAKE_HALF;
            } else {
                if (a.getHoveredSlot().hasItem() && Legacy4JClient.hasModOnServer() && LegacyItemUtil.canRepair(a.getHoveredSlot().getItem(), a.getMenu().getCarried()))
                    return LegacyComponents.REPAIR;
                if (a.getHoveredSlot().hasItem() && Legacy4JClient.hasModOnServer() && LegacyItemUtil.isDyeableItem(a.getHoveredSlot().getItem().typeHolder()) && LegacyItemUtil.getDyeColorOrNull(a.getMenu().getCarried().getItem()) != null)
                    return LegacyComponents.DYE;
                else if (isBundle(a.getMenu().getCarried()) && BundleItem.getFullnessDisplay(a.getMenu().getCarried()) > 0 && !a.getHoveredSlot().hasItem())
                    return LegacyComponents.RELEASE;
                else if (a.getHoveredSlot().hasItem() && !a.getMenu().getCarried().isEmpty() && !FactoryItemUtil.equalItems(a.getMenu().getCarried(), a.getHoveredSlot().getItem()) && a.getHoveredSlot().mayPlace(a.getHoveredSlot().getItem()))
                    return LegacyComponents.SWAP;
                else if (!a.getHoveredSlot().hasItem() && a.getHoveredSlot().mayPlace(a.getHoveredSlot().getItem()))
                    return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.PLACE_ONE : LegacyComponents.PLACE;
            }
        }
        return null;
    }

    static Component getMenuQuickAction(LegacyMenuAccess<?> a) {
        if (a.getHoveredSlot() != null && a.getHoveredSlot().hasItem()) {
            if (a.getMenu() instanceof InventoryMenu menu) {
                for (int i = 5; i < 9; i++) {
                    if (i == a.getHoveredSlot().index) break;
                    Slot slot = menu.getSlot(i);
                    if (LegacySlotDisplay.isVisibleAndActive(slot) && menu.getSlot(i).mayPlace(a.getHoveredSlot().getItem()))
                        return LegacyComponents.EQUIP;
                }
            }
            return LegacyComponents.QUICK_MOVE;
        }
        return null;
    }

    static boolean isBundle(ItemStack stack) {
        return stack.is(ItemTags.BUNDLES);
    }

    static boolean isBundleAndAcceptItem(ItemStack stack, ItemStack itemToAccept) {
        return isBundle(stack) && BundleItem.getFullnessDisplay(stack) <= (1 - (float) itemToAccept.getCount() / itemToAccept.getMaxStackSize()) && !itemToAccept.isEmpty() && itemToAccept.getItem().canFitInsideContainerItems();
    }

    static boolean isSpear(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
    }

    static ControlTooltip create(Supplier<Icon> icon, Supplier<Component> action) {
        return new ControlTooltip() {
            public Icon getIcon() {
                return icon.get();
            }

            public Component getAction() {
                return action.get();
            }
        };
    }

    static ControlTooltip create(LegacyKeyMapping mapping, Supplier<Component> action) {
        return create(() -> getIconFromKeyMapping(mapping), action);
    }

    static float getAlpha() {
        return Math.max(Minecraft.getInstance().screen == null ? 0.0f : 0.2f, LegacyRenderUtil.getHUDOpacity());
    }

    static ComponentIcon getKbmIcon(String key) {
        return getControlIcon(key, ControlType.getKbmActiveType());
    }

    static LegacyIcon getKeyIcon(int i) {
        InputConstants.Type type = i >= 0 ? i <= 9 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM : null;
        if (type == null) return null;
        InputConstants.Key key = type.getOrCreate(i);
        return ControlType.getKbmActiveType().icons().computeIfAbsent(key.getName(), i2 -> new KeyIcon(key) {
            @Override
            public Component getComponent(boolean allowPressed) {
                return getControlIcon(key.getType() == InputConstants.Type.MOUSE ? pressed() && allowPressed ? MOUSE_BASE_FOCUSED_CHAR : MOUSE_BASE_CHAR : pressed() && allowPressed ? KEY_PRESSED_CHAR : KEY_CHAR, ControlType.getKbmActiveType()).getComponent();
            }

            @Override
            public Component getOverlayComponent(boolean allowPressed) {
                return key.getDisplayName();
            }
        });
    }

    static Component getPickAction(Minecraft minecraft) {
        ItemStack result;
        BlockState b;
        if ((minecraft.hitResult instanceof EntityHitResult r && (result = r.getEntity().getPickResult()) != null || minecraft.hitResult instanceof BlockHitResult h && h.getType() != HitResult.Type.MISS && !(result = (b = minecraft.level.getBlockState(h.getBlockPos()))/*? if <1.21.4 {*//*.getBlock()*//*?}*/.getCloneItemStack(minecraft.level, h.getBlockPos(),/*? if >=1.21.4 {*/true/*?} else {*//*b*//*?}*/)).isEmpty()) && (Legacy4JClient.playerHasInfiniteMaterials() || minecraft.player.getInventory().findSlotMatchingItem(result) != -1))
            return minecraft.hitResult instanceof EntityHitResult ? LegacyComponents.PICK_ENTITY : ((LegacyKeyMapping) minecraft.options.keyPickItem).getDisplayName();

        return null;
    }

    static Component getUseAction(Minecraft minecraft) {
        if (minecraft.player == null) return null;
        Component action = getActualUse(minecraft);
        if (isSpear(minecraft.player.getMainHandItem()) && action != LegacyComponents.EQUIP)
            return LegacyComponents.CHARGE;
        return action;
    }

    static Component getAttackAction(Minecraft minecraft) {
        return minecraft.player != null && isSpear(minecraft.player.getMainHandItem()) ? LegacyComponents.JAB : getMainAction(minecraft);
    }

    static Component getMainAction(Minecraft minecraft) {
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation().x(), minecraft.hitResult.getLocation().z()))
            return null;
        if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS) {
            BlockState state = minecraft.level.getBlockState(r.getBlockPos());
            if (minecraft.player.getAbilities().instabuild && minecraft.player.getMainHandItem().is(ItemTags.SWORDS))
                return null;
            if (state.getBlock() instanceof NoteBlock && !minecraft.player.getAbilities().instabuild)
                return LegacyComponents.PLAY;
            else if ((minecraft.player.getAbilities().instabuild || state.getBlock().defaultDestroyTime() >= 0 && !minecraft.player.blockActionRestricted(minecraft.level, r.getBlockPos(), minecraft.gameMode.getPlayerMode())))
                return LegacyComponents.MINE;
        }
        return null;
    }

    ListMap<String, ActionHolder> GENERIC_USES = new ListMap<>();
    ListMap<String, ActionHolder> ENTITY_INTERACT_ACTIONS = new ListMap<>();
    ListMap<String, ActionHolder> BLOCK_USE_ACTIONS = new ListMap<>();
    ListMap<String, ActionHolder> BLOCK_USE_ITEM_ON_ACTIONS = new ListMap<>();
    ListMap<String, ActionHolder> ITEM_USE_ON_ACTIONS = new ListMap<>();
    ListMap<String, ActionHolder> ITEM_USE_ACTIONS = new ListMap<>();

    static ActionHolder registerGenericUse(String id, Function<Player, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof Player p ? function.apply(p) : null;
        GENERIC_USES.put(id, holder);
        return holder;
    }

    static ActionHolder registerGenericUse(String id, Predicate<Player> predicate, Component action) {
        return registerGenericUse(id, ctx -> predicate.test(ctx) ? action : null);
    }

    static ActionHolder registerEntityInteract(String id, Function<EntityInteract, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof EntityInteract entityInteract ? function.apply(entityInteract) : null;
        ENTITY_INTERACT_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerEntityInteract(String id, Predicate<EntityInteract> predicate, Component action) {
        return registerEntityInteract(id, ctx -> predicate.test(ctx) ? action : null);
    }

    static ActionHolder registerBlockUse(String id, Function<BlockUse, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof BlockUse blockUse ? function.apply(blockUse) : null;
        BLOCK_USE_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerResultBlockUse(String id, Function<BlockUse, ResultAction> function) {
        ResultActionHolder holder = ctx -> ctx instanceof BlockUse blockUse ? function.apply(blockUse) : null;
        BLOCK_USE_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerBlockUse(String id, Predicate<BlockUse> predicate, Component action) {
        return registerBlockUse(id, ctx -> predicate.test(ctx) ? action : null);
    }

    static ActionHolder registerBlockUseItemOn(String id, Function<BlockUseItemOn, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof BlockUseItemOn blockUseItemOn ? function.apply(blockUseItemOn) : null;
        BLOCK_USE_ITEM_ON_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerResultBlockUseItemOn(String id, Function<BlockUseItemOn, ResultAction> function) {
        ResultActionHolder holder = ctx -> ctx instanceof BlockUseItemOn blockUseItemOn ? function.apply(blockUseItemOn) : null;
        BLOCK_USE_ITEM_ON_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerBlockUseItemOn(String id, Predicate<BlockUseItemOn> predicate, Component action) {
        return registerBlockUseItemOn(id, ctx -> predicate.test(ctx) ? action : null);
    }

    static ActionHolder registerUseItemOn(String id, Function<BlockUseItemOn, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof BlockUseItemOn useOnContext ? function.apply(useOnContext) : null;
        ITEM_USE_ON_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerUseItemOn(String id, Predicate<BlockUseItemOn> predicate, Component action) {
        return registerUseItemOn(id, ctx -> predicate.test(ctx) ? action : null);
    }


    static ActionHolder registerUseItem(String id, Function<UseItem, Component> function) {
        ActionHolder holder = ctx -> ctx instanceof UseItem useContext ? function.apply(useContext) : null;
        ITEM_USE_ACTIONS.put(id, holder);
        return holder;
    }

    static ActionHolder registerUseItem(String id, Predicate<UseItem> predicate, Component action) {
        return registerUseItem(id, ctx -> predicate.test(ctx) ? action : null);
    }

    ActionHolder WAKE_UP = registerGenericUse("wake_up", Player::isSleeping, LegacyComponents.WAKE_UP);

    ActionHolder TRADE = registerEntityInteract("trade", ctx -> ctx.entity instanceof AbstractVillager m && (!(m instanceof Villager v) || /*? if <1.21.5 {*//*v.getVillagerData().getProfession() != VillagerProfession.NONE*//*?} else {*/!v.getVillagerData().profession().is(VillagerProfession.NONE)/*?}*/) && !m.isTrading(), LegacyComponents.TRADE);
    ActionHolder ITEM_FRAME = registerEntityInteract("item_frame", ctx -> {
        if (ctx.entity instanceof ItemFrame frame) {
            if (!frame.getItem().isEmpty()) return LegacyComponents.ROTATE;
            else if (!ctx.handItem.isEmpty()) return LegacyComponents.PLACE;
        }
        return null;
    });
    ActionHolder LEASH_ENTITY = registerEntityInteract("leash", ctx -> {
        if (ctx.entity instanceof LeashFenceKnotEntity knot) {
            BlockPos fencePos = knot.getPos();
            if (!ctx.entity.level().getEntities((Entity) null, new AABB(fencePos).inflate(8.0D), e -> (e instanceof Mob mob && mob.getLeashHolder() == ctx.player) || e instanceof Boat boat && boat.getLeashHolder() == ctx.player || e instanceof ChestBoat chestBoat && chestBoat.getLeashHolder() == ctx.player).isEmpty())
                return LegacyComponents.ATTACH;
            return LegacyComponents.DETACH;
        }
        boolean isLeashedToPlayer = (ctx.entity instanceof Mob m && m.getLeashHolder() == ctx.player) || ctx.entity instanceof Boat b && b.getLeashHolder() == ctx.player || ctx.entity instanceof ChestBoat cb && cb.getLeashHolder() == ctx.player;
        if (isLeashedToPlayer)
            return LegacyComponents.UNLEASH;

        if (ctx.handItem.getItem() instanceof LeadItem) {
            boolean isLeashableEntity = (ctx.entity instanceof Mob m && m.canBeLeashed(/* ? if <1.20.5 { *//* ctx.player *//* ?} */)) || ctx.entity instanceof AbstractHorse || ctx.entity instanceof Llama || ctx.entity instanceof Parrot || ctx.entity instanceof Boat || ctx.entity instanceof ChestBoat;
            if (isLeashableEntity)
                return LegacyComponents.LEASH;
        }
        return null;
    });
    ActionHolder ARMOR_STAND = registerEntityInteract("armor_stand", ctx -> {
        if (ctx.entity instanceof ArmorStand stand) {
            if (ctx.player.isShiftKeyDown())
                return LegacyComponents.CHANGE_POSE;
            return ctx.handItem.isEmpty() ? stand.getItemBySlot(((ArmorStandAccessor) stand).getLocationClickedSlot(ctx.location)).isEmpty() ? null : LegacyComponents.TAKE  : LegacyComponents.EQUIP;
        }
        return null;
    });
    ActionHolder CREEPER = registerEntityInteract("creeper", ctx -> ctx.entity instanceof Creeper creeper && !creeper.isIgnited() && ctx.handItem.is(Items.FLINT_AND_STEEL), LegacyComponents.IGNITE);
    //? if >=1.21.11 {
    ActionHolder NAUTILUS_INVENTORY = registerEntityInteract("nautilus_inventory", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canOpenNautilusInventory(ctx.player, nautilus), LegacyComponents.IGNITE);
    ActionHolder NAUTILUS_SADDLE = registerEntityInteract("nautilus_inventory", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canEquipNautilus(nautilus, ctx.handItem, EquipmentSlot.SADDLE), LegacyComponents.SADDLE);
    ActionHolder NAUTILUS_EQUIP = registerEntityInteract("nautilus_equip", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canEquipNautilus(nautilus, ctx.handItem, EquipmentSlot.BODY), LegacyComponents.EQUIP);
    //?}
    ActionHolder WOLF_EQUIP = registerEntityInteract("wolf_equip", ctx -> ctx.entity instanceof Wolf wolf && wolf.isTame() && ctx.handItem.has(DataComponents.EQUIPPABLE) && ctx.handItem.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.BODY && wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    ActionHolder TAMABLE_ANIMAL = registerEntityInteract("tamable_animal", ctx -> {
        if (ctx.entity instanceof TamableAnimal a && a.isTame() && a.isOwnedBy(ctx.player)/*? if >=1.21.11 {*/ && !(a instanceof AbstractNautilus)/*?}*/ && !canDyeEntity(ctx) && (!(a instanceof Parrot p) || (p.onGround() && !ctx.player.isPassenger())))
            return a.isInSittingPose() ? LegacyComponents.FOLLOW_ME : LegacyComponents.SIT;
        return null;
    });
    ActionHolder ALLAY = registerEntityInteract("allay", ctx -> {
        if (ctx.entity instanceof Allay allay) {
            ItemStack allayItem = allay.getItemInHand(InteractionHand.MAIN_HAND);
            if (ctx.handItem.isEmpty() && !allayItem.isEmpty())
                return LegacyComponents.TAKE;
            if (allayItem.isEmpty() && !ctx.handItem.isEmpty() && !ctx.handItem.is(Items.LEAD))
                return LegacyComponents.GIVE;
        }
        return null;
    });
    ActionHolder CHESTED_HORSE = registerEntityInteract("chested_horse", ctx -> ctx.entity instanceof AbstractChestedHorse horse && ctx.handItem.is(Items.CHEST) && horse.isTamed() && !horse.hasChest() && !horse.isVehicle() && !ctx.player.isSecondaryUseActive(), LegacyComponents.ATTACH_CHEST);
    ActionHolder COMMAND_BLOCK_MINECART = registerEntityInteract("command_block_minecart", ctx -> ctx.entity != null && ctx.entity.getType() == EntityType.COMMAND_BLOCK_MINECART && ctx.player.canUseGameMasterBlocks(), LegacyComponents.EDIT);
    ActionHolder HORSE_INVENTORY = registerEntityInteract("horse_inventory", ctx -> ctx.entity instanceof AbstractHorse h && h.isTamed() && ctx.player.isSecondaryUseActive(), LegacyComponents.OPEN);
    ActionHolder MINECART_INVENTORY = registerEntityInteract("minecart_inventory", ctx -> ctx.entity instanceof AbstractMinecartContainer, LegacyComponents.OPEN);
    ActionHolder BOAT_INVENTORY = registerEntityInteract("boat_inventory", ctx -> ctx.entity instanceof AbstractChestBoat, LegacyComponents.OPEN);
    ActionHolder HAPPY_GHAST_HARNESS = registerEntityInteract("happy_ghast_harness", ctx -> ctx.entity instanceof HappyGhast ghast && ctx.handItem.is(ItemTags.HARNESSES) && ghast.canUseSlot(EquipmentSlot.BODY) && ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.HARNESS);
    ActionHolder SADDLABLE = registerEntityInteract("saddle", ctx -> /*? if <1.21.5 {*//*MainHand.getItem() instanceof SaddleItem && *//*?}*/ctx.entity instanceof /*? if <1.21.5 {*//*Saddleable*//*?} else {*/ Mob/*?}*/ s &&/*? if <1.21.5 {*//*s.isSaddleable()*//*?} else {*/s.isEquippableInSlot(ctx.handItem, EquipmentSlot.SADDLE)/*?}*/ && !s.isSaddled() && (!(s instanceof AbstractHorse h) || h.isTamed()), LegacyComponents.SADDLE);
    ActionHolder SHEAR_EQUIPMENT = registerEntityInteract("shear_equipment", ctx -> {
        if (ctx.entity instanceof LivingEntity living && ctx.handItem.getItem() instanceof ShearsItem) {
            if (canShearEquipment(living, EquipmentSlot.BODY) && living.getItemBySlot(EquipmentSlot.BODY).is(ItemTags.HARNESSES))
                return LegacyComponents.REMOVE_HARNESS;
            //? if >=1.21.11 {
            if (living instanceof AbstractNautilus && canShearEquipment(living, EquipmentSlot.BODY))
                return LegacyComponents.REMOVE_ARMOR;
            //?}
            if (canShearEquipment(living, EquipmentSlot.SADDLE))
                return LegacyComponents.REMOVE_SADDLE;
        }
        return null;
    });
    ActionHolder HORSE_EQUIP = registerEntityInteract("horse_equip", ctx -> ctx.entity instanceof AbstractHorse h && h.isTamed() && !ctx.handItem.is(Items.SADDLE) && ctx.handItem.has(DataComponents.EQUIPPABLE) && ctx.handItem.get(DataComponents.EQUIPPABLE).slot().equals(EquipmentSlot.BODY) && h.isEquippableInSlot(ctx.handItem, EquipmentSlot.BODY) && h.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    ActionHolder LLAMA_EQUIP = registerEntityInteract("llama_equip", ctx -> ctx.entity instanceof Llama llama && llama.isTamed() && ctx.handItem.is(ItemTags.WOOL_CARPETS) && llama.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    ActionHolder RIDE = registerEntityInteract("ride", ctx -> {
        if (ctx.entity != null && ((EntityAccessor)ctx.entity).canVehicleAddPassenger(ctx.player) && ((EntityAccessor)ctx.entity).canRideVehicle(ctx.entity)) {
            boolean holdingLead = ctx.player.getMainHandItem().getItem() instanceof LeadItem;
            if (!holdingLead) {
                if (ctx.entity instanceof Boat || ctx.entity instanceof ChestBoat) return LegacyComponents.SAIL;
                else if (ctx.entity instanceof AbstractMinecart m && /*? if <1.21.2 {*//*m.getMinecartType() == AbstractMinecart.Type.RIDEABLE*//*?} else {*/m.isRideable()/*?}*/)
                    return LegacyComponents.RIDE;
                else if ((ctx.entity instanceof HappyGhast ghast && !ctx.entity.isVehicle() && !ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()) || ctx.entity instanceof /*? if <1.21.5 {*//*Saddleable*//*?} else {*/ Mob/*?}*/ s && !ctx.entity.isVehicle() && ((!(ctx.entity instanceof AbstractHorse) && s.isSaddled()) || ctx.entity instanceof AbstractHorse h && !ctx.player.isSecondaryUseActive() && !h.isBaby() && (h.isTamed() && !h.isFood(ctx.player.getMainHandItem()) || ctx.player.getMainHandItem().isEmpty())))
                    return LegacyComponents.MOUNT;
            }
        }
        return null;
    });
    ActionHolder CURE_ZOMBIE_VILLAGER = registerEntityInteract("cure_zombie_villager", ctx -> ctx.entity instanceof ZombieVillager zombieVillager && ctx.handItem.is(Items.GOLDEN_APPLE) && !zombieVillager.isConverting(), LegacyComponents.CURE);
    ActionHolder BARTER_PIGLIN = registerEntityInteract("barter_piglin", ctx -> ctx.entity instanceof Piglin piglin && ctx.handItem.is(Items.GOLD_INGOT) && !piglin.isBaby() && piglin.getOffhandItem().isEmpty(), LegacyComponents.BARTER);
    ActionHolder COLLECT_BUCKETABLE = registerEntityInteract("collect_bucketable", ctx -> ctx.entity instanceof Bucketable && ctx.handItem.is(Items.WATER_BUCKET), LegacyComponents.COLLECT);
    ActionHolder MILK_MUSHROOM = registerEntityInteract("milk_mushroom", ctx -> ctx.entity instanceof MushroomCow mushroomCow && ctx.handItem.is(Items.BOWL) && !mushroomCow.isBaby(), LegacyComponents.MILK);
    ActionHolder MILK_COW = registerEntityInteract("milk_cow", ctx -> ctx.entity instanceof AbstractCow cow && ctx.handItem.is(Items.BUCKET) && !cow.isBaby(), LegacyComponents.MILK);
    ActionHolder FURNACE_MINECART = registerEntityInteract("furnace_minecart", ctx -> ctx.entity instanceof MinecartFurnace && ctx.handItem.is(ItemTags.COALS), LegacyComponents.FUEL);
    ActionHolder TAME_ANIMAL = registerEntityInteract("tame_animal", ControlTooltip::canTame, LegacyComponents.TAME);
    ActionHolder DYE_COLLAR = registerEntityInteract("dye_collar", ControlTooltip::canDyeCollar, LegacyComponents.DYE_COLLAR);
    ActionHolder DYE_ENTITY = registerEntityInteract("dye_entity", ControlTooltip::canDyeEntity, LegacyComponents.DYE);
    ActionHolder FEED_ANIMAL = registerEntityInteract("feed_animal", ctx -> canFeed(ctx) || canFeedWithGoldenDandelion(ctx.entity, ctx.handItem), LegacyComponents.FEED);
    ActionHolder LOVE_MODE_ANIMAL = registerEntityInteract("love_mode_animal", ControlTooltip::canSetLoveMode, LegacyComponents.LOVE_MODE);
    ActionHolder REPAIR_IRON_GOLEM = registerEntityInteract("repair_iron_golem", ctx -> ctx.entity instanceof IronGolem g && ctx.handItem.is(Items.IRON_INGOT) && g.getHealth() < g.getMaxHealth(), LegacyComponents.REPAIR);
    ActionHolder SHEAR_ENTITY = registerEntityInteract("shear_entity", ctx -> ctx.entity instanceof Shearable shearable && shearable.readyForShearing() && ctx.handItem.is(Items.SHEARS), LegacyComponents.SHEAR);
    ActionHolder BRUSH_ARMADILLO = registerEntityInteract("brush_armadillo", ctx -> ctx.entity instanceof Armadillo armadillo && !armadillo.isBaby() && ctx.handItem.is(Items.BRUSH), LegacyComponents.BRUSH);
    ActionHolder NAME_ENTITY = registerEntityInteract("name_entity", ctx -> ctx.entity instanceof LivingEntity e && !(e instanceof Player) && e.isAlive() && ctx.handItem.getItem() instanceof NameTagItem && FactoryItemUtil.hasCustomName(ctx.handItem), LegacyComponents.NAME);

    ActionHolder FENCE = registerBlockUse("fence", ctx -> {
        if (ctx.state != null && ctx.state.is(BlockTags.FENCES))
            if (!ctx.level.getEntities((Entity) null, new AABB(ctx.pos).inflate(8.0D), e -> (e instanceof Mob mob && mob.getLeashHolder() == ctx.player) || e instanceof Boat boat && boat.getLeashHolder() == ctx.player || e instanceof ChestBoat chestBoat && chestBoat.getLeashHolder() == ctx.player).isEmpty())
                return LegacyComponents.ATTACH;
        return null;
    });
    ActionHolder SLEEP = registerResultBlockUse("sleep", ctx -> ctx.state.getBlock() instanceof BedBlock ? canSleep(ctx) ? ResultAction.of(LegacyComponents.SLEEP) : ResultAction.cancel() : ResultAction.pass());
    ActionHolder CHANGE_PITCH = registerBlockUse("change_pitch", ctx -> ctx.state.getBlock() instanceof NoteBlock, LegacyComponents.CHANGE_PITCH);
    ActionHolder REDSTONE_ORE_USE = registerBlockUse("redstone_ore_use", ctx -> ctx.state.getBlock() instanceof RedStoneOreBlock, LegacyComponents.USE);
    ActionHolder MECHANISMS_USE = registerBlockUse("mechanisms_use", ctx -> ctx.state.getBlock() instanceof RepeaterBlock || ctx.state.getBlock() instanceof ComparatorBlock || ctx.state.getBlock() instanceof RedStoneWireBlock, LegacyComponents.USE);
    ActionHolder COLLECT_COMPOSTER = registerBlockUse("collect_composter", ctx -> ctx.state.getBlock() instanceof ComposterBlock && ctx.state.getValue(ComposterBlock.LEVEL) == 8, LegacyComponents.COLLECT);
    ActionHolder EJECT_RECORD = registerBlockUse("eject_record", ctx -> ctx.state.getBlock() instanceof JukeboxBlock && ctx.state.getValue(HAS_RECORD), LegacyComponents.EJECT);
    ActionHolder INVERT_DETECTOR = registerBlockUse("invert_detector", ctx -> ctx.state.getBlock() instanceof DaylightDetectorBlock, LegacyComponents.INVERT);
    ActionHolder RING_BELL = registerBlockUse("ring_bell", ctx -> ctx.state.getBlock() instanceof BellBlock, LegacyComponents.INVERT);
    ActionHolder READ_LECTERN = registerBlockUse("read_lectern", ctx -> ctx.state.getBlock() instanceof LecternBlock && ctx.state.getValue(LecternBlock.HAS_BOOK), LegacyComponents.READ);
    ActionHolder EDIT_COMMAND_BLOCK = registerBlockUse("edit_command_block", ctx -> ctx.state.getBlock() instanceof CommandBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.EDIT);
    ActionHolder CONFIGURE_STRUCTURE_BLOCK = registerBlockUse("configure_structure_block", ctx -> ctx.state.getBlock() instanceof StructureBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.CONFIGURE);
    ActionHolder CONFIGURE_JIGSAW_BLOCK = registerBlockUse("configure_jigsaw_block", ctx -> ctx.state.getBlock() instanceof StructureBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.CONFIGURE);
    ActionHolder ADJUST_LIGHT = registerBlockUseItemOn("adjust_light", ctx -> ctx.state.getBlock() instanceof LightBlock && ctx.player.canUseGameMasterBlocks() && ctx.itemStack.is(Items.LIGHT), LegacyComponents.ADJUST);
    ActionHolder COLLECT_FLOWER_POT = registerBlockUse("collect_flower_pot", ctx -> ctx.state.getBlock() instanceof FlowerPotBlock pot && /*? if <1.20.2 {*//*pot.getContent()*//*?} else {*/pot.getPotted()/*?}*/ != Blocks.AIR, LegacyComponents.COLLECT);

    ActionHolder PLACE_BOOK_LECTERN = registerBlockUseItemOn("place_book_lectern", ctx -> ctx.state.getBlock() instanceof LecternBlock && (ctx.itemStack.is(Items.WRITABLE_BOOK) || ctx.itemStack.is(Items.WRITTEN_BOOK)), LegacyComponents.PLACE);
    ActionHolder EAT_CAKE = registerBlockUseItemOn("eat_cake", ctx -> (ctx.state.getBlock() instanceof CakeBlock || ctx.state.getBlock() instanceof CandleCakeBlock) && ctx.player.canEat(false), LegacyComponents.EAT);
    ActionHolder HARVEST_SWEET_BERRIES = registerBlockUseItemOn("harvest_sweet_berries", ctx -> canHarvestSweetBerries(ctx.state, ctx.player), LegacyComponents.HARVEST);
    ActionHolder HARVEST_GLOW_BERRIES = registerBlockUseItemOn("harvest_glow_berries", ctx -> canHarvestGlowBerries(ctx.state), LegacyComponents.HARVEST);
    ActionHolder PLACE_DECORATED_POT  = registerBlockUseItemOn("place_decorated_pot", ctx -> {
        if (!(ctx.state.getBlock() instanceof DecoratedPotBlock) || !(ctx.level.getBlockEntity(ctx.hitResult.getBlockPos()) instanceof DecoratedPotBlockEntity pot))
            return null;
        ItemStack stored = pot.getTheItem();
        if (!ctx.itemStack.isEmpty() && (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, ctx.itemStack) && stored.getCount() < stored.getMaxStackSize()))
            return LegacyComponents.PLACE;
        return null;
    });
    ActionHolder OPEN_CLOSE_DOOR = registerBlockUseItemOn("open_close_door", ctx ->  {
        if (DoorBlock.isWoodenDoor(ctx.state) || ctx.state.getBlock() instanceof TrapDoorBlock && ctx.state.getBlock() != Blocks.IRON_TRAPDOOR || ctx.state.getBlock() instanceof FenceGateBlock)
            return ctx.state.getValue(BlockStateProperties.OPEN) ? LegacyComponents.CLOSE : LegacyComponents.OPEN;
        return null;
    });
    ActionHolder OPEN_BLOCK = registerBlockUseItemOn("open_block", ctx ->  {
        if ((ctx.state.getBlock() instanceof ButtonBlock || ctx.state.getBlock() instanceof LeverBlock || ctx.state.getBlock() instanceof EnderChestBlock || ctx.state.getMenuProvider(ctx.level, ctx.pos) != null || ctx.level.getBlockEntity(ctx.pos) instanceof MenuProvider))
            return (ctx.state.getBlock() instanceof AbstractChestBlock || ctx.state.getBlock() instanceof ShulkerBoxBlock || ctx.state.getBlock() instanceof BarrelBlock || ctx.state.getBlock() instanceof HopperBlock || ctx.state.getBlock() instanceof DropperBlock) ? LegacyComponents.OPEN : LegacyComponents.USE;
        return null;
    });
    ActionHolder INSERT_END_PORTAL_FRAME = registerBlockUseItemOn("insert_end_portal_frame", ctx -> ctx.state.getBlock() instanceof EndPortalFrameBlock && ctx.state.getValue(EndPortalFrameBlock.HAS_EYE) && ctx.itemStack.is(Items.ENDER_EYE), LegacyComponents.INSERT);
    ActionHolder CHARGE_RESPAWN_ANCHOR = registerBlockUseItemOn("charge_respawn_anchor", ctx -> ctx.state.getBlock() instanceof RespawnAnchorBlock && ctx.itemStack.is(Items.GLOWSTONE) && ctx.state.getValue(RespawnAnchorBlock.CHARGE) < RespawnAnchorBlock.MAX_CHARGES, LegacyComponents.CHARGE);
    ActionHolder UNLOCK_VAULT = registerBlockUseItemOn("unlock_vault", ctx -> canUnlockVault(ctx.state, ctx.itemStack), LegacyComponents.UNLOCK);
    ActionHolder PLAY_JUKEBOX = registerBlockUseItemOn("play_jukebox", ctx -> ctx.state.getBlock() instanceof JukeboxBlock && ctx.itemStack.has(DataComponents.JUKEBOX_PLAYABLE), LegacyComponents.PLAY);
    ActionHolder COLLECT_BEEHIVE = registerBlockUseItemOn("collect_beehive", ctx -> ctx.state.getBlock() instanceof BeehiveBlock && ctx.itemStack.is(Items.GLASS_BOTTLE) && ctx.state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS, LegacyComponents.COLLECT);
    ActionHolder SHEAR_BEEHIVE = registerBlockUseItemOn("shear_beehive", ctx -> ctx.state.getBlock() instanceof BeehiveBlock && ctx.itemStack.is(Items.SHEARS) && ctx.state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS, LegacyComponents.SHEAR);
    ActionHolder FILL_COMPOSTER = registerBlockUseItemOn("fill_composter", ctx -> ctx.state.getBlock() instanceof ComposterBlock && ctx.state.getValue(ComposterBlock.LEVEL) < ComposterBlock.MAX_LEVEL && ComposterBlock.COMPOSTABLES.containsKey(ctx.itemStack.getItem()), LegacyComponents.FILL);
    ActionHolder COOK_CAMPFIRE = registerBlockUseItemOn("cook_campfire", ctx -> !ctx.itemStack.isEmpty() && ctx.level.getBlockEntity(ctx.pos) instanceof CampfireBlockEntity e && /*? if <1.21.2 {*//*e.getCookableRecipe(actualItem).isPresent()*//*?} else {*/ctx.level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(ctx.itemStack)/*?}*/, LegacyComponents.COOK);
    ActionHolder PLACE_LECTERN_BOOK = registerBlockUseItemOn("place_lectern_book", ctx -> ctx.state.getBlock() instanceof LecternBlock && !ctx.state.getValue(LecternBlock.HAS_BOOK) && ctx.itemStack.is(ItemTags.LECTERN_BOOKS), LegacyComponents.PLACE);

    ActionHolder REMOVE_CHISELED_BOOK_SHELF = registerBlockUse("remove_chiseled_book_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ChiseledBookShelfBlock shelf) {
            OptionalInt slot = shelf.getHitSlot(ctx.hitResult, ctx.hitResult.getDirection());
            if (slot.isPresent()) {
                int s = slot.getAsInt();
                if (ctx.state.getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(s)))
                    return LegacyComponents.REMOVE;
            }
        }
        return null;
    });
    ActionHolder PLACE_CHISELED_BOOK_SHELF = registerBlockUseItemOn("place_chiseled_book_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ChiseledBookShelfBlock shelf && shelf.getHitSlot(ctx.hitResult, ctx.hitResult.getDirection()).isPresent()) {
            if (ctx.itemStack.is(Items.BOOK) || ctx.itemStack.is(Items.WRITABLE_BOOK) || ctx.itemStack.is(Items.WRITTEN_BOOK) || ctx.itemStack.is(Items.ENCHANTED_BOOK))
                return LegacyComponents.PLACE;
        }
        return null;
    });
    ActionHolder PLACE_SHELF = registerBlockUseItemOn("place_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ShelfBlock shelf && ctx.level.getBlockEntity(ctx.pos) instanceof ShelfBlockEntity shelfEntity) {
            OptionalInt slot = shelf.getHitSlot(ctx.hitResult, ctx.state.getValue(ShelfBlock.FACING));
            if (slot.isPresent()) {
                ItemStack item = shelfEntity.getItem(slot.getAsInt());
                if (ctx.itemStack.isEmpty())
                    return item.isEmpty() ? null : LegacyComponents.TAKE;
                return item.isEmpty() ? LegacyComponents.PLACE : LegacyComponents.SWAP;
            }
        }
        return null;
    });
    ActionHolder CHANGE_COPPER_GOLEM_POSE = registerBlockUseItemOn("change_copper_golem_pose", ctx -> ctx.state.is(BlockTags.COPPER_GOLEM_STATUES) && !ctx.itemStack.is(ItemTags.AXES), LegacyComponents.CHANGE_POSE);
    ActionHolder SIGN_EDIT = registerBlockUse("sign_edit", ctx -> ctx.state.getBlock() instanceof SignBlock, LegacyComponents.EDIT);
    ActionHolder SIGN_TEXT_STYLE = registerBlockUseItemOn("sign_text_style", ctx -> {
        if (ctx.level.getBlockEntity(ctx.hitResult.getBlockPos()) instanceof SignBlockEntity sign) {
            if (ctx.itemStack.is(Items.HONEYCOMB) && !sign.isWaxed())
                return LegacyComponents.WAX;

            SignText text = sign.isFacingFrontText(ctx.player) ? sign.getFrontText() : sign.getBackText();

            if (ctx.itemStack.is(Items.GLOW_INK_SAC) && !text.hasGlowingText())
                return LegacyComponents.GLOW;
            if (ctx.itemStack.is(Items.INK_SAC) && text.hasGlowingText())
                return LegacyComponents.REMOVE_GLOW;
            if (LegacyItemUtil.getDyeColorOrNull(ctx.itemStack.getItem()) != null && text.getColor() != LegacyItemUtil.getDyeColor(ctx.itemStack.getItem()))
                return LegacyComponents.DYE;
        }
        return null;
    });

    ActionHolder CAULDRON_INTERACTIONS = registerBlockUseItemOn("cauldron_interactions", ctx -> {
        if (ctx.state.getBlock() instanceof AbstractCauldronBlock) {
            Block block = ctx.state.getBlock();
            boolean isEmptyBottle = ctx.itemStack.is(Items.GLASS_BOTTLE);
            boolean isWaterBottle = LegacyItemUtil.isWaterBottle(ctx.itemStack);
            boolean isEmptyBucket = ctx.itemStack.is(Items.BUCKET);
            boolean isWaterBucket = ctx.itemStack.is(Items.WATER_BUCKET);
            boolean isPowderBucket = ctx.itemStack.is(Items.POWDER_SNOW_BUCKET);
            boolean isLavaBucket = ctx.itemStack.is(Items.LAVA_BUCKET);
            boolean isOtherPotion = (ctx.itemStack.is(Items.POTION) || ctx.itemStack.is(Items.SPLASH_POTION) || ctx.itemStack.is(Items.LINGERING_POTION)) && !isWaterBottle;
            boolean isArrow = ctx.itemStack.is(Items.ARROW);
            WaterCauldronBlockEntity be = null;
            if (ctx.level.getBlockEntity(ctx.pos) instanceof WaterCauldronBlockEntity wbe) be = wbe;
            int level = ctx.state.hasProperty(LayeredCauldronBlock.LEVEL) ? ctx.state.getValue(LayeredCauldronBlock.LEVEL) : 0;
            boolean isDyed = be != null && be.waterColor != null;
            if (isDyed && (isWaterBottle || isWaterBucket))
                return LegacyComponents.FLUSH;
            if (isArrow && be != null && !be.hasWater() && level > 0)
                return ctx.itemStack.getCount() > 1 ? LegacyComponents.TIP_ARROWS : LegacyComponents.TIP_ARROW;
            if (isEmptyBottle && block == Blocks.WATER_CAULDRON && !isDyed && level > 0)
                return LegacyComponents.COLLECT;
            if (isEmptyBucket) {
                if (block == Blocks.WATER_CAULDRON && level == 3)
                    return LegacyComponents.COLLECT;
                if (block == Blocks.LAVA_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON)
                    return LegacyComponents.COLLECT;
            }
            if (isWaterBottle && block == Blocks.CAULDRON)
                return LegacyComponents.FILL;
            if (isWaterBottle && block == Blocks.WATER_CAULDRON && !isDyed && level < 3)
                return LegacyComponents.FILL;
            if (isWaterBucket) {
                if (block == Blocks.CAULDRON || block == Blocks.LAVA_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON)
                    return LegacyComponents.FILL;
                if (block == Blocks.WATER_CAULDRON && level < 3)
                    return LegacyComponents.FILL;
            }
            if (isPowderBucket && (block == Blocks.CAULDRON || block == Blocks.WATER_CAULDRON || block == Blocks.LAVA_CAULDRON))
                return LegacyComponents.FILL;
            if (isLavaBucket && (block == Blocks.CAULDRON || block == Blocks.WATER_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON))
                return LegacyComponents.FILL;
            if (isOtherPotion && block == Blocks.CAULDRON)
                return LegacyComponents.FILL;
        }
        return null;
    });

    ActionHolder EMPTY_COLLECT_CAULDRON = registerBlockUseItemOn("empty_collect_cauldron", ctx -> {
        BlockHitResult bucketHitResult;
        if (ctx.itemStack.getItem() instanceof BucketItem i  && !(ctx.state != null && ctx.state.getBlock() instanceof AbstractCauldronBlock) && (bucketHitResult = mayInteractItemAt(ctx.level, ctx.player, ctx.itemStack, Item.getPlayerPOVHitResult(ctx.level, ctx.player, LegacyItemUtil.getBucketFluid(i) == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE))) != null) {
            BlockState state = ctx.level.getBlockState(bucketHitResult.getBlockPos());
            if (LegacyItemUtil.getBucketFluid(i) != Fluids.EMPTY) {
                BlockState resultState = state.getBlock() instanceof LiquidBlockContainer && LegacyItemUtil.getBucketFluid(i) == Fluids.WATER ? state : ctx.level.getBlockState(bucketHitResult.getBlockPos().relative(bucketHitResult.getDirection()));
                if (resultState.canBeReplaced(LegacyItemUtil.getBucketFluid(i)) || resultState.isAir() || resultState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(/*? if >=1.20.2 {*/ctx.player, /*?}*/ctx.level, bucketHitResult.getBlockPos(), resultState, LegacyItemUtil.getBucketFluid(i)))
                    return LegacyComponents.EMPTY;
            } else if (state.getBlock() instanceof BucketPickup && !state.getFluidState().isEmpty()) return LegacyComponents.COLLECT;
        }
        return null;
    });

    ActionHolder WATER_CAULDRON_INTERACTIONS = registerBlockUseItemOn("water_cauldron_interactions", ctx -> {
        if (ctx.level.getBlockEntity(ctx.pos) instanceof WaterCauldronBlockEntity be) {
            boolean dyedItem = LegacyItemUtil.isDyedItem(ctx.itemStack);
            boolean isDyeable = LegacyItemUtil.isDyeableItem(ctx.itemStack.typeHolder());
            if (isDyeable) {
                if (be.waterColor == null && dyedItem)
                    return LegacyComponents.CLEAN;
                if (be.waterColor != null)
                    return LegacyComponents.DYE;
            }
            if (LegacyItemUtil.getDyeColorOrNull(ctx.itemStack.getItem()) != null)
                return LegacyComponents.MIX;
        }
        return null;
    });


    ActionHolder PLANT_FLOWER_POT = registerBlockUseItemOn("plant_flower_pot", ctx -> ctx.state.getBlock() instanceof FlowerPotBlock pot && /*? if <1.20.2 {*//*pot.getContent()*//*?} else {*/pot.getPotted()/*?}*/ == Blocks.AIR && ctx.itemStack.getItem() instanceof BlockItem b && FlowerPotBlockAccessor.getPottedByContent().containsKey(b.getBlock()), LegacyComponents.PLANT);
    ActionHolder CARVE_PUMPKIN = registerBlockUseItemOn("carve_pumpkin", ctx -> ctx.state.getBlock() instanceof PumpkinBlock && ctx.itemStack.is(Items.SHEARS), LegacyComponents.CARVE);
    ActionHolder BRUSH_BLOCK = registerBlockUseItemOn("brush_block", ctx -> ctx.state.getBlock() instanceof BrushableBlock && ctx.itemStack.is(Items.BRUSH), LegacyComponents.BRUSH);

    ActionHolder PLACE_ITEM = registerUseItemOn("place_item", ctx -> {
        if (canPlace(ctx)) {
            if (ctx.itemStack.getItem() instanceof BlockItem b && b.getBlock() instanceof LanternBlock && isHangingLanternPlacement(ctx))
                return LegacyComponents.HANG;
            return ctx.itemStack.getItem() instanceof BlockItem b && isPlant(b.getBlock()) ? LegacyComponents.PLANT : LegacyComponents.PLACE;
        }
        return null;
    });
    ActionHolder HANG_ITEM = registerUseItemOn("hang_item", ControlTooltip::canHang, LegacyComponents.HANG);
    ActionHolder TILL = registerUseItemOn("till", ControlTooltip::canTill, LegacyComponents.TILL);
    ActionHolder PEEL_BARK = registerUseItemOn("peel_bark", ctx -> ctx.itemStack.getItem() instanceof AxeItem && AxeItem.STRIPPABLES.get(ctx.state.getBlock()) != null && !(ctx.hand.equals(InteractionHand.MAIN_HAND) && ctx.player.getOffhandItem().is(Items.SHIELD) && !ctx.player.isSecondaryUseActive()), LegacyComponents.PEEL_BARK);
    ActionHolder DIG_PATH = registerUseItemOn("dig_path", ctx -> ctx.itemStack.getItem() instanceof ShovelItem && ctx.level.getBlockState(ctx.pos.above()).isAir() && ShovelItem.FLATTENABLES.get(ctx.state.getBlock()) != null, LegacyComponents.DIG_PATH);
    ActionHolder DOUSE = registerUseItemOn("douse", ctx -> ctx.itemStack.getItem() instanceof ShovelItem && ctx.state.getBlock() instanceof CampfireBlock && ctx.state.getValue(CampfireBlock.LIT), LegacyComponents.DOUSE);

    ActionHolder WAX_BLOCK = registerUseItemOn("wax_block", ctx -> ctx.itemStack.is(Items.HONEYCOMB) && HoneycombItem.WAXABLES.get().containsKey(ctx.state.getBlock()), LegacyComponents.WAX);
    ActionHolder SCRAPE_BLOCK = registerUseItemOn("scrape_block", ctx -> ctx.itemStack.getItem() instanceof AxeItem && (HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(ctx.state.getBlock()) || WeatheringCopper.getPrevious(ctx.state).isPresent()), LegacyComponents.SCRAPE);
    ActionHolder IGNITE = registerUseItemOn("ignite", ctx -> (ctx.itemStack.getItem() instanceof FlintAndSteelItem || ctx.itemStack.getItem() instanceof FireChargeItem) && (BaseFireBlock.canBePlacedAt(ctx.level, ctx.pos.relative(ctx.hitResult.getDirection()), ctx.player.getDirection()) || CampfireBlock.canLight(ctx.state) || CandleBlock.canLight(ctx.state) || CandleCakeBlock.canLight(ctx.state)), LegacyComponents.IGNITE);
    ActionHolder IGNITE_CANDLE_CAKE = registerUseItemOn("ignite_candle_cake", ctx -> (ctx.itemStack.getItem() instanceof FlintAndSteelItem || ctx.itemStack.getItem() instanceof FireChargeItem) && ctx.state.getBlock() instanceof CandleCakeBlock && CandleCakeBlock.canLight(ctx.state), LegacyComponents.IGNITE);

    ActionHolder COLLECT_GLASS_BOTTLE = registerUseItem("collect_glass_bottle", ctx -> {
        if (ctx.itemStack.is(Items.GLASS_BOTTLE)) {
            BlockHitResult hit = Item.getPlayerPOVHitResult( ctx.level, ctx.player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && ctx.level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
                return LegacyComponents.COLLECT;
        }
        return null;
    });
    ActionHolder MOISTEN = registerUseItemOn("moisten", ctx -> LegacyItemUtil.isWaterBottle(ctx.itemStack) && ctx.state.is(BlockTags.CONVERTABLE_TO_MUD), LegacyComponents.MOISTEN);
    ActionHolder LODESTONE_COMPASS = registerUseItemOn("lodestone_compass", ctx -> ctx.itemStack.getItem() instanceof CompassItem && ctx.state.is(Blocks.LODESTONE), LegacyComponents.DIRECT);
    ActionHolder SHEAR_PLANT = registerUseItemOn("shear_plant", ctx -> ctx.itemStack.getItem() instanceof ShearsItem && canShearPlant(ctx.state), LegacyComponents.HANG);
    ActionHolder BONEMEAL_PLANT = registerUseItemOn("bonemeal_plant", ctx -> ctx.itemStack.getItem() instanceof BoneMealItem && ctx.state.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(ctx.level, ctx.pos, ctx.state/*? if <=1.20.2 {*//*,true*//*?}*/), LegacyComponents.GROW);
    ActionHolder LAUNCH_FIREWORK = registerUseItemOn("launch_firework", ctx -> ctx.itemStack.getItem() instanceof FireworkRocketItem, LegacyComponents.LAUNCH);
    ActionHolder PLACE_BOAT = registerUseItem("place_boat", ctx -> ctx.itemStack.getItem() instanceof BoatItem && canPlaceBoat(ctx), LegacyComponents.PLACE);
    ActionHolder PLACE_ON_WATER = registerUseItem("place_on_water", ctx -> (ctx.itemStack.is(Items.LILY_PAD) || ctx.itemStack.is(Items.FROGSPAWN)) && canPlaceOnWater(ctx), LegacyComponents.PLACE);
    ActionHolder BLOCK = registerUseItem("block", ctx -> ctx.itemStack.getUseAnimation().equals(/*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.BLOCK) && (!(ctx.itemStack.getItem() instanceof ShieldItem) || LegacyGameRules.getSidedBooleanGamerule(ctx.player, LegacyGameRules.LEGACY_SHIELD_CONTROLS.get())), LegacyComponents.BLOCK);
    ActionHolder EQUIP_SWAP = registerUseItem("equip_swap", ControlTooltip::canEquipSwap, LegacyComponents.EQUIP);
    ActionHolder BOOST_VEHICLE = registerUseItem("boost_vehicle", ControlTooltip::canBoost, LegacyComponents.BOOST);
    ActionHolder THROW_CHARGE_TRIDENT = registerUseItem("throw_charge_trident", ctx -> {
        if (ctx.itemStack.getItem() instanceof TridentItem) {
            float riptide = EnchantmentHelper./*? if <1.20.5 {*//*getRiptide(ctx.itemStack)*//*?} else {*/getTridentSpinAttackStrength(ctx.itemStack, ctx.player)/*?}*/;
            if (ctx.player.getUseItem() == ctx.itemStack) {
                if (riptide > 0.0F)
                    return ctx.player.getTicksUsingItem() >= 10 ? LegacyComponents.DASH : LegacyComponents.CHARGE;
                return LegacyComponents.THROW;
            } else if (riptide <= 0.0F || ctx.player.isInWaterOrRain())
                return LegacyComponents.CHARGE;
        }
        return null;
    });
    ActionHolder THROW_PROJECTILE = registerUseItem("throw_projectile", ctx -> ctx.itemStack.getItem() instanceof EggItem || ctx.itemStack.getItem() instanceof SnowballItem || ctx.itemStack.getItem() instanceof EnderpearlItem || ctx.itemStack.getItem() instanceof EnderEyeItem || ctx.itemStack.getItem() instanceof ThrowablePotionItem || ctx.itemStack.getItem() instanceof ExperienceBottleItem || ctx.itemStack.getItem() instanceof WindChargeItem, LegacyComponents.THROW);
    ActionHolder BOOST_FIREWORK = registerUseItem("boost_firework", ctx -> ctx.itemStack.getItem() instanceof FireworkRocketItem && ctx.player.isFallFlying(), LegacyComponents.LAUNCH);
    ActionHolder DRAW_BOW = registerUseItem("draw_bow", ctx -> (ctx.itemStack.getItem() instanceof BowItem || ctx.itemStack.getItem() instanceof CrossbowItem) && !ctx.player.isUsingItem() && !ctx.player.getProjectile(ctx.itemStack).isEmpty(), LegacyComponents.DRAW);
    ActionHolder RELEASE_BOW = registerUseItem("release_bow", ctx -> (ctx.itemStack.getItem() instanceof BowItem && ctx.player.isUsingItem() && ctx.player.getUseItem() == ctx.itemStack) || (ctx.itemStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(ctx.itemStack)), LegacyComponents.RELEASE);
    ActionHolder ZOOM_SPYGLASS = registerUseItem("zoom_spyglass", ctx -> ctx.itemStack.getItem() instanceof SpyglassItem, LegacyComponents.ZOOM);
    ActionHolder OPEN_WRITABLE_BOOK = registerUseItem("open_writable_book", ctx -> ctx.itemStack.getItem() instanceof WritableBookItem, LegacyComponents.OPEN);
    ActionHolder READ_WRITTEN_BOOK = registerUseItem("open_written_book", ctx -> ctx.itemStack.getItem() instanceof WrittenBookItem, LegacyComponents.READ);
    ActionHolder REEL_FISHING_ROD = registerUseItem("reel_fishing_rod", ctx -> ctx.itemStack.getItem() instanceof FishingRodItem && ctx.player.fishing != null, LegacyComponents.REEL);
    ActionHolder CAST_FISHING_ROD = registerUseItem("cast_fishing_rod", ctx -> ctx.itemStack.getItem() instanceof FishingRodItem && ctx.player.fishing == null, LegacyComponents.REEL);
    ActionHolder BLOW_INSTRUMENT = registerUseItem("blow_instrument", ctx -> ctx.itemStack.getItem() instanceof InstrumentItem && !ctx.player.isUsingItem(), LegacyComponents.BLOW);
    ActionHolder RELEASE_BUNDLE = registerUseItem("release_bundle", ctx -> isBundle(ctx.itemStack) && BundleItem.getFullnessDisplay(ctx.itemStack) > 0, LegacyComponents.RELEASE);
    ActionHolder CONSUME_ITEM = registerUseItem("consume_item", ctx -> isConsumable(ctx.itemStack, ctx.player) ? isDrinkable(ctx.itemStack) ? LegacyComponents.DRINK : LegacyComponents.EAT : null);


    record EntityInteract(Entity entity, Player player, InteractionHand hand, ItemStack handItem, Vec3 location) {

    }

    record BlockUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

    }

    record BlockUseItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        public BlockUseItemOn(Player player, InteractionHand hand, BlockHitResult hitResult) {
            this(player.getItemInHand(hand), player.level().getBlockState(hitResult.getBlockPos()), player.level(), hitResult.getBlockPos(), player, hand, hitResult);
        }
    }

    record UseItem(ItemStack itemStack, Level level, Player player, InteractionHand hand) {

    }

    static Component getActualUse(Minecraft minecraft) {
        if (minecraft.player.isHandsBusy() || (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation())))
            return null;

        for (ActionHolder value : GENERIC_USES.values()) {
            Component c = value.getAction(minecraft.player);
            if (c != null) return c;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack actualItem = minecraft.player.getItemInHand(hand);

            //Not sure why, but this is done in vanilla instead of continue
            if (!actualItem.isItemEnabled(minecraft.level.enabledFeatures())) return null;

            if (minecraft.hitResult instanceof EntityHitResult entityHitResult && !ENTITY_INTERACT_ACTIONS.isEmpty()) {
                EntityInteract entityInteract = new EntityInteract(entityHitResult.getEntity(), minecraft.player, hand, actualItem, entityHitResult.getLocation());
                for (ActionHolder value : ENTITY_INTERACT_ACTIONS.values()) {
                    ResultAction action = value.getResultAction(entityInteract);
                    if (action.canReturn()) return action.action;
                }
            }

            if (minecraft.hitResult instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {

                boolean haveSomethingInOurHands = !minecraft.player.getMainHandItem().isEmpty() || !minecraft.player.getOffhandItem().isEmpty();
                boolean suppressUsingBlock = minecraft.player.isSecondaryUseActive() && haveSomethingInOurHands;

                if (!BLOCK_USE_ACTIONS.isEmpty() && !suppressUsingBlock) {
                    BlockUse blockUse = new BlockUse(minecraft.level.getBlockState(hitResult.getBlockPos()), minecraft.level, hitResult.getBlockPos(), minecraft.player, hitResult);

                    if (blockUse.state.getBlock() instanceof ActionHolder value) {
                        ResultAction action = value.getResultAction(blockUse);
                        if (action.canReturn()) return action.action;
                    }

                    for (ActionHolder value : BLOCK_USE_ACTIONS.values()) {
                        ResultAction action = value.getResultAction(blockUse);
                        if (action.canReturn()) return action.action;
                    }
                }

                if (!BLOCK_USE_ITEM_ON_ACTIONS.isEmpty() || !ITEM_USE_ON_ACTIONS.isEmpty()) {
                    BlockUseItemOn blockUse = new BlockUseItemOn(actualItem, minecraft.level.getBlockState(hitResult.getBlockPos()), minecraft.level, hitResult.getBlockPos(), minecraft.player, hand, hitResult);

                    if (!suppressUsingBlock) {
                        if (blockUse.state.getBlock() instanceof ActionHolder value) {
                            ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action;
                        }

                        for (ActionHolder value : BLOCK_USE_ITEM_ON_ACTIONS.values()) {
                            ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action;
                        }
                    }

                    if (!actualItem.isEmpty() && !minecraft.player.getCooldowns().isOnCooldown(actualItem)) {
                        if (actualItem.getItem() instanceof ActionHolder value) {
                            ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action;
                        }

                        for (ActionHolder value : ITEM_USE_ON_ACTIONS.values()) {
                            ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action;
                        }
                    }
                }
            }

            if (!ITEM_USE_ACTIONS.isEmpty() && !actualItem.isEmpty() && !minecraft.player.getCooldowns().isOnCooldown(actualItem)) {
                UseItem useItem = new UseItem(actualItem, minecraft.level, minecraft.player, hand);

                if (actualItem.getItem() instanceof ActionHolder value) {
                    ResultAction action = value.getResultAction(useItem);
                    if (action.canReturn()) return action.action;
                }

                for (ActionHolder value : ITEM_USE_ACTIONS.values()) {
                    ResultAction action = value.getResultAction(useItem);
                    if (action.canReturn()) return action.action;
                }
            }
        }
       return null;
    }

    static boolean canSleep(BlockUse ctx) {
        return !ctx.player.isSleeping() && ctx.player.isAlive() && ctx.level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, ctx.pos).canSleep(ctx.level);
    }

    static BlockHitResult mayInteractItemAt(Level level, Player player, ItemStack itemStack, HitResult result) {
        if (result instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && level.mayInteract(player, r.getBlockPos()) && player.mayUseItemAt(r.getBlockPos().relative(r.getDirection()), r.getDirection(), itemStack)) {
            return r;
        }
        return null;
    }

    static boolean isHoldingBoneMeal(Player player) {
        return player.getMainHandItem().is(Items.BONE_MEAL) || player.getOffhandItem().is(Items.BONE_MEAL);
    }

    static boolean canHarvestSweetBerries(BlockState state, Player player) {
        if (!(state.getBlock() instanceof SweetBerryBushBlock)) return false;
        int age = state.getValue(SweetBerryBushBlock.AGE);
        return age > 1 && (age < SweetBerryBushBlock.MAX_AGE || !isHoldingBoneMeal(player));
    }

    static boolean canHarvestGlowBerries(BlockState state) {
        return CaveVines.hasGlowBerries(state);
    }

    static boolean canSetLoveMode(EntityInteract ctx) {
        return (ctx.entity instanceof Animal a && !a.isBaby() && a.isFood(ctx.handItem) && a.canFallInLove() && !a.isInLove() && (!(a instanceof AbstractHorse) || isLoveFood(a, ctx.handItem)));
    }

    static boolean canUnlockVault(BlockState state, ItemStack item) {
        if (!(state != null && state.getBlock() instanceof VaultBlock) || state.getValue(VaultBlock.STATE) != VaultState.ACTIVE)
            return false;
        return state.getValue(VaultBlock.OMINOUS) ? item.is(Items.OMINOUS_TRIAL_KEY) : item.is(Items.TRIAL_KEY);
    }

    static boolean canFeed(EntityInteract ctx) {
        return (ctx.entity instanceof Animal a && a.isFood(ctx.handItem) && (!(a instanceof AbstractHorse) && a.isBaby() || a instanceof AbstractHorse h && (a instanceof Llama || (a.isBaby() || !ctx.handItem.is(Items.HAY_BLOCK))) && (!h.isTamed() || !isLoveFood(a, ctx.handItem) && a.getHealth() < a.getMaxHealth() && !ctx.player.isSecondaryUseActive()))) || (ctx.entity instanceof Panda panda && ctx.handItem.is(Items.BAMBOO) && panda.isFood(ctx.handItem) && !panda.isEating() && !panda.canFallInLove()) || ctx.entity instanceof Dolphin && ctx.handItem.is(ItemTags.FISHES);
    }

    static boolean canFeedWithGoldenDandelion(Entity entity, ItemStack usedItem) {
        return entity instanceof AgeableMob mob && AgeableMob.canUseGoldenDandelion(usedItem, mob.isBaby(), 0, mob);
    }

    static boolean isLoveFood(Animal a, ItemStack stack) {
        return (a instanceof Llama && stack.is(Items.HAY_BLOCK)) || a instanceof Horse && ((stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)));
    }

    static boolean canShearEquipment(LivingEntity entity, EquipmentSlot slot) {
        ItemStack item = entity.getItemBySlot(slot);
        return item.has(DataComponents.EQUIPPABLE) && item.get(DataComponents.EQUIPPABLE).canBeSheared();
    }

    //? if >=1.21.11 {
    static boolean canOpenNautilusInventory(Player player, AbstractNautilus nautilus) {
        return !nautilus.isBaby() && nautilus.isTame() && player.isSecondaryUseActive() && (!nautilus.isVehicle() || nautilus.hasPassenger(player));
    }

    static boolean canEquipNautilus(AbstractNautilus nautilus, ItemStack item, EquipmentSlot slot) {
        return nautilus.canUseSlot(slot) && nautilus.isEquippableInSlot(item, slot) && nautilus.getItemBySlot(slot).isEmpty();
    }
    //?}


    static boolean canEquipSwap(UseItem ctx) {
        Equippable equippable = ctx.itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.swappable() && ctx.player.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(ctx.player.typeHolder());
    }

    static boolean canBoost(UseItem ctx) {
        Entity vehicle = ctx.player.getControlledVehicle();
        return ctx.itemStack.getItem() instanceof FoodOnAStickItem<?> i && vehicle instanceof ItemSteerable && vehicle.is(i.canInteractWith) &&
                (!(vehicle instanceof Pig pig) || !((ItemBasedSteeringAccessor)((PigAccessor)pig).getSteering()).getBoosting());
    }

    static boolean canPlace(BlockUseItemOn ctx) {
        if (ctx.itemStack.isEmpty())
            return false;
        if (ctx.itemStack.getItem() instanceof SpawnEggItem e)
            return true;
        if (ctx.itemStack.getItem() instanceof BlockItem b) {
            BlockPlaceContext c = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
            return c.canPlace() && ((BlockItemAccessor) b).getPlacementBlockState(c) != null;
        }
        return canPlaceArmorStand(ctx) || canPlaceMinecart(ctx) || canPlaceEndCrystal(ctx);
    }

    static boolean canPlaceArmorStand(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof ArmorStandItem) || ctx.hitResult.getDirection() == Direction.DOWN)
            return false;
        BlockPlaceContext context = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
        AABB box = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(context.getClickedPos()));
        return ctx.level.noCollision(null, box) && ctx.level.getEntities(null, box).isEmpty();
    }

    static boolean canPlaceMinecart(BlockUseItemOn ctx) {
        return ctx.itemStack.getItem() instanceof MinecartItem && ctx.level.getBlockState(ctx.hitResult.getBlockPos()).is(BlockTags.RAILS);
    }

    static boolean canPlaceEndCrystal(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof EndCrystalItem))
            return false;
        BlockPos above = ctx.pos.above();
        return (ctx.state.is(Blocks.OBSIDIAN) || ctx.state.is(Blocks.BEDROCK)) && ctx.level.isEmptyBlock(above) && ctx.level.getEntities(null, new AABB(above)).isEmpty();
    }

    static boolean canPlaceBoat(UseItem ctx) {
        BlockHitResult hitResult = Item.getPlayerPOVHitResult(ctx.level, ctx.player, ClipContext.Fluid.ANY);
        return hitResult.getType() == HitResult.Type.BLOCK && ctx.level.mayInteract(ctx.player, hitResult.getBlockPos());
    }

    static boolean canPlaceOnWater(UseItem ctx) {
        BlockHitResult hitResult = mayInteractItemAt(ctx.level, ctx.player, ctx.itemStack, Item.getPlayerPOVHitResult(ctx.level, ctx.player, ClipContext.Fluid.SOURCE_ONLY));
        return hitResult != null && ctx.level.getFluidState(hitResult.getBlockPos()).is(FluidTags.WATER);
    }

    static boolean isHangingLanternPlacement(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof BlockItem blockItem))
            return false;
        BlockPlaceContext context = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
        if (!context.canPlace())
            return false;
        BlockState state = ((BlockItemAccessor) blockItem).getPlacementBlockState(context);
        return state != null && state.hasProperty(BlockStateProperties.HANGING) && state.getValue(BlockStateProperties.HANGING);
    }

    static boolean canHang(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof HangingEntityItemAccessor hanging && (Block.canSupportCenter(ctx.level, ctx.hitResult.getBlockPos(), ctx.hitResult.getDirection()) || ctx.state.isSolid() || DiodeBlock.isDiode(ctx.state))))
            return false;

        if (hanging.getType() == EntityType.PAINTING)
            return Direction.Plane.HORIZONTAL.test(ctx.hitResult.getDirection());

        return hanging.getType() == EntityType.ITEM_FRAME || hanging.getType() == EntityType.GLOW_ITEM_FRAME;
    }

    static boolean canTill(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof HoeItem)) return false;
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> use = HoeItem.TILLABLES.get(ctx.level.getBlockState(ctx.pos).getBlock());
        return use != null && use.getFirst().test(new UseOnContext(ctx.player, ctx.hand, ctx.hitResult));
    }

    static boolean canShearPlant(BlockState state) {
        return state != null && state.getBlock() instanceof GrowingPlantHeadBlock plant && !plant.isMaxAge(state);
    }

    static boolean canTame(EntityInteract ctx) {
        return ((ctx.entity instanceof TamableAnimal t && !t.isTame() && ((t instanceof Wolf && ctx.handItem.is(Items.BONE)) || (!(t instanceof Wolf) && t.isFood(ctx.handItem)))) || (ctx.entity instanceof Parrot && (ctx.handItem.is(Items.WHEAT_SEEDS) || ctx.handItem.is(Items.MELON_SEEDS) || ctx.handItem.is(Items.PUMPKIN_SEEDS) || ctx.handItem.is(Items.BEETROOT_SEEDS))) || (ctx.hand == InteractionHand.MAIN_HAND && ctx.entity instanceof AbstractHorse h && !h.isTamed() && !ctx.player.isSecondaryUseActive() && ctx.handItem.isEmpty()));
    }

    static boolean isPlant(Block block) {
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof GrowingPlantBlock || block instanceof BambooStalkBlock || block instanceof CactusBlock || block instanceof SaplingBlock || block instanceof FlowerBlock || block instanceof DoublePlantBlock || block instanceof MushroomBlock || block instanceof CropBlock || block instanceof KelpPlantBlock || block instanceof SeagrassBlock || block instanceof StemBlock || block instanceof CocoaBlock;
    }

    static boolean isConsumable(ItemStack stack, Player player) {
        return stack.has(DataComponents.CONSUMABLE) && stack.get(DataComponents.CONSUMABLE).canConsume(player, stack);
    }

    static boolean isDrinkable(ItemStack stack) {
        return stack.getUseAnimation() == /*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.DRINK;
    }

    static boolean canDyeEntity(EntityInteract ctx) {
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(ctx.handItem.getItem());
        if (color == null || ctx.player == null)
            return false;
        return ctx.entity instanceof Sheep sheep && sheep.getColor() != color || ctx.entity instanceof Shulker shulker && shulker.getColor() != color || canDyeCollar(ctx.entity, ctx.player, color);
    }

    static boolean canDyeCollar(EntityInteract ctx) {
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(ctx.handItem.getItem());
        return color != null && ctx.player != null && canDyeCollar(ctx.entity, ctx.player, color);
    }

    static boolean canDyeCollar(Entity entity, Player player, DyeColor color) {
        return entity instanceof Wolf w && w.isTame() && w.isOwnedBy(player) && w.getCollarColor() != color || entity instanceof Cat c && c.isTame() && c.isOwnedBy(player) && c.getCollarColor() != color;
    }

    Icon getIcon();

    @Nullable
    Component getAction();

    interface Listener {
        Listener EMPTY = new Listener() {};

        static Listener of(Object o) {
            return o instanceof Listener e ? e : EMPTY;
        }

        default ControlTooltips getControlTooltips() {
            return getRenderer().tooltips();
        }

        default ControlTooltipRenderer getRenderer() {
            return ControlTooltipRenderer.getInstance();
        }

        default void setupControlTooltips() {
            addControlTooltips(getControlTooltips().list().clear());
        }

        default void addControlTooltips(ControlTooltipList list) {
            if (this instanceof Gui) setupGui(list, Minecraft.getInstance());
            if (this instanceof Screen s) {
                if (this instanceof LegacyMenuAccess<?> a) setupDefaultContainerScreen(list, a);
                else setupDefaultScreen(list, s);
            }
        }
    }

    record ResultAction(Component action, boolean canReturn) {
        public static final ResultAction PASS = new ResultAction(null, false);
        public static final ResultAction CANCEL = new ResultAction(null, true);

        public static ResultAction of(Component action) {
            return action == null ? pass() : new ResultAction(action, true);
        }

        public static ResultAction pass() {
            return PASS;
        }

        public static ResultAction cancel() {
            return CANCEL;
        }
    }

    @FunctionalInterface
    interface ActionHolder {

        default ResultAction getResultAction(Object ctx) {
            return ResultAction.of(getAction(ctx));
        }

        @Nullable
        Component getAction(Object ctx);

        @Nullable
        default Component getAction(Screen screen) {
            return getAction((ScreenContext) (() -> screen));
        }

        interface ScreenContext {
            Screen screen();
        }

        record KeyContext(KeyEvent keyEvent, Screen screen) implements ScreenContext {

            public int key() {
                return keyEvent.key();
            }

        }
    }

    @FunctionalInterface
    interface ResultActionHolder extends ActionHolder {
        @Override
        ResultAction getResultAction(Object ctx);

        @Override
        default Component getAction(Object ctx) {
            return getResultAction(ctx).action;
        }
    }

    class GuiManager implements ResourceManagerReloadListener {
        public static final List<ControlTooltip> controlTooltips = new ArrayList<>();

        public static <T> Predicate<T> staticPredicate(boolean b) {
            return o -> b;
        }

        protected ControlTooltip guiControlTooltipFromJson(JsonObject o) {
            LegacyKeyMapping mapping = LegacyKeyMapping.of(KeyMapping.ALL.get(GsonHelper.getAsString(o, "keyMapping")));
            BiPredicate<Item, DataComponentPatch> itemPredicate = o.has("heldItem") ? o.get("heldItem") instanceof JsonObject obj ? IOUtil.registryMatchesItem(obj) : o.get("heldItem").getAsBoolean() ? (i, t) -> i != null && i != Items.AIR : (i, t) -> false : (i, t) -> true;
            Predicate<Block> blockPredicate = o.has("hitBlock") ? o.get("hitBlock") instanceof JsonObject obj ? IOUtil.registryMatches(BuiltInRegistries.BLOCK, obj) : o.get("hitBlock").getAsBoolean() ? b -> !b.defaultBlockState().isAir() : b -> false : b -> true;
            Predicate<EntityType<?>> entityPredicate = o.has("hitEntity") ? o.get("hitEntity") instanceof JsonObject obj ? IOUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE, obj) : staticPredicate(o.get("hitEntity").getAsBoolean()) : e -> true;
            Minecraft minecraft = Minecraft.getInstance();
            String actionKey = o.get("action") instanceof JsonPrimitive p ? p.getAsString() : null;
            Component c = actionKey == null ? mapping.getDisplayName() : Component.translatable(actionKey);
            return create(mapping, () -> minecraft.player != null && (!"legacy.action.hit".equals(actionKey) || !isSpear(minecraft.player.getMainHandItem())) && itemPredicate.test(minecraft.player.getMainHandItem().getItem(), minecraft.player.getMainHandItem()./*? if <1.20.5 {*//*getTag*//*?} else {*/getComponentsPatch/*?}*/()) && ((minecraft.hitResult instanceof BlockHitResult r && blockPredicate.test(minecraft.level.getBlockState(r.getBlockPos()).getBlock())) || (minecraft.hitResult instanceof EntityHitResult er && entityPredicate.test(er.getEntity().getType()))) ? c : null);
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            controlTooltips.clear();
            manager.listResources(CONTROL_TOOLTIPS + "/gui", (string) -> string.getPath().endsWith(".json")).forEach((location, resource) -> {
                try {
                    BufferedReader bufferedReader = resource.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("tooltips");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e -> {
                            if (e instanceof JsonObject o) controlTooltips.add(guiControlTooltipFromJson(o));
                        });
                    else if (ioElement instanceof JsonObject o) controlTooltips.add(guiControlTooltipFromJson(o));
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            });
        }

        @Override
        public String getName() {
            return "legacy:control_tooltip_gui";
        }
    }

}
