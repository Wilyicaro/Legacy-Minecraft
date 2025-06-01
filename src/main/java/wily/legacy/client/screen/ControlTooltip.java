package wily.legacy.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
//? if <1.21.5 {
//?} else {
/*import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
*///?}
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
//? if >=1.21.2 {
/*import net.minecraft.world.item.crafting.RecipePropertySet;
*///?}
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.ItemContainerPlatform;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryItemUtil;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.inventory.LegacySlot;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.mixin.base.FlowerPotBlockAccessor;
import wily.legacy.mixin.base.HangingEntityItemAccessor;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;

public interface ControlTooltip {
    String CONTROL_TOOLTIPS = "control_tooltips";

    BiFunction<String,Style,ComponentIcon> CONTROL_ICON_FUNCTION = Util.memoize((s,style)-> ComponentIcon.of(Component.literal(s).withStyle(style)));
    Function<Icon[], Icon> COMPOUND_ICON_FUNCTION = Util.memoize(Icon::createCompound);
    Function<ComponentIcon[], ComponentIcon> COMPOUND_COMPONENT_ICON_FUNCTION = Util.memoize(ComponentIcon::createCompound);

    String MOUSE_BASE_CHAR = "\uC002";
    String MOUSE_BASE_FOCUSED_CHAR = "\uC003";
    String KEY_CHAR = "\uC000";
    String KEY_PRESSED_CHAR = "\uC001";
    Component MORE = Component.literal("...").withStyle(ChatFormatting.GRAY);
    Component SPACE = Component.literal("  ");
    Component PLUS = Component.literal("+");
    ComponentIcon SPACE_ICON = ComponentIcon.of(SPACE);
    ComponentIcon PLUS_ICON = ComponentIcon.of(PLUS);

    static ComponentIcon getControlIcon(String s, ControlType type){
        return CONTROL_ICON_FUNCTION.apply(s,type.getStyle());
    }

    Function<String, MutableComponent> CONTROL_ACTION_CACHE = Util.memoize(s-> Component.translatable(s));

    static MutableComponent getAction(String key){
        return CONTROL_ACTION_CACHE.apply(key);
    }

    static Component getSelectAction(GuiEventListener listener, ActionHolder.Context context) {
        return listener.isFocused() && context instanceof ActionHolder.KeyContext c && c.key() == InputConstants.KEY_RETURN ? LegacyComponents.SELECT : null;
    }


    Icon getIcon();
    @Nullable
    Component getAction();


    Map<String,ArbitrarySupplier<ComponentIcon>> commonIcons = new HashMap<>();

    static ArbitrarySupplier<ComponentIcon> registerCommonComponentIcon(String key, ArbitrarySupplier<ComponentIcon> supplier){
        commonIcons.put(key, supplier);
        return supplier;
    }

