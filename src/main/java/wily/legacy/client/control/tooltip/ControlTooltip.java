package wily.legacy.client.control.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
//? if >=1.21.11 {
//?}
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.LegacyControls;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.client.control.navigation.LegacyMenuAccess;
import wily.legacy.client.control.navigation.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.*;

public interface ControlTooltip {
    BiFunction<String, Style, ComponentIcon> CONTROL_ICON_FUNCTION = Util.memoize((s, style) -> ComponentIcon.of(Component.literal(s).withStyle(style)));
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

    static ComponentIcon getControlIcon(String s, ControlType type) {
        return CONTROL_ICON_FUNCTION.apply(s, type.styleOrEmpty());
    }

    static MutableComponent getAction(String key) {
        return CONTROL_ACTION_CACHE.apply(key);
    }

    static <T> Component getSelectAction(GuiEventListener listener, T context) {
        return listener.isFocused() && context instanceof ActionHolder.KeyContext c && c.key() == InputConstants.KEY_RETURN ? LegacyComponents.SELECT : null;
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
        return list.add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_NUMPADENTER) : ControllerBinding.DOWN_BUTTON.getIcon(), () -> getKeyMessage(InputConstants.KEY_NUMPADENTER, screen)).add(CommonIcon.PRESS::get, () -> getKeyMessage(InputConstants.KEY_RETURN, screen));
    }

    static ControlTooltipList setupDefaultScreen(ControlTooltipList list, Screen screen) {
        return setupDefaultButtons(list, screen).add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon(), () -> getKeyMessage(InputConstants.KEY_RETURN, screen)).add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(), () -> screen.shouldCloseOnEsc() ? CommonComponents.GUI_BACK : null);
    }

    static ControlTooltipList setupDefaultContainerScreen(ControlTooltipList list, LegacyMenuAccess<?> a) {
        return list.
                add(CommonIcon.MENU_MAIN_ACTION::get, () -> getMenuMainAction(a)).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(), () -> LegacyComponents.EXIT).
                add(CommonIcon.MENU_OFF_ACTION::get, () -> getMenuOffAction(a)).
                add(CommonIcon.MENU_QUICK_ACTION::get, () -> getMenuQuickAction(a)).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_W) : ControllerBinding.RIGHT_TRIGGER.getIcon(), () -> a.getHoveredSlot() != null && a.getHoveredSlot().hasItem() && !a.isMouseDragging() && LegacyTipManager.hasTip(a.getHoveredSlot().getItem()) ? LegacyComponents.WHATS_THIS : null).
                add(() -> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.LEFT_TRIGGER.getIcon(), () -> a.getMenu().getCarried().getCount() > 1 && !a.isOutsideClick(0) ? LegacyComponents.DISTRIBUTE : null);
    }

    static void setupGui(ControlTooltipList list) {
        list.tooltips.addAll(LegacyControls.guiControlTooltipManager.list());
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
            if (this instanceof Gui) setupGui(list);
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

}
