package wily.legacy.client.control.tooltip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
//? if >=1.21.11 {
//?}
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.IOUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

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
        Component action = UsePrediction.evaluate(minecraft);
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