    ArbitrarySupplier<ComponentIcon> PRESS = registerCommonComponentIcon("press", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> OPTION = registerCommonComponentIcon("option", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> EXTRA = registerCommonComponentIcon("extra", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> PLAYER_MOVEMENT = registerCommonComponentIcon("player_movement", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_W), getKeyIcon(InputConstants.KEY_A), getKeyIcon(InputConstants.KEY_S), getKeyIcon(InputConstants.KEY_D)}) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> POINTER_MOVEMENT = registerCommonComponentIcon("pointer_movement", ()-> ControlType.getActiveType().isKbm() ? getKbmIcon(MOUSE_BASE_CHAR) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> CAMERA_MOVEMENT = registerCommonComponentIcon("camera_movement", ()-> ControlType.getActiveType().isKbm() ? getKbmIcon(MOUSE_BASE_CHAR) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_MAIN_ACTION = registerCommonComponentIcon("menu_main_action", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.DOWN_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_OFF_ACTION = registerCommonComponentIcon("menu_off_action", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_RIGHT) : ControllerBinding.LEFT_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> MENU_QUICK_ACTION = registerCommonComponentIcon("menu_quick_action", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT), PLUS_ICON,getKeyIcon(InputConstants.KEY_LSHIFT)}) : ControllerBinding.UP_BUTTON.getIcon());
    ArbitrarySupplier<ComponentIcon> NAVIGATION = registerCommonComponentIcon("navigation", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_UP), getKeyIcon(InputConstants.KEY_LEFT), getKeyIcon(InputConstants.KEY_DOWN), getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> HORIZONTAL_NAVIGATION = registerCommonComponentIcon("horizontal_navigation", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_LEFT), getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> VERTICAL_NAVIGATION = registerCommonComponentIcon("vertical_navigation", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_UP), getKeyIcon(InputConstants.KEY_DOWN)}) : ControllerBinding.LEFT_STICK.getIcon());
    ArbitrarySupplier<ComponentIcon> LEFT_TAB = registerCommonComponentIcon("left_tab", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon());
    ArbitrarySupplier<ComponentIcon> RIGHT_TAB = registerCommonComponentIcon("right_tab", ()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon());

    ArbitrarySupplier<ComponentIcon> LEFT_CRAFTING_TYPE = registerCommonComponentIcon("left_crafting_type",  ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON, getKeyIcon(InputConstants.KEY_LBRACKET)}) : ControllerBinding.LEFT_TRIGGER.getIcon());
    ArbitrarySupplier<ComponentIcon> RIGHT_CRAFTING_TYPE = registerCommonComponentIcon("right_crafting_type", ()-> ControlType.getActiveType().isKbm() ? COMPOUND_COMPONENT_ICON_FUNCTION.apply(new ComponentIcon[]{getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON, getKeyIcon(InputConstants.KEY_RBRACKET)}) : ControllerBinding.RIGHT_TRIGGER.getIcon());
    ArbitrarySupplier<ComponentIcon> CANCEL_BINDING = registerCommonComponentIcon("cancel_binding", ()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.BACK.getIcon());

    static Component getKeyMessage(int key, Screen screen){
        for (GuiEventListener child : screen.children()) {
            Component component;
            if (child instanceof ActionHolder accessor && (component = accessor.getAction(new ActionHolder.KeyContext(key,screen))) != null) return component;
        }
        return null;
    }

    static Component getKeyboardAction(ActionHolder.KeyContext keyContext){
        return keyContext.key == InputConstants.KEY_NUMPADENTER && ControlType.getActiveType().isKbm() || keyContext.key == InputConstants.KEY_RETURN  && !ControlType.getActiveType().isKbm() ? LegacyComponents.SHOW_KEYBOARD : null;
    }

    static ControlTooltip.Renderer setupDefaultButtons(Renderer renderer, Screen screen){
        return renderer.add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_NUMPADENTER) : ControllerBinding.DOWN_BUTTON.getIcon(), ()-> getKeyMessage(InputConstants.KEY_NUMPADENTER, screen)).add(PRESS::get,()-> getKeyMessage(InputConstants.KEY_RETURN,screen));
    }

    static ControlTooltip.Renderer setupDefaultScreen(Renderer renderer, Screen screen){
        return setupDefaultButtons(renderer,screen).add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.getIcon(),()-> getKeyMessage(InputConstants.KEY_RETURN,screen)).add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(),()->screen.shouldCloseOnEsc() ? CommonComponents.GUI_BACK : null);
    }

    static ControlTooltip.Renderer setupDefaultContainerScreen(Renderer renderer, LegacyMenuAccess<?> a){
        return renderer.
                add(MENU_MAIN_ACTION::get,()-> getMenuMainAction(a)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.getIcon(),()-> LegacyComponents.EXIT).
                add(MENU_OFF_ACTION::get,()-> getMenuOffAction(a)).
                add(MENU_QUICK_ACTION::get,()-> getMenuQuickAction(a)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_W) : ControllerBinding.RIGHT_TRIGGER.getIcon(),()->a.getHoveredSlot() != null && a.getHoveredSlot().hasItem() && LegacyTipManager.hasTip(a.getHoveredSlot().getItem()) ? LegacyComponents.WHATS_THIS : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.LEFT_TRIGGER.getIcon(),()-> a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DISTRIBUTE : null);
    }

    static Component getIconComponentFromKeyMapping(LegacyKeyMapping mapping){
        ComponentIcon icon = getIconFromKeyMapping(mapping);
        return icon == null ? LegacyComponents.NONE : icon.getComponent();
    }

    static ComponentIcon getIconFromKeyMapping(LegacyKeyMapping mapping){
        return ControlType.getActiveType().isKbm() ? getKeyIcon(mapping.getKey().getValue()) : mapping.getBinding() == null ? null : mapping.getBinding().getIcon();
    }

    static Component getMenuMainAction(LegacyMenuAccess<?> a){
        if (a.isOutsideClick(0) && !a.getMenu().getCarried().isEmpty()) return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ALL : LegacyComponents.DROP;
        if (a.getHoveredSlot() != null && (a.getHoveredSlot().hasItem() || !a.getMenu().getCarried().isEmpty())){
            if (a.getHoveredSlot().hasItem() && !FactoryItemUtil.equalItems(a.getHoveredSlot().getItem(),a.getMenu().getCarried()) && !isBundleAndAcceptItem(a.getHoveredSlot().getItem(),a.getMenu().getCarried())){
                return a.getMenu().getCarried().isEmpty() ? LegacyComponents.TAKE : isBundleAndAcceptItem(a.getMenu().getCarried(),a.getHoveredSlot().getItem()) ? LegacyComponents.PICK_UP : LegacyComponents.SWAP;
            } else if (!a.getMenu().getCarried().isEmpty() && a.getHoveredSlot().mayPlace(a.getMenu().getCarried())) return a.getHoveredSlot().getMaxStackSize() == 1 ? LegacyComponents.PLACE_ONE : a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.PLACE_ALL : LegacyComponents.PLACE;
        }
        return null;
    }

    static Component getMenuOffAction(LegacyMenuAccess<?> a){
        if (a.isOutsideClick(1) && !a.getMenu().getCarried().isEmpty()) return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ONE : LegacyComponents.DROP;
        if (a.getHoveredSlot() != null){
            if (a.getMenu().getCarried().isEmpty()) {
                if (isBundle(a.getHoveredSlot().getItem()) && BundleItem.getFullnessDisplay(a.getHoveredSlot().getItem()) > 0) return LegacyComponents.PICK_UP;
                else if (a.getHoveredSlot().getItem().getCount() > 1) return LegacyComponents.TAKE_HALF;
            } else {
                if (a.getHoveredSlot().hasItem() && Legacy4JClient.hasModOnServer() && Legacy4J.canRepair(a.getHoveredSlot().getItem(),a.getMenu().getCarried())) return LegacyComponents.REPAIR;
                if (a.getHoveredSlot().hasItem() && Legacy4JClient.hasModOnServer() && Legacy4J.isDyeableItem(a.getHoveredSlot().getItem().getItemHolder()) && a.getMenu().getCarried().getItem() instanceof DyeItem) return LegacyComponents.DYE;
                else if (isBundle(a.getMenu().getCarried()) && BundleItem.getFullnessDisplay(a.getMenu().getCarried()) > 0 && !a.getHoveredSlot().hasItem()) return LegacyComponents.RELEASE;
                else if (a.getHoveredSlot().hasItem() && !a.getMenu().getCarried().isEmpty() && !FactoryItemUtil.equalItems(a.getMenu().getCarried(),a.getHoveredSlot().getItem()) && a.getHoveredSlot().mayPlace(a.getHoveredSlot().getItem())) return LegacyComponents.SWAP;
                else if (!a.getHoveredSlot().hasItem() && a.getHoveredSlot().mayPlace(a.getHoveredSlot().getItem())) return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.PLACE_ONE : LegacyComponents.PLACE;
            }
        }
        return null;
    }

    static Component getMenuQuickAction(LegacyMenuAccess<?> a){
        if (a.getHoveredSlot() != null && a.getHoveredSlot().hasItem()) {
            if (a.getMenu() instanceof InventoryMenu menu) {
                for (int i = 5; i < 9; i++) {
                    if (i == a.getHoveredSlot().index) break;
                    Slot slot = menu.getSlot(i);
                    if (LegacySlotDisplay.isVisibleAndActive(slot) && menu.getSlot(i).mayPlace(a.getHoveredSlot().getItem())) return LegacyComponents.EQUIP;
                }
            }
            return LegacyComponents.QUICK_MOVE;
        }
        return null;
    }

    static boolean isBundle(ItemStack stack){
        return /*? if <1.21.2 {*/stack.getItem() instanceof BundleItem/*?} else {*//*stack.is(ItemTags.BUNDLES)*//*?}*/;
    }

    static boolean isBundleAndAcceptItem(ItemStack stack, ItemStack itemToAccept){
        return isBundle(stack) && BundleItem.getFullnessDisplay(stack) <= (1-(float) itemToAccept.getCount()/itemToAccept.getMaxStackSize()) && !itemToAccept.isEmpty() && itemToAccept.getItem().canFitInsideContainerItems();
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

    static ControlTooltip create(LegacyKeyMapping mapping,Supplier<Component> action){
        return create(()-> getIconFromKeyMapping(mapping),action);
    }

    interface Icon {
        int render(GuiGraphics graphics, int x, int y, boolean allowPressed, boolean simulate);
        static Icon createCompound(Icon[] icons){
            return (graphics, x, y, allowPressed, simulate) -> {
                int totalWidth = 0;
                for (Icon icon : icons) totalWidth+= icon.render(graphics, x + totalWidth, y, allowPressed, simulate);
                return totalWidth;
            };
        }
    }

    interface ComponentIcon extends Icon {
        Component getComponent();

        static ComponentIcon of(Component component){
            return new ComponentIcon() {
                @Override
                public Component getComponent() {
                    return component;
                }

                @Override
                public int render(GuiGraphics graphics, int x, int y, boolean allowPressed, boolean simulate)  {
                    Font font = Minecraft.getInstance().font;
                    if (!simulate) graphics.drawString(font,getComponent(),x,y,0xFFFFFF,false);
                    return font.width(getComponent());
                };
            };
        }

        static ComponentIcon createCompound(ComponentIcon[] componentIcons){
            MutableComponent allIcons = Component.empty();
            for (ComponentIcon componentIcon : componentIcons) {
                allIcons.append(componentIcon.getComponent());
            }
            return of(allIcons);
        }

        static ComponentIcon createCompound(Component[] components){
            MutableComponent allIcons = Component.empty();
            for (Component component : components) {
                allIcons.append(component);
            }
            return of(allIcons);
        }

        static ComponentIcon compoundOf(ComponentIcon... componentIcons){
            return COMPOUND_COMPONENT_ICON_FUNCTION.apply(componentIcons);
        }
    }

    abstract class LegacyIcon implements ComponentIcon {
        public abstract Component getComponent(boolean allowPressed);
        public abstract Component getOverlayComponent(boolean allowPressed);
        public Component getComponent(){
            return getComponent(false);
        }
        public abstract boolean pressed();
        boolean lastPressed = false;
        long startPressTime = 0L;

        public abstract boolean canLoop();
        public float getPressInterval(){
            return (Util.getMillis() - startPressTime) / 280f;
        }
        public Component getActualIcon(char[] chars, boolean allowPressed, ControlType type){
            return chars == null ? null : ControlTooltip.getControlIcon(String.valueOf(chars[chars.length > 1 && allowPressed && startPressTime != 0 && (canLoop() || getPressInterval() <= 1) ? 1 + Math.round(((getPressInterval() / 2) <= 1.4f ? (getPressInterval() / 2f) % 1f : 0.4f) * (chars.length - 2)) : 0]),type).getComponent();
        }

        @Override
        public int render(GuiGraphics graphics, int x, int y, boolean allowPressed, boolean simulate){
            Component c = getComponent(allowPressed);
            Component co = getOverlayComponent(allowPressed);
            Font font = Minecraft.getInstance().font;
            int cw = c == null ? 0 : font.width(c);
            int cow = co == null ? 0 : font.width(co);
            if (!pressed() && getPressInterval() % 1 < 0.1) startPressTime = 0;
            if (allowPressed && pressed() && !lastPressed && startPressTime == 0) startPressTime = Util.getMillis();
            lastPressed = pressed();

            if (!simulate && c != null) {
                graphics.drawString(font,c,x + (co == null || cw > cow ? 0 : (cow - cw) / 2),y,0xFFFFFF,false);
            }
            if (!simulate && co != null){
                float rel = startPressTime == 0 ? 0 : canLoop() ? getPressInterval() % 1 : Math.min(getPressInterval(),1);
                float d = 1 - Math.max(0,(rel >= 0.5f ? 1 - rel: rel) * 2/5);

                graphics.pose().pushPose();
                graphics.pose().translate(x + (c == null || cow > cw ? (cow - cow * d) / 2 : (cw  - cow * d) / 2f),y + (9 - 9 * d) / 2 ,0);
                graphics.pose().scale(d,d,d);
                float alpha = FactoryGuiGraphics.of(graphics).getColor()[3];
                FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f,alpha * (0.8f + (rel >= 0.5f ? 0.2f : 0)));
                graphics.drawString(font,co,0,0,0xFFFFFF,false);
                FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f,alpha);
                graphics.pose().popPose();
            }
            return Math.max(cw,cow);
        }

        public static LegacyIcon create(InputConstants.Key key, char[] iconChars, char[] iconOverlayChars, Character tipIcon){
            return create(key, (k,b)-> create(b,iconChars,iconOverlayChars,tipIcon,()-> k.getType() != InputConstants.Type.MOUSE, ControlType::getKbmActiveType));
        }

        public static LegacyIcon create(BooleanSupplier pressed, char[] iconChars, char[] iconOverlayChars, Character tipIcon, BooleanSupplier loop, Supplier<ControlType> type){
            return new LegacyIcon() {

                @Override
                public Component getComponent(boolean allowPressed) {
                    return getActualIcon(iconChars,allowPressed,type.get());
                }

                @Override
                public Component getOverlayComponent(boolean allowPressed) {
                    return getActualIcon(iconOverlayChars,allowPressed,type.get());
                }

                @Override
                public Component getComponent() {
                    return tipIcon == null ? super.getComponent() == null ? getOverlayComponent(false) : super.getComponent() : getControlIcon(String.valueOf(tipIcon), ControlType.getActiveControllerType()).getComponent();
                }

                @Override
                public boolean pressed() {
                    return pressed.getAsBoolean();
                }

                @Override
                public boolean canLoop() {
                    return loop.getAsBoolean();
                }
            };
        }

        public static LegacyIcon create(InputConstants.Key key, BiFunction<InputConstants.Key,BooleanSupplier, LegacyIcon> iconGetter){
            long window = Minecraft.getInstance().getWindow().getWindow();
            return iconGetter.apply(key,()->(key.getType() == InputConstants.Type.KEYSYM ? InputConstants.isKeyDown(window, key.getValue()) : GLFW.glfwGetMouseButton(window, key.getValue()) == 1));
        }
    }


    class Renderer implements Renderable {
        public static final FactoryEvent<BiConsumer<Screen,ControlTooltip.Renderer>> SCREEN_EVENT = new FactoryEvent<>(e-> (screen, event)-> e.invokeAll(c->c.accept(screen,event)));
        public static final FactoryEvent<BiConsumer<Gui,ControlTooltip.Renderer>> GUI_EVENT = new FactoryEvent<>(e-> (screen, event)-> e.invokeAll(c->c.accept(screen,event)));

        static final Renderer INSTANCE = new Renderer();
        public static Renderer getInstance(){
            return INSTANCE;
        }
        private final Minecraft minecraft = Minecraft.getInstance();
        public final List<ControlTooltip> tooltips = new ArrayList<>();
        protected final Map<Component,Icon> renderTooltips = new Object2ReferenceLinkedOpenHashMap<>();

        public static Renderer of(Object o){
            return o instanceof Event e ? e.getControlTooltips() : getInstance();
        }

        public Renderer clear(){
            tooltips.clear();
            return this;
        }

        public Renderer set(int ordinal,Supplier<Icon> control, Supplier<Component> action){
            return set(ordinal,create(control,action));
        }

        public Renderer set(int ordinal, ControlTooltip tooltip){
            tooltips.set(ordinal,tooltip);
            return this;
        }

        public Renderer replace(int ordinal, Function<Icon,Icon> control, Function<Component,Component>  action){
            ControlTooltip old = tooltips.get(ordinal);
            return set(ordinal,ControlTooltip.create(()->control.apply(old.getIcon()),()->action.apply(old.getAction())));
        }

        public Renderer add(KeyMapping mapping){
            return add(LegacyKeyMapping.of(mapping));
        }

        public Renderer add(KeyMapping mapping, Supplier<Component> action){
            return add(create(LegacyKeyMapping.of(mapping),action));
        }

        public Renderer add(KeyMapping mapping, BooleanSupplier extraCondition){
            return add(create(LegacyKeyMapping.of(mapping), ()-> extraCondition.getAsBoolean() ? LegacyKeyMapping.of(mapping).getDisplayName() : null));
        }

        public Renderer add(LegacyKeyMapping mapping){
            return add(mapping,mapping::getDisplayName);
        }

        public Renderer add(LegacyKeyMapping mapping, Supplier<Component> action){
            return add(create(mapping,action));
        }

        public Renderer add(LegacyKeyMapping mapping, BooleanSupplier extraCondition){
            return add(create(mapping, ()-> extraCondition.getAsBoolean() ? mapping.getDisplayName() : null));
        }

        public Renderer addCompound(Supplier<Icon[]> control, Supplier<Component> action){
            return add(create(()-> COMPOUND_ICON_FUNCTION.apply(control.get()),action));
        }

        public Renderer add(Supplier<Icon> control, Supplier<Component>  action){
            return add(create(control,action));
        }

        public Renderer add(ControlTooltip tooltip){
            tooltips.add(tooltip);
            return this;
        }

        public boolean allowPressed(){
            return minecraft.screen != null;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, float f) {
            if (!LegacyOptions.inGameTooltips.get() && minecraft.screen == null || !LegacyOptions.displayControlTooltips.get()) return;
            renderTooltips.clear();
            for (ControlTooltip tooltip : tooltips) {
                Component action;
                Icon icon;
                if ((action = tooltip.getAction()) == null || (icon = tooltip.getIcon()) == null) continue;
                renderTooltips.compute(action, (k, existingIcon) -> existingIcon == null ? icon : existingIcon.equals(icon) || !LegacyOptions.displayMultipleControlsFromAction.get() ? existingIcon : COMPOUND_ICON_FUNCTION.apply(new Icon[]{existingIcon,SPACE_ICON, icon}));
            }
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f, Math.max(minecraft.screen == null ? 0.0f : 0.2f,  ScreenUtil.getHUDOpacity()));
            guiGraphics.pose().pushPose();
            boolean left = LegacyOptions.controlTooltipDisplay.get().isLeft();
            double hudDiff = (1 - LegacyOptions.hudDistance.get()) * 60D;
            double xDiff = -Math.min(hudDiff,30);
            guiGraphics.pose().translate(left ? xDiff : guiGraphics.guiWidth() - xDiff, Math.min(hudDiff,16),1000);
            int baseHeight = guiGraphics.guiHeight() - 29;

            renderTooltips.forEach((action,icon)->{
                if (left) {
                    int controlWidth = icon.render(guiGraphics, 32, baseHeight, allowPressed(), false);
                    if (controlWidth > 0) {
                        guiGraphics.drawString(minecraft.font, action, 34 + controlWidth, baseHeight, CommonColor.ACTION_TEXT.get());
                        guiGraphics.pose().translate(controlWidth + minecraft.font.width(action) + 12, 0, 0);
                        guiGraphics.flush();
                    }
                } else {
                    int controlWidth = icon.render(guiGraphics, -32, baseHeight, allowPressed(), true);
                    if (controlWidth > 0) {
                        guiGraphics.pose().translate(-controlWidth - minecraft.font.width(action), 0, 0);
                        icon.render(guiGraphics, -32, baseHeight, allowPressed(), false);
                        guiGraphics.drawString(minecraft.font, action, -30 + controlWidth, baseHeight, CommonColor.ACTION_TEXT.get());
                        guiGraphics.pose().translate(-12, 0, 0);
                        guiGraphics.flush();
                    }
                }
            });
            guiGraphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
            FactoryScreenUtil.disableBlend();
        }
    }

    static ComponentIcon getKbmIcon(String key){
        return getControlIcon(key, ControlType.getKbmActiveType());
    }

    static LegacyIcon getKeyIcon(int i){
        InputConstants.Type type = i >= 0 ? i <= 9 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM : null;
        if (type == null) return null;
        InputConstants.Key key = type.getOrCreate(i);
        return ControlType.getKbmActiveType().getIcons().computeIfAbsent(key.getName(), i2-> LegacyIcon.create(key,(k, b)->new LegacyIcon() {
            @Override
            public Component getComponent(boolean allowPressed) {
                return getControlIcon(k.getType() == InputConstants.Type.MOUSE ? b.getAsBoolean() && allowPressed ? MOUSE_BASE_FOCUSED_CHAR : MOUSE_BASE_CHAR : b.getAsBoolean() && allowPressed ? KEY_PRESSED_CHAR : KEY_CHAR,ControlType.getKbmActiveType()).getComponent();
            }

            @Override
            public Component getOverlayComponent(boolean allowPressed) {
                return k.getDisplayName();
            }

            @Override
            public boolean pressed() {
                return b.getAsBoolean();
            }
            @Override
            public boolean canLoop() {
                return k.getType() != InputConstants.Type.MOUSE;
            }
        }));
    }

    interface Event {
        Event EMPTY = new Event(){};

        static Event of(Object o){
            return o instanceof Event e ? e : EMPTY;
        }

        default ControlTooltip.Renderer getControlTooltips(){
            return ControlTooltip.Renderer.getInstance();
        }

        default void setupControlTooltips(){
            addControlTooltips(getControlTooltips().clear());
        }

        default void addControlTooltips(ControlTooltip.Renderer renderer){
            if (this instanceof Gui) GuiManager.applyGUIControlTooltips(renderer,Minecraft.getInstance());
            if (this instanceof Screen s){
                if (this instanceof LegacyMenuAccess<?> a) setupDefaultContainerScreen(renderer,a);
                else setupDefaultScreen(renderer,s);
            }
        }
    }

    class GuiManager implements ResourceManagerReloadListener {
        public static final List<ControlTooltip> controlTooltips = new ArrayList<>();

        public static void applyGUIControlTooltips(Renderer renderer, Minecraft minecraft){
            renderer.add(minecraft.options.keyJump,()-> minecraft.player.isUnderWater() ? LegacyComponents.SWIM_UP : null).add(minecraft.options.keyInventory, ()-> !minecraft.gameMode.isServerControlledInventory() || !(minecraft.player.getVehicle() instanceof AbstractHorse h) || h.isTamed()).add(Legacy4JClient.keyCrafting).add(minecraft.options.keyUse,()-> getActualUse(minecraft)).add(minecraft.options.keyAttack,()->getMainAction(minecraft));
            renderer.tooltips.addAll(controlTooltips);
            renderer.add(minecraft.options.keyShift,()-> minecraft.player.isPassenger() ? minecraft.player.getVehicle() instanceof LivingEntity ? LegacyComponents.DISMOUNT : LegacyComponents.EXIT : minecraft.player./*? if >=1.21 {*/getInBlockState/*?} else {*//*getFeetBlockState*//*?}*/().is(Blocks.SCAFFOLDING) ? LegacyComponents.HOLD_TO_DESCEND : null).add(minecraft.options.keyPickItem,()-> getPickAction(minecraft));
        }

        protected ControlTooltip guiControlTooltipFromJson(JsonObject o){
            LegacyKeyMapping mapping = LegacyKeyMapping.of(KeyMapping.ALL.get(GsonHelper.getAsString(o, "keyMapping")));
            BiPredicate<Item, /*? if <1.20.5 {*//*CompoundTag*//*?} else {*/DataComponentPatch/*?}*/> itemPredicate = o.has("heldItem") ? o.get("heldItem") instanceof JsonObject obj ? JsonUtil.registryMatchesItem(obj) : o.get("heldItem").getAsBoolean() ? (i, t)-> i != null && i != Items.AIR : (i, t)-> false : (i, t)-> true;
            Predicate<Block> blockPredicate = o.has("hitBlock") ? o.get("hitBlock") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.BLOCK,obj) : o.get("hitBlock").getAsBoolean() ? b-> !b.defaultBlockState().isAir() : b-> false : b-> true;
            Predicate<EntityType<?>> entityPredicate = o.has("hitEntity") ? o.get("hitEntity") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE,obj) : staticPredicate(o.get("hitEntity").getAsBoolean()) : e-> true;
            Minecraft minecraft = Minecraft.getInstance();
            Component c = o.get("action") instanceof JsonPrimitive p ? Component.translatable(p.getAsString()) : mapping.getDisplayName();
            return create(mapping, ()->minecraft.player != null && itemPredicate.test(minecraft.player.getMainHandItem().getItem(),minecraft.player.getMainHandItem()./*? if <1.20.5 {*//*getTag*//*?} else {*/getComponentsPatch/*?}*/()) && ((minecraft.hitResult instanceof BlockHitResult r && blockPredicate.test(minecraft.level.getBlockState(r.getBlockPos()).getBlock())) || (minecraft.hitResult instanceof EntityHitResult er && entityPredicate.test(er.getEntity().getType()))) ? c : null);
        }

        public static <T> Predicate<T> staticPredicate(boolean b){
            return o-> b;
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

    static Component getPickAction(Minecraft minecraft){
        ItemStack result;
        BlockState b;
        if ((minecraft.hitResult instanceof EntityHitResult r &&  (result = r.getEntity().getPickResult()) != null || minecraft.hitResult instanceof BlockHitResult h && h.getType() != HitResult.Type.MISS && !(result = (b = minecraft.level.getBlockState(h.getBlockPos()))/*? if <1.21.4 {*/.getBlock()/*?}*/.getCloneItemStack(minecraft.level,h.getBlockPos(),/*? if >=1.21.4 {*//*true*//*?} else {*/b/*?}*/)).isEmpty()) && (Legacy4JClient.playerHasInfiniteMaterials() || minecraft.player.getInventory().findSlotMatchingItem(result) != -1))
            return minecraft.hitResult instanceof EntityHitResult ? LegacyComponents.PICK_ENTITY : ((LegacyKeyMapping) minecraft.options.keyPickItem).getDisplayName();

        return null;
    }

    static Component getMainAction(Minecraft minecraft){
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation().x(),minecraft.hitResult.getLocation().z())) return null;
        if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS){
            BlockState state = minecraft.level.getBlockState(r.getBlockPos());
            if (state.getBlock() instanceof NoteBlock && !minecraft.player.getAbilities().instabuild) return LegacyComponents.PLAY;
            else if ((minecraft.player.getAbilities().instabuild || state.getBlock().defaultDestroyTime() >= 0 && !minecraft.player.blockActionRestricted(minecraft.level,r.getBlockPos(),minecraft.gameMode.getPlayerMode()))) return LegacyComponents.MINE;
        }
        return null;
    }

    static Component getActualUse(Minecraft minecraft){
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation().x(),minecraft.hitResult.getLocation().z())) return null;

        BlockHitResult blockHit = minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS ? r : null;
        BlockState blockState = blockHit == null ? null : minecraft.level.getBlockState(blockHit.getBlockPos());
        Entity entity = minecraft.hitResult instanceof EntityHitResult r ? r.getEntity() : null;
        if (minecraft.player.isSleeping()) return LegacyComponents.WAKE_UP;
        if ((blockState != null && (blockState.getBlock() instanceof ButtonBlock || blockState.getBlock() instanceof LeverBlock || blockState.getBlock() instanceof DoorBlock || blockState.getBlock() instanceof TrapDoorBlock || blockState.getBlock() instanceof SignBlock || blockState.getBlock() instanceof FenceGateBlock || blockState.getBlock() instanceof EnderChestBlock || (blockState.getMenuProvider(minecraft.level,blockHit.getBlockPos()) != null || minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof MenuProvider)))) return (blockState.getBlock() instanceof AbstractChestBlock<?> || blockState.getBlock() instanceof ShulkerBoxBlock || blockState.getBlock() instanceof BarrelBlock || blockState.getBlock() instanceof HopperBlock || blockState.getBlock() instanceof DropperBlock) ? LegacyComponents.OPEN : LegacyComponents.USE;
        if (minecraft.hitResult instanceof EntityHitResult r && (r.getEntity() instanceof AbstractVillager m && (!(m instanceof Villager v) || /*? if <1.21.5 {*/v.getVillagerData().getProfession() != VillagerProfession.NONE/*?} else {*//*!v.getVillagerData().profession().is(VillagerProfession.NONE)*//*?}*/) && !m.isTrading())) return LegacyComponents.TRADE;
        if (entity instanceof ItemFrame itemFrame && !itemFrame.getItem().isEmpty()) return LegacyComponents.ROTATE;

        if (blockState != null && blockState.getBlock() instanceof BedBlock) return LegacyComponents.SLEEP;
        if (blockState != null && blockState.getBlock() instanceof NoteBlock) return LegacyComponents.CHANGE_PITCH;
        if (blockState != null && blockState.getBlock() instanceof ComposterBlock && blockState.getValue(ComposterBlock.LEVEL) == 8) return LegacyComponents.COLLECT;
        if (blockState != null && blockState.getBlock() instanceof JukeboxBlock && blockState.getValue(HAS_RECORD)) return LegacyComponents.EJECT;
        if (blockState != null && blockState.getBlock() instanceof DaylightDetectorBlock) return LegacyComponents.INVERT;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack actualItem = minecraft.player.getItemInHand(hand);

            if (actualItem.isEmpty()) continue;

            if (blockHit != null && Legacy4J.isDyeableItem(actualItem.getItemHolder()) && minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof WaterCauldronBlockEntity be) {
                if (be.waterColor == null && Legacy4J.isDyedItem(actualItem)) return LegacyComponents.CLEAR;
                else if (be.waterColor != null && !Legacy4J.isDyedItem(actualItem)) return LegacyComponents.DYE;
            }

            if (blockHit != null && actualItem.getItem() instanceof DyeItem && minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof WaterCauldronBlockEntity) return LegacyComponents.MIX;



            if (blockState != null && blockState.getBlock() instanceof AbstractCauldronBlock c && (actualItem.is(Items.WATER_BUCKET) || actualItem.is(Items.LAVA_BUCKET) || (actualItem.is(Items.POTION) || actualItem.is(Items.SPLASH_POTION) || actualItem.is(Items.LINGERING_POTION))  && (blockState.is(Blocks.CAULDRON) || blockState.is(Blocks.WATER_CAULDRON) && !c.isFull(blockState)) && Legacy4J.getPotionContent(actualItem) != null)) return LegacyComponents.FILL;
            if (blockState != null && blockState.getBlock() instanceof AbstractCauldronBlock c && (actualItem.is(Items.BUCKET) && c.isFull(blockState) || actualItem.is(Items.POTION) && Legacy4J.getPotionContent(actualItem) == null)) return LegacyComponents.COLLECT;
            if (canPlace(minecraft, actualItem, hand)) return actualItem.getItem() instanceof BlockItem b && isPlant(b.getBlock()) ? LegacyComponents.PLANT : LegacyComponents.PLACE;
            if (canHang(minecraft, blockHit, blockState, actualItem)) return LegacyComponents.HANG;
            if (blockState != null && blockState.getBlock() instanceof FlowerPotBlock pot && pot./*? if <1.20.2 {*//*getContent*//*?} else {*/getPotted/*?}*/() == Blocks.AIR && actualItem.getItem() instanceof BlockItem b && FlowerPotBlockAccessor.getPottedByContent().containsKey(b.getBlock())) return LegacyComponents.PLANT;
            if (canFeed(minecraft, entity, actualItem)) return LegacyComponents.FEED;
            if (canDyeEntity(minecraft, actualItem)) return LegacyComponents.DYE;
            if (actualItem.is(Items.IRON_INGOT) && entity instanceof IronGolem g  && g.getHealth() < g.getMaxHealth()) return LegacyComponents.REPAIR;
            if (entity instanceof MinecartFurnace && actualItem.is(ItemTags.COALS)) return LegacyComponents.USE;
            if (entity instanceof TamableAnimal a && a.isTame() && a.isFood(actualItem) && a.getHealth() < a.getMaxHealth()) return LegacyComponents.HEAL;
            if (entity instanceof TamableAnimal a && a.isTame() && a.isOwnedBy(minecraft.player)) return a.isInSittingPose() ? LegacyComponents.FOLLOW_ME : LegacyComponents.SIT;
            if (canTame(minecraft, hand, actualItem)) return LegacyComponents.TAME;
            if (canSetLoveMode(entity, actualItem)) return LegacyComponents.LOVE_MODE;
            if (blockHit != null && actualItem.getItem() instanceof BoneMealItem && blockState.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(minecraft.level,blockHit.getBlockPos(),blockState/*? if <=1.20.2 {*//*,true*//*?}*/)) return LegacyComponents.GROW;
            if (blockState != null && blockState.getBlock() instanceof ComposterBlock && blockState.getValue(ComposterBlock.LEVEL) < 7 && ComposterBlock.COMPOSTABLES.containsKey(actualItem.getItem())) return LegacyComponents.FILL;
            if (blockHit != null && !actualItem.isEmpty() && minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof CampfireBlockEntity e && /*? if <1.21.2 {*/e.getCookableRecipe(actualItem).isPresent()/*?} else {*//*minecraft.level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(actualItem)*//*?}*/) return LegacyComponents.COOK;
            if (blockState != null && actualItem.getItem() instanceof BrushItem && blockState.getBlock() instanceof BrushableBlock) return LegacyComponents.BRUSH;
            if (actualItem.getUseAnimation().equals(/*? if <1.21.2 {*/UseAnim/*?} else {*//*ItemUseAnimation*//*?}*/.BLOCK)) return LegacyComponents.BLOCK;
            if (/*? if <1.21.2 {*/actualItem.getItem() instanceof Equipable e/*?} else {*//*actualItem.has(DataComponents.EQUIPPABLE)*//*?}*/ && (!/*? if <1.20.5 {*//*(actualItem.getItem() instanceof HorseArmorItem)*//*?} else if <1.21.2 {*/e.getEquipmentSlot().equals(EquipmentSlot.BODY)/*?} else {*//*actualItem.get(DataComponents.EQUIPPABLE).slot().equals(EquipmentSlot.BODY) *//*?}*/ || minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Mob m && /*? if <1.20.5 {*//*m instanceof AbstractHorse h && h.isArmor(actualItem)*//*?} else if <1.21.2 {*/m.isBodyArmorItem(actualItem)/*?} else {*/ /*m.isEquippableInSlot(actualItem,EquipmentSlot.BODY)*//*?}*/)) return LegacyComponents.EQUIP;
            if (actualItem.getItem() instanceof EmptyMapItem || actualItem.getItem() instanceof FishingRodItem) return LegacyComponents.USE;
            if (actualItem.getItem() instanceof FireworkRocketItem && (minecraft.player.isFallFlying() || minecraft.hitResult instanceof BlockHitResult && minecraft.hitResult.getType() != HitResult.Type.MISS)) return LegacyComponents.LAUNCH;
            if (actualItem.getItem() instanceof ShearsItem){
                if (entity instanceof Sheep s && !s.isBaby() && !s.isSheared() || entity instanceof SnowGolem snowGolem && snowGolem.hasPumpkin() || entity instanceof MushroomCow) return LegacyComponents.SHEAR;
                else if (blockState != null && blockState.getBlock() instanceof PumpkinBlock) return LegacyComponents.CARVE;
            }
            if (actualItem.getItem() instanceof FoodOnAStickItem<?> i && minecraft.player.getControlledVehicle() instanceof ItemSteerable && minecraft.player.getControlledVehicle().getType() == i.canInteractWith) return LegacyComponents.BOOST;
            if (actualItem.getItem() instanceof LeadItem){
                if (entity instanceof Mob m && m.canBeLeashed(/*? if <1.20.5 {*//*minecraft.player*//*?}*/)) return LegacyComponents.LEASH;
                if (blockState != null && blockState.is(BlockTags.FENCES)) return LegacyComponents.ATTACH;
            }
            if (actualItem.getItem() instanceof NameTagItem && FactoryItemUtil.hasCustomName(actualItem) && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity e && !(e instanceof Player) && e.isAlive()) return LegacyComponents.NAME;
            if (actualItem.getItem() instanceof EggItem || actualItem.getItem() instanceof SnowballItem || actualItem.getItem() instanceof EnderpearlItem || actualItem.getItem() instanceof EnderEyeItem || actualItem.getItem() instanceof ThrowablePotionItem || actualItem.getItem() instanceof ExperienceBottleItem) return LegacyComponents.THROW;
            if (((actualItem.getItem() instanceof FlintAndSteelItem || actualItem.getItem() instanceof FireChargeItem) && minecraft.hitResult instanceof BlockHitResult r && blockState != null) && (BaseFireBlock.canBePlacedAt(minecraft.level, r.getBlockPos().relative(r.getDirection()), minecraft.player.getDirection()) || CampfireBlock.canLight(blockState) || CandleBlock.canLight(blockState) || CandleCakeBlock.canLight(blockState))) return LegacyComponents.IGNITE;

            if (actualItem.getItem() instanceof ProjectileWeaponItem && !minecraft.player.getProjectile(actualItem).isEmpty()){
                if (minecraft.player.getUseItem() == actualItem) return LegacyComponents.RELEASE;
                return LegacyComponents.DRAW;
            }
            if (actualItem.getItem() instanceof TridentItem){
                if (minecraft.player.getUseItem() == actualItem) return LegacyComponents.THROW;
                else if (EnchantmentHelper./*? if <1.20.5 {*//*getRiptide(actualItem)*//*?} else {*/getTridentSpinAttackStrength(actualItem, minecraft.player)/*?}*/ <= 0.0F || minecraft.player.isInWaterOrRain()) return LegacyComponents.CHARGE;
            }
            if (isBundle(actualItem) && BundleItem.getFullnessDisplay(actualItem) > 0) return LegacyComponents.RELEASE;

            BlockHitResult bucketHitResult;
            if (actualItem.getItem() instanceof BucketItem i && (bucketHitResult = mayInteractItemAt(minecraft,actualItem, Item.getPlayerPOVHitResult(minecraft.level, minecraft.player, ItemContainerPlatform.getBucketFluid(i) == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE))) != null){
                BlockState state = minecraft.level.getBlockState(bucketHitResult.getBlockPos());
                if (ItemContainerPlatform.getBucketFluid(i) != Fluids.EMPTY) {
                    BlockState resultState = state.getBlock() instanceof LiquidBlockContainer && ItemContainerPlatform.getBucketFluid(i) == Fluids.WATER ? state : minecraft.level.getBlockState(bucketHitResult.getBlockPos().relative(bucketHitResult.getDirection()));
                    if (resultState.canBeReplaced(ItemContainerPlatform.getBucketFluid(i)) || resultState.isAir() || resultState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(/*? if >=1.20.2 {*/minecraft.player, /*?}*/minecraft.level, bucketHitResult.getBlockPos(), state, ItemContainerPlatform.getBucketFluid(i))) return LegacyComponents.EMPTY;
                } else if (state.getBlock() instanceof BucketPickup) return LegacyComponents.COLLECT;
            }
            if (/*? if <1.21.5 {*/actualItem.getItem() instanceof SaddleItem && /*?}*/minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof /*? if <1.21.5 {*/Saddleable/*?} else {*//*Mob*//*?}*/ s &&/*? if <1.21.5 {*/s.isSaddleable()/*?} else {*//*s.isEquippableInSlot(actualItem, EquipmentSlot.SADDLE)*//*?}*/ && !s.isSaddled()) return LegacyComponents.SADDLE;
            if ((isEdible(actualItem) && minecraft.player.canEat(false)) || actualItem.getItem() instanceof PotionItem) return actualItem.getUseAnimation() == /*? if <1.21.2 {*/UseAnim/*?} else {*//*ItemUseAnimation*//*?}*/.DRINK ? LegacyComponents.DRINK : LegacyComponents.EAT;
            if (canTill(minecraft, hand, actualItem)) return LegacyComponents.TILL;
            if (actualItem.getItem() instanceof AxeItem && blockState != null && AxeItem.STRIPPABLES.get(blockState.getBlock()) != null && !(hand.equals(InteractionHand.MAIN_HAND) && minecraft.player.getOffhandItem().is(Items.SHIELD) && !minecraft.player.isSecondaryUseActive())) return LegacyComponents.PEEL_BARK;
            if (actualItem.getItem() instanceof ShovelItem && blockState != null && minecraft.level.getBlockState(blockHit.getBlockPos().above()).isAir() && ShovelItem.FLATTENABLES.get(blockState.getBlock()) != null) return LegacyComponents.DIG_PATH;
            if (actualItem.getItem() instanceof SpyglassItem) return LegacyComponents.ZOOM;
        }
        if (entity instanceof AbstractHorse h && h.isTamed() && minecraft.player.isSecondaryUseActive()) return LegacyComponents.OPEN;
        if (entity != null && entity.canAddPassenger(minecraft.player) && minecraft.player.canRide(entity)){
            if (entity instanceof Boat) return LegacyComponents.SAIL;
            else if (entity instanceof AbstractMinecart m && /*? if <1.21.2 {*/m.getMinecartType() == AbstractMinecart.Type.RIDEABLE/*?} else {*//*m.isRideable()*//*?}*/) return LegacyComponents.RIDE;
            else if (entity instanceof /*? if <1.21.5 {*/Saddleable/*?} else {*//*Mob*//*?}*/ s && !entity.isVehicle() && ((!(entity instanceof AbstractHorse) && s.isSaddled()) || entity instanceof AbstractHorse h && !minecraft.player.isSecondaryUseActive() && (h.isTamed() && !h.isFood(minecraft.player.getMainHandItem()) || minecraft.player.getMainHandItem().isEmpty()))) return LegacyComponents.MOUNT;
        }
        return null;
    }

    static BlockHitResult mayInteractItemAt(Minecraft minecraft, ItemStack usedItem, HitResult result){
        if (result instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.mayInteract(minecraft.player, r.getBlockPos()) && minecraft.player.mayUseItemAt(r.getBlockPos().relative(r.getDirection()), r.getDirection(), usedItem)){
            return r;
        }
        return null;
    }

    static boolean canFeed(Minecraft minecraft, Entity entity, ItemStack usedItem){
        return (entity instanceof Animal a && a.isFood(usedItem) && (!(a instanceof AbstractHorse) && a.isBaby() || a instanceof AbstractHorse h && (a instanceof Llama || (a.isBaby() || !usedItem.is(Items.HAY_BLOCK))) && (!h.isTamed() || !isLoveFood(a,usedItem) && a.getHealth() < a.getMaxHealth() && !minecraft.player.isSecondaryUseActive())));
    }

    static boolean canSetLoveMode(Entity entity, ItemStack usedItem){
        return (entity instanceof Animal a && !a.isBaby() && a.isFood(usedItem) && a.canFallInLove() && !a.isInLove() && (!(a instanceof AbstractHorse) || isLoveFood(a,usedItem)));
    }

    static boolean isLoveFood(Animal a, ItemStack stack){
        return (a instanceof Llama && stack.is(Items.HAY_BLOCK)) || a instanceof Horse && ((stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)));
    }

    static boolean canPlace(Minecraft minecraft, ItemStack usedItem, InteractionHand hand){
        BlockPlaceContext c;
        return minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !usedItem.isEmpty() && ((usedItem.getItem() instanceof SpawnEggItem e && (!(minecraft.hitResult instanceof EntityHitResult r) || r.getEntity().getType() == e.getType(/*? if >=1.21.4 {*//*minecraft.level.registryAccess(), *//*?}*/usedItem/*? if <1.20.5 {*//*.getTag()*//*?}*/))) || minecraft.hitResult instanceof BlockHitResult r && (usedItem.getItem() instanceof BlockItem b && (c =new BlockPlaceContext(minecraft.player,hand,usedItem,r)).canPlace() && b.getPlacementState(c) != null));
    }

    static boolean canHang(Minecraft minecraft, BlockHitResult hitResult, BlockState blockState, ItemStack usedItem){
        if (!(hitResult != null && usedItem.getItem() instanceof HangingEntityItemAccessor hanging && (Block.canSupportCenter(minecraft.level, hitResult.getBlockPos(), hitResult.getDirection()) || blockState.isSolid() || DiodeBlock.isDiode(blockState)))) return false;

        if (hanging.getType() == EntityType.PAINTING) {
            return Direction.Plane.HORIZONTAL.test(hitResult.getDirection());
        } else return hanging.getType() == EntityType.ITEM_FRAME || hanging.getType() == EntityType.GLOW_ITEM_FRAME;
    }

    static boolean canTill(Minecraft minecraft, InteractionHand hand, ItemStack usedItem){
        if (!(usedItem.getItem() instanceof HoeItem && minecraft.hitResult instanceof BlockHitResult r)) return false;
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> use = HoeItem.TILLABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock());
        return use != null && use.getFirst().test(new UseOnContext(minecraft.player,hand,r));
    }

    static boolean canTame(Minecraft minecraft, InteractionHand hand, ItemStack usedItem){
        return minecraft.hitResult instanceof EntityHitResult e && ((e.getEntity() instanceof TamableAnimal t && !t.isTame() && ((t instanceof Wolf && usedItem.is(Items.BONE)) || (!(t instanceof Wolf) && t.isFood(usedItem)))) || (hand == InteractionHand.MAIN_HAND && e.getEntity() instanceof AbstractHorse h && !h.isTamed() && !minecraft.player.isSecondaryUseActive() && usedItem.isEmpty()));
    }

    static boolean isPlant(Block block){
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof GrowingPlantBlock || block instanceof BambooStalkBlock || block instanceof CactusBlock;
    }

    static boolean isEdible(ItemStack stack){
        return /*? if <1.20.5 {*//*stack.isEdible()*//*?} else {*/stack.has(DataComponents.FOOD)/*?}*/;
    }

    static boolean canDyeEntity(Minecraft minecraft, ItemStack usedItem){
        return usedItem.getItem() instanceof DyeItem && minecraft.hitResult instanceof EntityHitResult result && (result.getEntity() instanceof Wolf w && w.isTame() || result.getEntity() instanceof Sheep || result.getEntity() instanceof Cat c && c.isTame());
    }

    interface ActionHolder {
        interface Context {
            default <C extends Context> Component actionOfContext(Class<C> contextClass, Function<C,Component> toAction){
                if (contextClass.isInstance(this)){
                    return toAction.apply(contextClass.cast(this));
                }return null;
            }
        }
        interface ScreenContext extends Context {
            Screen screen();
        }
        record KeyContext(int key, Screen screen) implements ScreenContext {

        }

        @Nullable
        Component getAction(Context context);
        @Nullable
        default Component getAction(Screen screen){
            return getAction((ScreenContext)(()-> screen));
        }
    }

}
