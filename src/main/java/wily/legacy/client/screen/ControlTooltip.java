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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.RecipePropertySet;
//?}
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.inventory.RenameItemMenu;
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

    BiFunction<String,Style,Component> CONTROL_ICON_FUNCTION = Util.memoize((s,style)-> Component.literal(s).withStyle(style));
    Function<Icon[], Icon> COMPOUND_ICON_FUNCTION = Util.memoize(Icon::createCompound);
    static Component getControlIcon(String s, ControlType type){
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
    Component MORE = Component.literal("...").withStyle(ChatFormatting.GRAY);
    Component SPACE = Component.literal("  ");
    Component PLUS = Component.literal("+");
    Icon SPACE_ICON = Icon.of(SPACE);
    Icon PLUS_ICON = Icon.of(PLUS);

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
        return renderer.add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_NUMPADENTER) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), ()-> getKeyMessage(InputConstants.KEY_NUMPADENTER, screen)).add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()-> getKeyMessage(InputConstants.KEY_RETURN,screen));
    }
    static ControlTooltip.Renderer setupDefaultScreen(Renderer renderer, Screen screen){
        return setupDefaultButtons(renderer,screen).add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()-> getKeyMessage(InputConstants.KEY_RETURN,screen)).add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(),()->screen.shouldCloseOnEsc() ? CommonComponents.GUI_BACK : null);
    }
    static ControlTooltip.Renderer setupDefaultContainerScreen(Renderer renderer, LegacyMenuAccess<?> a){
        return renderer.
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()->a.isOutsideClick(0) && !a.getMenu().getCarried().isEmpty() ? a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ALL : LegacyComponents.DROP : a.getHoveredSlot() != null && (a.getHoveredSlot().hasItem() || !a.getMenu().getCarried().isEmpty()) ? a.getHoveredSlot().hasItem() && !FactoryItemUtil.equalItems(a.getHoveredSlot().getItem(),a.getMenu().getCarried()) && !isBundleAndAcceptItem(a.getHoveredSlot().getItem(),a.getMenu().getCarried())  ? a.getMenu().getCarried().isEmpty() ? LegacyComponents.TAKE : isBundleAndAcceptItem(a.getMenu().getCarried(),a.getHoveredSlot().getItem()) ? LegacyComponents.PICK_UP : LegacyComponents.SWAP : LegacyComponents.PLACE : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(),()-> LegacyComponents.EXIT).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_RIGHT) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()->getMouseRightAction(a)).
                add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT), PLUS_ICON,getKeyIcon(InputConstants.KEY_LSHIFT)}) : ControllerBinding.UP_BUTTON.bindingState.getIcon(),()-> a.getHoveredSlot() != null && a.getHoveredSlot().hasItem() ? LegacyComponents.QUICK_MOVE : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_W) : ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon(),()->a.getHoveredSlot() != null && a.getHoveredSlot().hasItem() ? LegacyComponents.WHATS_THIS : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT) : ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(),()-> a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DISTRIBUTE : null);
    }

    static Component getMouseRightAction(LegacyMenuAccess<?> a){
        if (a.isOutsideClick(1) && !a.getMenu().getCarried().isEmpty()) return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.DROP_ONE : LegacyComponents.DROP;
        if (a.getHoveredSlot() != null){
            if (a.getMenu().getCarried().isEmpty()) {
                if (isBundle(a.getHoveredSlot().getItem()) && BundleItem.getFullnessDisplay(a.getHoveredSlot().getItem()) > 0) return LegacyComponents.PICK_UP;
                else if (a.getHoveredSlot().getItem().getCount() > 1) return LegacyComponents.TAKE_HALF;
            } else {
                if (a.getHoveredSlot().hasItem() && Legacy4J.canRepair(a.getHoveredSlot().getItem(),a.getMenu().getCarried())) return LegacyComponents.REPAIR;
                else if (isBundle(a.getMenu().getCarried()) && BundleItem.getFullnessDisplay(a.getMenu().getCarried()) > 0 && !a.getHoveredSlot().hasItem()) return LegacyComponents.RELEASE;
                else if (a.getHoveredSlot().hasItem() && !a.getMenu().getCarried().isEmpty() && !FactoryItemUtil.equalItems(a.getMenu().getCarried(),a.getHoveredSlot().getItem())) return LegacyComponents.SWAP;
                else if (!a.getHoveredSlot().hasItem()) return a.getMenu().getCarried().getCount() > 1 ? LegacyComponents.PLACE_ONE : LegacyComponents.PLACE;
            }
        }
        return null;
    }

    static boolean isBundle(ItemStack stack){
        return /*? if <1.21.2 {*//*stack.getItem() instanceof BundleItem*//*?} else {*/stack.is(ItemTags.BUNDLES)/*?}*/;
    }
    static boolean isBundleAndAcceptItem(ItemStack stack, ItemStack itemToAccept){
        return isBundle(stack) && BundleItem.getFullnessDisplay(stack) < 1 && !itemToAccept.isEmpty() && itemToAccept.getItem().canFitInsideContainerItems();
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
        return create(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(mapping.getKey().getValue()) : mapping.getBinding() == null ? null : mapping.getBinding().bindingState.getIcon(),action);
    }

    interface Icon {
        int render(GuiGraphics graphics, int x, int y, boolean allowPressed, boolean simulate);

        static Icon of(Component component){
            return (graphics, x, y, allowPressed, simulate) -> {
                Font font = Minecraft.getInstance().font;
                if (!simulate) graphics.drawString(font,component,x,y,0xFFFFFF,false);
                return font.width(component);
            };
        }

        static Icon createCompound(Icon[] icons){
            return (graphics, x, y, allowPressed, simulate) -> {
                int totalWidth = 0;
                for (Icon icon : icons) totalWidth+= icon.render(graphics, x + totalWidth, y, allowPressed, simulate);
                return totalWidth;
            };
        }
    }

    abstract class ComponentIcon implements Icon {
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
            return chars == null ? null : ControlTooltip.getControlIcon(String.valueOf(chars[chars.length > 1 && allowPressed && startPressTime != 0 && (canLoop() || getPressInterval() <= 1) ? 1 + Math.round(((getPressInterval() / 2) <= 1.4f ? (getPressInterval() / 2f) % 1f : 0.4f) * (chars.length - 2)) : 0]),type);
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
        public static ComponentIcon create(InputConstants.Key key, char[] iconChars, char[] iconOverlayChars, Character tipIcon){
            return create(key, (k,b)-> create(b,iconChars,iconOverlayChars,tipIcon,()-> k.getType() != InputConstants.Type.MOUSE, ControlType::getKbmActiveType));
        }
        public static ComponentIcon create(BooleanSupplier pressed, char[] iconChars, char[] iconOverlayChars, Character tipIcon, BooleanSupplier loop, Supplier<ControlType> type){
            return new ControlTooltip.ComponentIcon() {

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
                    return tipIcon == null ? super.getComponent() == null ? getOverlayComponent(false) : super.getComponent() : getControlIcon(String.valueOf(tipIcon), ControlType.getActiveControllerType());
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

        public static ComponentIcon create(InputConstants.Key key, BiFunction<InputConstants.Key,BooleanSupplier,ComponentIcon> iconGetter){
            long window = Minecraft.getInstance().getWindow().getWindow();
            return iconGetter.apply(key,()->(key.getType() == InputConstants.Type.KEYSYM ? InputConstants.isKeyDown(window, key.getValue()) : GLFW.glfwGetMouseButton(window, key.getValue()) == 1));
        }
    }


    class Renderer implements Renderable{
        public static final FactoryEvent<BiConsumer<Screen,ControlTooltip.Renderer>> SCREEN_EVENT = new FactoryEvent<>(e-> (screen, event)-> e.invokeAll(c->c.accept(screen,event)));
        public static final FactoryEvent<BiConsumer<Gui,ControlTooltip.Renderer>> GUI_EVENT = new FactoryEvent<>(e-> (screen, event)-> e.invokeAll(c->c.accept(screen,event)));

        static final Renderer INSTANCE = new Renderer();
        public static Renderer getInstance(){
            return INSTANCE;
        }
        private final Minecraft minecraft = Minecraft.getInstance();

        public static Renderer of(Object o){
            return o instanceof Event e ? e.getControlTooltips() : getInstance();
        }

        public final List<ControlTooltip> tooltips = new ArrayList<>();
        protected final Map<Component,Icon> renderTooltips = new Object2ReferenceLinkedOpenHashMap<>();
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
        public Renderer add(LegacyKeyMapping mapping){
            return add(mapping,mapping::getDisplayName);
        }
        public Renderer add(LegacyKeyMapping mapping, Supplier<Component> action){
            return add(create(mapping,action));
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
            if (!LegacyOption.inGameTooltips.get() && minecraft.screen == null) return;
            renderTooltips.clear();
            for (ControlTooltip tooltip : tooltips) {
                Component action;
                Icon icon;
                if ((action = tooltip.getAction()) == null || (icon = tooltip.getIcon()) == null) continue;
                renderTooltips.compute(action, (k, existingIcon) -> existingIcon == null ? icon : existingIcon.equals(icon) || !LegacyOption.displayMultipleControlsFromAction.get() ? existingIcon : COMPOUND_ICON_FUNCTION.apply(new Icon[]{existingIcon,SPACE_ICON, icon}));
            }
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f,Math.max(minecraft.screen == null ? 0.0f : 0.2f,  ScreenUtil.getHUDOpacity()));
            guiGraphics.pose().pushPose();
            double hudDiff = (1 - LegacyOption.hudDistance.get()) * 60D;
            guiGraphics.pose().translate(-Math.min(hudDiff,30), Math.min(hudDiff,16),0);
            int baseHeight = guiGraphics.guiHeight() - 29;

            renderTooltips.forEach((action,icon)->{
                int controlWidth = icon.render(guiGraphics, 32, baseHeight, allowPressed(), false);
                if (controlWidth > 0) {
                    guiGraphics.drawString(minecraft.font, action, 34 + controlWidth, baseHeight, CommonColor.ACTION_TEXT.get());
                    guiGraphics.pose().translate(controlWidth + minecraft.font.width(action) + 12, 0, 0);
                    guiGraphics.flush();
                }
            });
            guiGraphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }
    String MOUSE_BASE_CHAR = "\uC002";
    String MOUSE_BASE_FOCUSED_CHAR = "\uC003";
    String KEY_CHAR = "\uC000";
    String KEY_PRESSED_CHAR = "\uC001";


    static ComponentIcon getKeyIcon(int i){
        InputConstants.Type type = i >= 0 ? i <= 9 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM : null;
        if (type == null) return null;
        InputConstants.Key key = type.getOrCreate(i);
        return ControlType.getKbmActiveType().getIcons().computeIfAbsent(key.getName(), i2-> ComponentIcon.create(key,(k, b)->new ComponentIcon() {
            @Override
            public Component getComponent(boolean allowPressed) {
                return getControlIcon(k.getType() == InputConstants.Type.MOUSE ? b.getAsBoolean() && allowPressed ? MOUSE_BASE_FOCUSED_CHAR : MOUSE_BASE_CHAR : b.getAsBoolean() && allowPressed ? KEY_PRESSED_CHAR : KEY_CHAR,ControlType.getKbmActiveType());
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
            renderer.add(minecraft.options.keyJump,()-> minecraft.player.isUnderWater() ? LegacyComponents.SWIM_UP : null).add(Minecraft.getInstance().options.keyInventory).add(Legacy4JClient.keyCrafting).add(Minecraft.getInstance().options.keyUse,()-> getActualUse(minecraft)).add(Minecraft.getInstance().options.keyAttack,()->getMainAction(minecraft));
            renderer.tooltips.addAll(controlTooltips);
            renderer.add(minecraft.options.keyShift,()-> minecraft.player.isPassenger() ? minecraft.player.getVehicle() instanceof LivingEntity ? LegacyComponents.DISMOUNT : LegacyComponents.EXIT : null).add(minecraft.options.keyPickItem,()-> getPickAction(minecraft));
        }
        protected ControlTooltip guiControlTooltipFromJson(JsonObject o){
            LegacyKeyMapping mapping = LegacyKeyMapping.of(KeyMapping.ALL.get(GsonHelper.getAsString(o, "keyMapping")));
            BiPredicate<Item, /*? if <1.20.5 {*//*CompoundTag*//*?} else {*/DataComponentPatch/*?}*/> itemPredicate = o.has("heldItem") ? o.get("heldItem") instanceof JsonObject obj ? JsonUtil.registryMatchesItem(obj) : o.get("heldItem").getAsBoolean() ? (i, t)-> i != null && i != Items.AIR : (i, t)-> false : (i, t)-> true;
            Predicate<Block> blockPredicate = o.has("hitBlock") ? o.get("hitBlock") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.BLOCK,obj) : o.get("hitBlock").getAsBoolean() ? b-> !b.defaultBlockState().isAir() : b-> false : b-> true;
            Predicate<EntityType<?>> entityPredicate = o.has("hitEntity") ? o.get("hitEntity") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE,obj) : staticPredicate(o.get("hitEntity").getAsBoolean()) : e-> true;
            Minecraft minecraft = Minecraft.getInstance();
            Component c =o.get("action") instanceof JsonPrimitive p ? Component.translatable(p.getAsString()) : mapping.getDisplayName();
            return create(mapping, ()->minecraft.player != null && itemPredicate.test(minecraft.player.getMainHandItem().getItem(),minecraft.player.getMainHandItem()./*? if <1.20.5 {*//*getTag*//*?} else {*/getComponentsPatch/*?}*/()) && ((minecraft.hitResult instanceof BlockHitResult r && blockPredicate.test(minecraft.level.getBlockState(r.getBlockPos()).getBlock())) || (minecraft.hitResult instanceof EntityHitResult er && entityPredicate.test(er.getEntity().getType()))) ? c : null);
        }
        public static <T> Predicate<T> staticPredicate(boolean b){return o-> b;}

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
        if ((minecraft.hitResult instanceof EntityHitResult r &&  (result = r.getEntity().getPickResult()) != null || minecraft.hitResult instanceof BlockHitResult h && h.getType() != HitResult.Type.MISS && !(result = (b = minecraft.level.getBlockState(h.getBlockPos()))/*? if <1.21.4 {*//*.getBlock()*//*?}*/.getCloneItemStack(minecraft.level,h.getBlockPos(),/*? if >=1.21.4 {*/true/*?} else {*//*b*//*?}*/)).isEmpty()) && (minecraft.gameMode.hasInfiniteItems() || minecraft.player.getInventory().findSlotMatchingItem(result) != -1))
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
        BlockState blockState;
        if (minecraft.player.isSleeping()) return LegacyComponents.WAKE_UP;
        if ((minecraft.hitResult instanceof BlockHitResult r && ((blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof ButtonBlock || blockState.getBlock() instanceof LeverBlock || blockState.getBlock() instanceof DoorBlock || blockState.getBlock() instanceof FenceGateBlock || (blockState.getMenuProvider(minecraft.level,r.getBlockPos()) != null || minecraft.level.getBlockEntity(r.getBlockPos()) instanceof MenuProvider)))) return (blockState.getBlock() instanceof AbstractChestBlock<?> || blockState.getBlock() instanceof ShulkerBoxBlock || blockState.getBlock() instanceof BarrelBlock || blockState.getBlock() instanceof HopperBlock || blockState.getBlock() instanceof DropperBlock) ? LegacyComponents.OPEN :  LegacyComponents.USE;
        if (minecraft.hitResult instanceof EntityHitResult r && (r.getEntity() instanceof AbstractVillager m && (!(m instanceof Villager v) || v.getVillagerData().getProfession() != VillagerProfession.NONE) && !m.isTrading())) return LegacyComponents.TRADE;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack actualItem = minecraft.player.getItemInHand(hand);
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof BedBlock) return LegacyComponents.SLEEP;
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof NoteBlock) return LegacyComponents.CHANGE_PITCH;
            if (canPlace(minecraft, hand)) return actualItem.getItem() instanceof BlockItem b && isPlant(b.getBlock()) ? LegacyComponents.PLANT : LegacyComponents.PLACE;
            if (canFeed(minecraft, hand)) return LegacyComponents.FEED;
            if (actualItem.is(Items.IRON_INGOT) && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof IronGolem g  && g.getHealth() < g.getMaxHealth()) return LegacyComponents.REPAIR;
            if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof TamableAnimal a && a.isTame() && a.isFood(actualItem) && a.getHealth() < a.getMaxHealth()) return LegacyComponents.HEAL;
            if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof TamableAnimal a && a.isTame() && a.isOwnedBy(minecraft.player)) return a.isInSittingPose() ? LegacyComponents.FOLLOW_ME : LegacyComponents.SIT;
            if (canTame(minecraft, hand)) return LegacyComponents.TAME;
            if (canSetLoveMode(minecraft,hand)) return LegacyComponents.LOVE_MODE;
            if (minecraft.hitResult instanceof BlockHitResult r && actualItem.getItem() instanceof BoneMealItem && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(minecraft.level,r.getBlockPos(),blockState/*? if <=1.20.2 {*//*,true*//*?}*/)) return LegacyComponents.GROW;
            if (minecraft.hitResult instanceof BlockHitResult r && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof ComposterBlock){
                if (blockState.getValue(ComposterBlock.LEVEL) == 8) return LegacyComponents.COLLECT;
                else if (blockState.getValue(ComposterBlock.LEVEL) < 7 && ComposterBlock.COMPOSTABLES.containsKey(actualItem.getItem())) return LegacyComponents.FILL;
            }
            if (minecraft.hitResult instanceof BlockHitResult r && !actualItem.isEmpty() && minecraft.level.getBlockEntity(r.getBlockPos()) instanceof CampfireBlockEntity e && /*? if <1.21.2 {*//*e.getCookableRecipe(actualItem).isPresent()*//*?} else {*/minecraft.level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(actualItem)/*?}*/) return LegacyComponents.COOK;
            if (actualItem.getItem() instanceof BrushItem && minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof BrushableBlock) return LegacyComponents.BRUSH;
            if (/*? if <1.21.2 {*//*actualItem.getItem() instanceof Equipable e*//*?} else {*/actualItem.has(DataComponents.EQUIPPABLE)/*?}*/ && (/*? if <1.20.5 {*//*actualItem.getItem() instanceof HorseArmorItem*//*?} else if <1.21.2 {*//*!e.getEquipmentSlot().equals(EquipmentSlot.BODY)*//*?} else {*/!actualItem.get(DataComponents.EQUIPPABLE).slot().equals(EquipmentSlot.BODY) /*?}*/ || minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Mob m && /*? if <1.20.5 {*//*m instanceof AbstractHorse h && h.isArmor(actualItem)*//*?} else if <1.21.2 {*//*m.isBodyArmorItem(actualItem)*//*?} else {*/ m.isEquippableInSlot(actualItem,EquipmentSlot.BODY)/*?}*/)) return actualItem.getItem() instanceof ShieldItem ? LegacyComponents.BLOCK : LegacyComponents.EQUIP;
            if (actualItem.getItem() instanceof EmptyMapItem || actualItem.getItem() instanceof FishingRodItem) return LegacyComponents.USE;
            if (actualItem.getItem() instanceof FireworkRocketItem && (minecraft.player.isFallFlying() || minecraft.hitResult instanceof BlockHitResult && minecraft.hitResult.getType() != HitResult.Type.MISS)) return LegacyComponents.LAUNCH;
            if (actualItem.getItem() instanceof ShearsItem ){
                if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Sheep s && !s.isBaby() && !s.isSheared()) return LegacyComponents.SHEAR;
                else if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof PumpkinBlock) return LegacyComponents.CARVE;
            }
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof JukeboxBlock && blockState.getValue(HAS_RECORD)) return LegacyComponents.EJECT;
            if (actualItem.getItem() instanceof FoodOnAStickItem<?> i && minecraft.player.getControlledVehicle() instanceof ItemSteerable && minecraft.player.getControlledVehicle().getType() == i.canInteractWith) return LegacyComponents.BOOST;
            if (actualItem.getItem() instanceof LeadItem){
                if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Mob m && m.canBeLeashed(/*? if <1.20.5 {*//*minecraft.player*//*?}*/)) return LegacyComponents.LEASH;
                if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).is(BlockTags.FENCES)) return LegacyComponents.ATTACH;
            }
            if (actualItem.getItem() instanceof NameTagItem && RenameItemMenu.hasCustomName(actualItem) && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity e && !(e instanceof Player) && e.isAlive()) return LegacyComponents.NAME;
            if (actualItem.getItem() instanceof EggItem || actualItem.getItem() instanceof SnowballItem || actualItem.getItem() instanceof EnderpearlItem || actualItem.getItem() instanceof EnderEyeItem || actualItem.getItem() instanceof ThrowablePotionItem || actualItem.getItem() instanceof ExperienceBottleItem) return LegacyComponents.THROW;
            if (((actualItem.getItem() instanceof FlintAndSteelItem || actualItem.getItem() instanceof FireChargeItem) && minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS)&& (BaseFireBlock.canBePlacedAt(minecraft.level, r.getBlockPos().relative(r.getDirection()), minecraft.player.getDirection()) || CampfireBlock.canLight(blockState = minecraft.level.getBlockState(r.getBlockPos())) || CandleBlock.canLight(blockState) || CandleCakeBlock.canLight(blockState))) return LegacyComponents.IGNITE;

            if (actualItem.getItem() instanceof ProjectileWeaponItem && !minecraft.player.getProjectile(actualItem).isEmpty()){
                if (minecraft.player.getUseItem() == actualItem) return LegacyComponents.RELEASE;
                return LegacyComponents.DRAW;
            }
            if (actualItem.getItem() instanceof TridentItem){
                if (minecraft.player.getUseItem() == actualItem) return LegacyComponents.THROW;
                return LegacyComponents.CHARGE;
            }
            if (isBundle(actualItem) && BundleItem.getFullnessDisplay(actualItem) > 0) return LegacyComponents.RELEASE;

            HitResult bucketHitResult;
            if (actualItem.getItem() instanceof BucketItem i && mayInteractItemAt(minecraft,actualItem,bucketHitResult = (i.equals(Items.BUCKET) ? Item.getPlayerPOVHitResult(minecraft.level, minecraft.player,ClipContext.Fluid.SOURCE_ONLY) : minecraft.hitResult))){
                BlockState state;
                if (!i.equals(Items.BUCKET)) return (state = minecraft.level.getBlockState(((BlockHitResult)bucketHitResult).getBlockPos())).getBlock() instanceof LiquidBlockContainer || state.getBlock() instanceof AbstractCauldronBlock && CauldronInteraction.EMPTY/*? if >1.20.1 {*/.map()/*?}*/.containsKey(i) ? LegacyComponents.FILL: LegacyComponents.EMPTY;
                else if (minecraft.level.getBlockState(((BlockHitResult)bucketHitResult).getBlockPos()).getBlock() instanceof BucketPickup) return LegacyComponents.COLLECT;
            }
            if (minecraft.player.getItemInHand(hand).getItem() instanceof SaddleItem && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Saddleable s && s.isSaddleable() && !s.isSaddled()) return LegacyComponents.SADDLE;
            if ((isEdible(actualItem) && minecraft.player.canEat(false)) || actualItem.getItem() instanceof PotionItem) return actualItem.getUseAnimation() == /*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.DRINK ? LegacyComponents.DRINK : LegacyComponents.EAT;
            if (canTill(minecraft, hand)) return LegacyComponents.TILL;
            if (minecraft.player.getItemInHand(hand).getItem() instanceof AxeItem && minecraft.hitResult instanceof BlockHitResult r && AxeItem.STRIPPABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock()) != null) return LegacyComponents.PEEL_BARK;
            if (minecraft.player.getItemInHand(hand).getItem() instanceof ShovelItem && minecraft.hitResult instanceof BlockHitResult r && ShovelItem.FLATTENABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock()) != null) return LegacyComponents.DIG_PATH;
        }
        if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof AbstractHorse h && h.isTamed() && minecraft.player.isSecondaryUseActive()) return LegacyComponents.OPEN;
        if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity().canAddPassenger(minecraft.player) && minecraft.player.canRide(r.getEntity())){
            if (r.getEntity() instanceof Boat) return LegacyComponents.SAIL;
            else if (r.getEntity() instanceof AbstractMinecart m && /*? if <1.21.2 {*//*m.getMinecartType() == AbstractMinecart.Type.RIDEABLE*//*?} else {*/m.isRideable()/*?}*/) return LegacyComponents.RIDE;
            else if (r.getEntity() instanceof Saddleable s && !r.getEntity().isVehicle() && ((!(r.getEntity() instanceof AbstractHorse) && s.isSaddled()) || r.getEntity() instanceof AbstractHorse h && !minecraft.player.isSecondaryUseActive() && (h.isTamed() && !h.isFood(minecraft.player.getMainHandItem()) || minecraft.player.getMainHandItem().isEmpty()))) return LegacyComponents.MOUNT;
        }
        return null;
    }
    static boolean mayInteractItemAt(Minecraft minecraft, ItemStack usedItem, HitResult result){
        return result instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.mayInteract(minecraft.player, r.getBlockPos()) && minecraft.player.mayUseItemAt(r.getBlockPos(), r.getDirection(), usedItem);
    }
    static boolean canFeed(Minecraft minecraft, InteractionHand hand){
        ItemStack usedItem = minecraft.player.getItemInHand(hand);
        return (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Animal a && a.isFood(usedItem) && (!(a instanceof AbstractHorse) && a.isBaby() || a instanceof AbstractHorse h && (a instanceof Llama || (a.isBaby() || !usedItem.is(Items.HAY_BLOCK))) && (!h.isTamed() || !isLoveFood(a,usedItem) && a.getHealth() < a.getMaxHealth() && !minecraft.player.isSecondaryUseActive())));
    }
    static boolean canSetLoveMode(Minecraft minecraft, InteractionHand hand){
        ItemStack usedItem = minecraft.player.getItemInHand(hand);
        return (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Animal a && !a.isBaby() && a.isFood(minecraft.player.getItemInHand(hand)) && a.canFallInLove() && !a.isInLove() && (!(a instanceof AbstractHorse) || isLoveFood(a,usedItem)));
    }
    static boolean isLoveFood(Animal a, ItemStack stack){
        return (a instanceof Llama && stack.is(Items.HAY_BLOCK)) || a instanceof Horse && ((stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)));
    }
    static boolean canPlace(Minecraft minecraft, InteractionHand hand){
        ItemStack usedItem = minecraft.player.getItemInHand(hand);
        BlockPlaceContext c;
        return minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !usedItem.isEmpty() && ((usedItem.getItem() instanceof SpawnEggItem e && (!(minecraft.hitResult instanceof EntityHitResult r) || r.getEntity().getType() == e.getType(/*? if >=1.21.4 {*/minecraft.level.registryAccess(), /*?}*/usedItem/*? if <1.20.5 {*//*.getTag()*//*?}*/))) || minecraft.hitResult instanceof BlockHitResult r && (usedItem.getItem() instanceof BlockItem b && (c =new BlockPlaceContext(minecraft.player,hand,usedItem,r)).canPlace() && b.getPlacementState(c) != null));
    }
    static boolean canTill(Minecraft minecraft, InteractionHand hand){
        if (!(minecraft.player.getItemInHand(hand).getItem() instanceof HoeItem && minecraft.hitResult instanceof BlockHitResult r)) return false;
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> use = HoeItem.TILLABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock());
        return use != null && use.getFirst().test(new UseOnContext(minecraft.player,hand,r));
    }
    static boolean canTame(Minecraft minecraft, InteractionHand hand){
        ItemStack usedItem = minecraft.player.getItemInHand(hand);
        return minecraft.hitResult instanceof EntityHitResult e && ((e.getEntity() instanceof TamableAnimal t && !t.isTame() && ((t instanceof Wolf && usedItem.is(Items.BONE)) || (!(t instanceof Wolf) && t.isFood(usedItem)))) || (hand == InteractionHand.MAIN_HAND && e.getEntity() instanceof AbstractHorse h && !h.isTamed() && !minecraft.player.isSecondaryUseActive() && usedItem.isEmpty()));
    }
    static boolean isPlant(Block block){
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof GrowingPlantBlock || block instanceof BambooStalkBlock;
    }

    static boolean isEdible(ItemStack stack){
        return /*? if <1.20.5 {*//*stack.isEdible()*//*?} else {*/stack.has(DataComponents.FOOD)/*?}*/;
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
