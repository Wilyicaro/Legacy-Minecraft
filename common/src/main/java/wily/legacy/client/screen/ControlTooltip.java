package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import dev.architectury.hooks.fluid.FluidBucketHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.*;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;

public interface ControlTooltip {
    Renderer guiControlRenderer = new Renderer(null);
    String CONTROL_TOOLTIPS = "control_tooltips";
    BiFunction<String,Style,Component> CONTROL_ICON_FUNCTION = Util.memoize((s,style)-> Component.literal(s).withStyle(style));
    Function<Component[], Component> COMPOUND_COMPONENT_FUNCTION = Util.memoize((components)-> {
        if (components == null) return null;
        MutableComponent component = Component.empty();
        for (Component c : components)
            component.append(c);
        return component;
    });
    static Component getControlIcon(String s, Type type){
        return CONTROL_ICON_FUNCTION.apply(s,type.style);
    }

    LoadingCache<String, MutableComponent> CONTROL_ACTION_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public MutableComponent load(String key) {
            return Component.translatable(key);
        }
    });
    enum Type{
        KEYBOARD,
        x360,
        xONE,
        PS3,
        PS4,
        WII_U,
        SWITCH,
        STEAM;
        public final ResourceLocation font = new ResourceLocation(Legacy4J.MOD_ID, name().toLowerCase(Locale.ENGLISH) + "_components");
        public final Component displayName = Component.translatable("legacy.controls.controller."+name().toLowerCase(Locale.ENGLISH));
        public final Style style = Style.EMPTY.withFont(font);
        public boolean isKeyboard(){
            return this == KEYBOARD;
        }
    }
    static Type getActiveControllerType(){
        if (ScreenUtil.getLegacyOptions().controllerIcons().get() <= 0) {
            if (Legacy4JClient.controllerManager.connectedController != null){
                return Legacy4JClient.controllerManager.connectedController.getType();
            } else return Type.x360;
        } else return Type.values()[ScreenUtil.getLegacyOptions().controllerIcons().get()];
    }
    static Type getActiveType(){
        if (Legacy4JClient.controllerManager.connectedController != null) return getActiveControllerType();
        return Type.KEYBOARD;
    }
    Component getIcon();
    @Nullable
    Component getAction();
    Component MORE = Component.literal("...").withStyle(ChatFormatting.DARK_GRAY);
    Component SPACE = Component.literal("  ");
    Component PLUS = Component.literal("+");
    static ControlTooltip.Renderer defaultScreen(Screen screen){
        return new Renderer(screen).add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->screen.getFocused() == null ? null : CONTROL_ACTION_CACHE.getUnchecked("mco.template.button.select")).add(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_ESCAPE,true) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(true),()->CONTROL_ACTION_CACHE.getUnchecked("gui.back"));
    }
    static ControlTooltip create(Supplier<Component> icon, Supplier<Component> action) {
        return new ControlTooltip() {
            public Component getIcon() {
                return icon.get();
            }
            public Component getAction() {
                return action.get();
            }
        };
    }
    static ControlTooltip create(LegacyKeyMapping mapping,Supplier<Component> action, boolean allowPressed){
        return create(()-> getActiveType().isKeyboard() ? getKeyIcon(mapping.getKey().getValue(),allowPressed) : mapping.getBinding() == null ? null : mapping.getBinding().bindingState.getIcon(allowPressed),action);
    }
    class Renderer implements Renderable{

        @Nullable private final Screen screen;
        private final Minecraft minecraft = Minecraft.getInstance();
        public boolean allowPressed;
        public Renderer(@Nullable Screen screen){
            this.screen = screen;
            allowPressed = screen != null;
        }

        public List<ControlTooltip> tooltips = new ArrayList<>();
        public Renderer add(LegacyKeyMapping mapping){
            return add(mapping,mapping::getDisplayName);
        }
        public Renderer add(LegacyKeyMapping mapping, Supplier<Component> action){
            return add(create(mapping,action,allowPressed));
        }
        public Renderer addCompound(Supplier<Component[]> control, Supplier<Component>  action){
            return add(create(()-> COMPOUND_COMPONENT_FUNCTION.apply(control.get()),action));
        }
        public Renderer add(Supplier<Component> control, Supplier<Component>  action){
            return add(create(control,action));
        }
        public Renderer add(ControlTooltip tooltip){
            tooltips.add(tooltip);
            return this;
        }
        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, float f) {
            if ((!ScreenUtil.getLegacyOptions().inGameTooltips().get() && screen == null) || minecraft.screen != screen) return;
            int xDiff = 0;
            RenderSystem.enableBlend();
            guiGraphics.setColor(1.0f,1.0f,1.0f,Math.max(screen == null ? 0.0f : 0.2f,  ScreenUtil.getHUDOpacity()));
            for (ControlTooltip tooltip : tooltips) {
                Component icon;
                Component action;
                if ((icon = tooltip.getIcon()) == null || (action = tooltip.getAction()) == null) continue;
                int controlWidth = minecraft.font.width(icon);
                guiGraphics.drawString(minecraft.font,icon,33 + xDiff, guiGraphics.guiHeight() - 29,0xFFFFFF,false);
                guiGraphics.drawString(minecraft.font,action,33 + xDiff + controlWidth + 2, guiGraphics.guiHeight() - 29,0xFFFFFF);
                xDiff +=controlWidth + minecraft.font.width(action) + 12;
            }
            guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
            RenderSystem.disableBlend();
        }
    }
    Component KEY_ICON = getControlIcon("" + '\uC000',Type.KEYBOARD);
    Component MOUSE_BASE = getControlIcon("" + '\uC000',Type.KEYBOARD);
    Component MOUSE_BASE_FOCUSED = getControlIcon("" + '\uC000',Type.KEYBOARD);
    Component KEY_PRESSED_ICON = getControlIcon("" + '\uC001',Type.KEYBOARD);
    static Component getKeyIcon(int i, boolean allowPressed){
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Type type = i >= 0 ? i <= 9 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM : null;
        if (type == null) return null;
        boolean isKeyDown = allowPressed && (type == InputConstants.Type.KEYSYM ? InputConstants.isKeyDown(window, i) : GLFW.glfwGetMouseButton(window, i) == 1);
        char c;
        switch (i){
            case InputConstants.MOUSE_BUTTON_LEFT -> c = isKeyDown ? '\uE000' : '\uD000';
            case InputConstants.MOUSE_BUTTON_RIGHT -> c = isKeyDown ? '\uE001' : '\uD001';
            case InputConstants.MOUSE_BUTTON_MIDDLE -> c = isKeyDown ? '\uE002' : '\uD002';
            case InputConstants.KEY_SPACE -> c = isKeyDown ? '\u1032' : '\u0032';
            case InputConstants.KEY_COMMA -> c = isKeyDown ? '\u1044' : '\u0044';
            case InputConstants.KEY_MINUS-> c = isKeyDown ? '\u1045' : '\u0045';
            case InputConstants.KEY_PERIOD -> c = isKeyDown ? '\u1046' : '\u0046';
            case InputConstants.KEY_SLASH -> c = isKeyDown ? '\u1047' : '\u0047';
            case InputConstants.KEY_SEMICOLON -> c = isKeyDown ? '\u1059' : '\u0059';
            case InputConstants.KEY_EQUALS -> c = isKeyDown ? '\u1061' : '\u0061';
            case InputConstants.KEY_0 -> c = isKeyDown ? '\u1048' : '\u0048';
            case InputConstants.KEY_1 -> c = isKeyDown ? '\u1049' : '\u0049';
            case InputConstants.KEY_2 -> c = isKeyDown ? '\u1050' : '\u0050';
            case InputConstants.KEY_3 -> c = isKeyDown ? '\u1051' : '\u0051';
            case InputConstants.KEY_4 -> c = isKeyDown ? '\u1052' : '\u0052';
            case InputConstants.KEY_5 -> c = isKeyDown ? '\u1053' : '\u0053';
            case InputConstants.KEY_6 -> c = isKeyDown ? '\u1054' : '\u0054';
            case InputConstants.KEY_7 -> c = isKeyDown ? '\u1055' : '\u0055';
            case InputConstants.KEY_8 -> c = isKeyDown ? '\u1056' : '\u0056';
            case InputConstants.KEY_9 -> c = isKeyDown ? '\u1057' : '\u0057';
            case InputConstants.KEY_A -> c = isKeyDown ? '\u1065' : '\u0065';
            case InputConstants.KEY_B -> c = isKeyDown ? '\u1066' : '\u0066';
            case InputConstants.KEY_C -> c = isKeyDown ? '\u1067' : '\u0067';
            case InputConstants.KEY_D -> c = isKeyDown ? '\u1068' : '\u0068';
            case InputConstants.KEY_E -> c = isKeyDown ? '\u1069' : '\u0069';
            case InputConstants.KEY_F -> c = isKeyDown ? '\u1070' : '\u0070';
            case InputConstants.KEY_G -> c = isKeyDown ? '\u1071' : '\u0071';
            case InputConstants.KEY_H -> c = isKeyDown ? '\u1072' : '\u0072';
            case InputConstants.KEY_I -> c = isKeyDown ? '\u1073' : '\u0073';
            case InputConstants.KEY_J -> c = isKeyDown ? '\u1074' : '\u0074';
            case InputConstants.KEY_K -> c = isKeyDown ? '\u1075' : '\u0075';
            case InputConstants.KEY_L -> c = isKeyDown ? '\u1076' : '\u0076';
            case InputConstants.KEY_M -> c = isKeyDown ? '\u1077' : '\u0077';
            case InputConstants.KEY_N -> c = isKeyDown ? '\u1078' : '\u0078';
            case InputConstants.KEY_O -> c = isKeyDown ? '\u1079' : '\u0079';
            case InputConstants.KEY_P -> c = isKeyDown ? '\u1080' : '\u0080';
            case InputConstants.KEY_Q -> c = isKeyDown ? '\u1081' : '\u0081';
            case InputConstants.KEY_R -> c = isKeyDown ? '\u1082' : '\u0082';
            case InputConstants.KEY_S -> c = isKeyDown ? '\u1083' : '\u0083';
            case InputConstants.KEY_T -> c = isKeyDown ? '\u1084' : '\u0084';
            case InputConstants.KEY_U -> c = isKeyDown ? '\u1085' : '\u0085';
            case InputConstants.KEY_V -> c = isKeyDown ? '\u1086' : '\u0086';
            case InputConstants.KEY_W -> c = isKeyDown ? '\u1087' : '\u0087';
            case InputConstants.KEY_X -> c = isKeyDown ? '\u1088' : '\u0088';
            case InputConstants.KEY_Y -> c = isKeyDown ? '\u1089' : '\u0089';
            case InputConstants.KEY_Z -> c = isKeyDown ? '\u1090' : '\u0090';
            case InputConstants.KEY_LBRACKET -> c = isKeyDown ? '\u1091' : '\u0091';
            case InputConstants.KEY_RBRACKET -> c = isKeyDown ? '\u1093' : '\u0093';
            case InputConstants.KEY_GRAVE-> c = isKeyDown ? '\u1096' : '\u0096';
            case InputConstants.KEY_ESCAPE -> c = isKeyDown ? '\u1256' : '\u0256';
            case InputConstants.KEY_RETURN -> c = isKeyDown ? '\u1257' : '\u0257';
            case InputConstants.KEY_TAB -> c = isKeyDown ? '\u1258' : '\u0258';
            case InputConstants.KEY_INSERT -> c = isKeyDown ? '\u1260' : '\u0260';
            case InputConstants.KEY_DELETE -> c = isKeyDown ? '\u1261' : '\u0261';
            case InputConstants.KEY_UP -> c = isKeyDown ? '\u1265' : '\u0265';
            case InputConstants.KEY_DOWN -> c = isKeyDown ? '\u1264' : '\u0264';
            case InputConstants.KEY_LEFT -> c = isKeyDown ? '\u1263' : '\u0263';
            case InputConstants.KEY_RIGHT -> c = isKeyDown ? '\u1262' : '\u0262';
            case InputConstants.KEY_PAGEDOWN -> c = isKeyDown ? '\u1267' : '\u0267';
            case InputConstants.KEY_PAGEUP -> c = isKeyDown ? '\u1266' : '\u0266';
            case InputConstants.KEY_HOME -> c = isKeyDown ? '\u1268' : '\u0268';
            case InputConstants.KEY_END -> c = isKeyDown ? '\u1269' : '\u0269';
            case InputConstants.KEY_CAPSLOCK -> c = isKeyDown ? '\u1280' : '\u0280';
            case InputConstants.KEY_F1 -> c = isKeyDown ? '\u1290' : '\u0290';
            case InputConstants.KEY_F2 -> c = isKeyDown ? '\u1291' : '\u0291';
            case InputConstants.KEY_F3 -> c = isKeyDown ? '\u1292' : '\u0292';
            case InputConstants.KEY_F4 -> c = isKeyDown ? '\u1293' : '\u0293';
            case InputConstants.KEY_F5 -> c = isKeyDown ? '\u1294' : '\u0294';
            case InputConstants.KEY_F6 -> c = isKeyDown ? '\u1295' : '\u0295';
            case InputConstants.KEY_F7 -> c = isKeyDown ? '\u1296' : '\u0296';
            case InputConstants.KEY_F8 -> c = isKeyDown ? '\u1297' : '\u0297';
            case InputConstants.KEY_F9 -> c = isKeyDown ? '\u1298' : '\u0298';
            case InputConstants.KEY_F10 -> c = isKeyDown ? '\u1299' : '\u0299';
            case InputConstants.KEY_F11 -> c = isKeyDown ? '\u1300' : '\u0300';
            case InputConstants.KEY_F12 -> c = isKeyDown ? '\u1301' : '\u0301';
            case InputConstants.KEY_LSHIFT -> c = isKeyDown ? '\u1340' : '\u0340';
            case InputConstants.KEY_LCONTROL -> c = isKeyDown ? '\u1341' : '\u0341';
            case InputConstants.KEY_LALT -> c = isKeyDown ? '\u1342' : '\u0342';
            case InputConstants.KEY_RSHIFT -> c = isKeyDown ? '\u1344' : '\u0344';
            case InputConstants.KEY_RCONTROL -> c = isKeyDown ? '\u1345' : '\u0345';
            case InputConstants.KEY_RALT -> c = isKeyDown ? '\u1346' : '\u0346';
            default -> {
                return type.getOrCreate(i).getDisplayName().copy().append(allowPressed && isKeyDown ? KEY_PRESSED_ICON : KEY_ICON);
            }
        }
        return getControlIcon(String.valueOf(c),Type.KEYBOARD);
    }
    class GuiManager extends SimplePreparableReloadListener<List<ControlTooltip>> {

        @Override
        protected List<ControlTooltip> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<ControlTooltip> controlTooltips = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();

            manager.listResources(CONTROL_TOOLTIPS + "/gui", (string) -> string.getPath().endsWith(".json")).forEach(((location, resource) -> {
                try {
                    BufferedReader bufferedReader = resource.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("tooltips");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e-> {
                            if (e instanceof JsonObject o) controlTooltips.add(guiControlTooltipFromJson(o));
                        });
                    else if (ioElement instanceof JsonObject o) controlTooltips.add(guiControlTooltipFromJson(o));
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
            return controlTooltips;
        }
        protected ControlTooltip guiControlTooltipFromJson(JsonObject o){
            LegacyKeyMapping mapping = (LegacyKeyMapping)KeyMapping.ALL.get(GsonHelper.getAsString(o, "keyMapping"));
            BiPredicate<Item, CompoundTag> itemPredicate = o.has("heldItem") ? o.get("heldItem") instanceof JsonObject obj ? JsonUtil.registryMatchesItem(obj) : o.get("heldItem").getAsBoolean() ? (i,t)-> i != null && i != Items.AIR : (i, t)-> false : (i, t)-> true;
            Predicate<Block> blockPredicate = o.has("hitBlock") ? o.get("hitBlock") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.BLOCK,obj) : o.get("hitBlock").getAsBoolean() ? b-> !b.defaultBlockState().isAir() : b-> false : b-> true;
            Predicate<EntityType<?>> entityPredicate = o.has("hitEntity") ? o.get("hitEntity") instanceof JsonObject obj ? JsonUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE,obj) : staticPredicate(o.get("hitEntity").getAsBoolean()) : e-> true;
            Minecraft minecraft = Minecraft.getInstance();
            Component c =o.get("action") instanceof JsonPrimitive p ? Component.translatable(p.getAsString()) : mapping.getDisplayName();
            return create(mapping, ()->minecraft.player != null && itemPredicate.test(minecraft.player.getMainHandItem().getItem(),minecraft.player.getMainHandItem().getTag()) && ((minecraft.hitResult instanceof BlockHitResult r && blockPredicate.test(minecraft.level.getBlockState(r.getBlockPos()).getBlock())) || (minecraft.hitResult instanceof EntityHitResult er && entityPredicate.test(er.getEntity().getType()))) ? c : null,false);
        }
        public static <T> Predicate<T> staticPredicate(boolean b){return o-> b;}

        @Override
        protected void apply(List<ControlTooltip> list, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            Minecraft minecraft = Minecraft.getInstance();
            guiControlRenderer.tooltips.clear();
            guiControlRenderer.add((LegacyKeyMapping) Minecraft.getInstance().options.keyInventory).add((LegacyKeyMapping) Legacy4JClient.keyCrafting).add((LegacyKeyMapping) Minecraft.getInstance().options.keyUse,()-> getActualUse(minecraft)).add((LegacyKeyMapping) Minecraft.getInstance().options.keyAttack,()->getMainAction(minecraft));
            guiControlRenderer.tooltips.addAll(list);
            guiControlRenderer.add((LegacyKeyMapping) minecraft.options.keyShift,()->  minecraft.player.isPassenger() ? CONTROL_ACTION_CACHE.getUnchecked(minecraft.player.getVehicle() instanceof LivingEntity ? "legacy.action.dismount" : "legacy.action.exit") : null);
            guiControlRenderer.add((LegacyKeyMapping) minecraft.options.keyPickItem,()-> getPickAction(minecraft));
        }
    }
    static Component getPickAction(Minecraft minecraft){
        ItemStack result;
        BlockState b;
        if ((minecraft.hitResult instanceof EntityHitResult r &&  (result = r.getEntity().getPickResult()) != null || minecraft.hitResult instanceof BlockHitResult h && h.getType() != HitResult.Type.MISS && !(result = (b = minecraft.level.getBlockState(h.getBlockPos())).getBlock().getCloneItemStack(minecraft.level,h.getBlockPos(),b)).isEmpty()) && (minecraft.gameMode.hasInfiniteItems() || minecraft.player.getInventory().findSlotMatchingItem(result) != -1))
            return minecraft.hitResult instanceof EntityHitResult ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.pick_entity") : ((LegacyKeyMapping) minecraft.options.keyPickItem).getDisplayName();

        return null;
    }
    static Component getMainAction(Minecraft minecraft){
        if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS){
            BlockState state = minecraft.level.getBlockState(r.getBlockPos());
            if (state.getBlock() instanceof NoteBlock && !minecraft.player.getAbilities().instabuild) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.play");
            else if ((minecraft.player.getAbilities().instabuild || state.getBlock().defaultDestroyTime() >= 0 && !minecraft.player.blockActionRestricted(minecraft.level,r.getBlockPos(),minecraft.gameMode.getPlayerMode()))) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.mine");
        }
        return null;
    }
    static Component getActualUse(Minecraft minecraft){
        BlockState blockState;
        if (minecraft.player.isSleeping()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.wake_up");
        if ((minecraft.hitResult instanceof BlockHitResult r && ((blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof ButtonBlock || blockState.getBlock() instanceof LeverBlock || blockState.getBlock() instanceof DoorBlock || blockState.getBlock() instanceof FenceGateBlock || (blockState.getMenuProvider(minecraft.level,r.getBlockPos()) != null || minecraft.level.getBlockEntity(r.getBlockPos()) instanceof MenuProvider)))) return CONTROL_ACTION_CACHE.getUnchecked((blockState.getBlock() instanceof AbstractChestBlock<?> || blockState.getBlock() instanceof ShulkerBoxBlock || blockState.getBlock() instanceof BarrelBlock) ? "legacy.action.open" :  "key.use");
        if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof AbstractVillager v && !v.getOffers().isEmpty()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.trade");
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack actualItem = minecraft.player.getItemInHand(hand);
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof BedBlock) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.sleep");
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof NoteBlock) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.change_pitch");
            if (canPlace(minecraft, hand)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.place");
            if (canFeed(minecraft, hand)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.feed");
            if ( actualItem.is(Items.IRON_INGOT) && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof IronGolem g  && g.getHealth() < g.getMaxHealth()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.repair");
            if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof TamableAnimal a && a.isTame() && a.isFood(actualItem) && a.getHealth() < a.getMaxHealth()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.heal");
            if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof TamableAnimal a && a.isTame() && a.isOwnedBy(minecraft.player)) return CONTROL_ACTION_CACHE.getUnchecked(a.isInSittingPose() ? "legacy.action.follow_me" : "legacy.action.sit" );
            if (canTame(minecraft, hand)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.tame");
            if (canSetLoveMode(minecraft,hand)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.love_mode");
            if (minecraft.hitResult instanceof BlockHitResult r && actualItem.getItem() instanceof BoneMealItem && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(minecraft.level,r.getBlockPos(),blockState)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.grow");
            if (minecraft.hitResult instanceof BlockHitResult r && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof ComposterBlock){
                if (blockState.getValue(ComposterBlock.LEVEL) == 8) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.collect");
                else if (blockState.getValue(ComposterBlock.LEVEL) < 7 && ComposterBlock.COMPOSTABLES.containsKey(actualItem.getItem())) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.fill");
            }
            if (minecraft.hitResult instanceof BlockHitResult r && !actualItem.isEmpty() && minecraft.level.getBlockEntity(r.getBlockPos()) instanceof CampfireBlockEntity e && e.getCookableRecipe(actualItem).isPresent()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.cook");
            if (actualItem.getItem() instanceof BrushItem && minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof BrushableBlock) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.brush");
            if (actualItem.getItem() instanceof Equipable) return CONTROL_ACTION_CACHE.getUnchecked(actualItem.getItem() instanceof ShieldItem ? "legacy.action.block" : "legacy.action.equip");
            if (actualItem.getItem() instanceof EmptyMapItem || actualItem.getItem() instanceof FishingRodItem) return CONTROL_ACTION_CACHE.getUnchecked("key.use");
            if (actualItem.getItem() instanceof FireworkRocketItem && (minecraft.player.isFallFlying() || minecraft.hitResult instanceof BlockHitResult && minecraft.hitResult.getType() != HitResult.Type.MISS)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.launch");
            if (actualItem.getItem() instanceof ShearsItem ){
                if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Sheep s && !s.isSheared()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.shear");
                else if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof PumpkinBlock) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.carve");
            }
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && (blockState = minecraft.level.getBlockState(r.getBlockPos())).getBlock() instanceof JukeboxBlock && blockState.getValue(HAS_RECORD)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.eject");
            if (actualItem.getItem() instanceof FoodOnAStickItem<?> i && minecraft.player.getControlledVehicle() instanceof ItemSteerable && minecraft.player.getControlledVehicle().getType() == i.canInteractWith) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.boost");
            if (actualItem.getItem() instanceof LeadItem){
                if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Mob m && m.canBeLeashed(minecraft.player)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.leash");
                if (minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS && minecraft.level.getBlockState(r.getBlockPos()).is(BlockTags.FENCES)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.attach");
            }
            if (actualItem.getItem() instanceof NameTagItem && actualItem.hasCustomHoverName() && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity e && !(e instanceof Player) && e.isAlive()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.name");
            if (actualItem.getItem() instanceof EggItem || actualItem.getItem() instanceof SnowballItem || actualItem.getItem() instanceof EnderpearlItem || actualItem.getItem() instanceof EnderEyeItem || actualItem.getItem() instanceof ThrowablePotionItem || actualItem.getItem() instanceof ExperienceBottleItem) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.throw");
            if (((actualItem.getItem() instanceof FlintAndSteelItem || actualItem.getItem() instanceof FireChargeItem) && minecraft.hitResult instanceof BlockHitResult r && minecraft.hitResult.getType() != HitResult.Type.MISS)&& (BaseFireBlock.canBePlacedAt(minecraft.level, r.getBlockPos().relative(r.getDirection()), minecraft.player.getDirection()) || CampfireBlock.canLight(blockState = minecraft.level.getBlockState(r.getBlockPos())) || CandleBlock.canLight(blockState) || CandleCakeBlock.canLight(blockState))) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.ignite");

            if (actualItem.getItem() instanceof ProjectileWeaponItem && !minecraft.player.getProjectile(actualItem).isEmpty()){
                if (minecraft.player.getUseItem() == actualItem) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.release");
                return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.draw");
            }
            if (actualItem.getItem() instanceof TridentItem){
                if (minecraft.player.getUseItem() == actualItem) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.throw");
                return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.charge");
            }
            HitResult bucketHitResult;
            if (actualItem.getItem() instanceof BucketItem i && mayInteractItemAt(minecraft,actualItem,bucketHitResult = (FluidBucketHooks.getFluid(i) == Fluids.EMPTY ? Item.getPlayerPOVHitResult(minecraft.level, minecraft.player,ClipContext.Fluid.SOURCE_ONLY) : minecraft.hitResult))){
                BlockState state;
                if (FluidBucketHooks.getFluid(i) != Fluids.EMPTY) return CONTROL_ACTION_CACHE.getUnchecked((state = minecraft.level.getBlockState(((BlockHitResult)bucketHitResult).getBlockPos())).getBlock() instanceof LiquidBlockContainer || state.getBlock() instanceof AbstractCauldronBlock ? "legacy.action.fill" : "legacy.action.empty");
                else if (minecraft.level.getBlockState(((BlockHitResult)bucketHitResult).getBlockPos()).getBlock() instanceof BucketPickup) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.collect");
            }
            if (minecraft.player.getItemInHand(hand).getItem() instanceof SaddleItem && minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof Saddleable s && s.isSaddleable() && !s.isSaddled()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.saddle");
            if ((actualItem.isEdible() && minecraft.player.canEat(false)) || actualItem.getItem() instanceof PotionItem) return CONTROL_ACTION_CACHE.getUnchecked(actualItem.getUseAnimation() == UseAnim.DRINK ? "legacy.action.drink" : "legacy.action.eat");
            if (canTill(minecraft, hand)) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.till");
            if (minecraft.player.getItemInHand(hand).getItem() instanceof AxeItem && minecraft.hitResult instanceof BlockHitResult r && AxeItem.STRIPPABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock()) != null) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.strip");
            if (minecraft.player.getItemInHand(hand).getItem() instanceof ShovelItem && minecraft.hitResult instanceof BlockHitResult r && ShovelItem.FLATTENABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock()) != null) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.dig_path");
        }
        if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity() instanceof AbstractHorse h && h.isTamed() && minecraft.player.isSecondaryUseActive()) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.open");
        if (minecraft.hitResult instanceof EntityHitResult r && r.getEntity().canAddPassenger(minecraft.player) && minecraft.player.canRide(r.getEntity())){
            if (r.getEntity() instanceof Boat) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.sail");
            else if (r.getEntity() instanceof AbstractMinecart m && m.getMinecartType() == AbstractMinecart.Type.RIDEABLE) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.ride");
            else if (r.getEntity() instanceof Saddleable s && !r.getEntity().isVehicle() && ((!(r.getEntity() instanceof AbstractHorse) && s.isSaddled()) || r.getEntity() instanceof AbstractHorse h && !minecraft.player.isSecondaryUseActive() && (h.isTamed() && !h.isFood(minecraft.player.getMainHandItem()) || minecraft.player.getMainHandItem().isEmpty()))) return CONTROL_ACTION_CACHE.getUnchecked("legacy.action.mount");
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
        return minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !usedItem.isEmpty() && ((usedItem.getItem() instanceof SpawnEggItem e && (!(minecraft.hitResult instanceof EntityHitResult r) || r.getEntity().getType() == e.getType(usedItem.getTag()))) || minecraft.hitResult instanceof BlockHitResult r && (usedItem.getItem() instanceof BlockItem b && (c =new BlockPlaceContext(minecraft.player,hand,usedItem,r)).canPlace() && b.getPlacementState(c) != null));
    }
    static boolean canTill(Minecraft minecraft, InteractionHand hand){
        if (!(minecraft.player.getItemInHand(hand).getItem() instanceof HoeItem && minecraft.hitResult != null && minecraft.hitResult instanceof BlockHitResult r)) return false;
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> use = HoeItem.TILLABLES.get(minecraft.level.getBlockState(r.getBlockPos()).getBlock());
        return use != null && use.getFirst().test(new UseOnContext(minecraft.player,hand,r));
    }
    static boolean canTame(Minecraft minecraft, InteractionHand hand){
        ItemStack usedItem = minecraft.player.getItemInHand(hand);
        return minecraft.hitResult != null && minecraft.hitResult instanceof EntityHitResult e && ((e.getEntity() instanceof TamableAnimal t && !t.isTame() && ((t instanceof Wolf && usedItem.is(Items.BONE)) || (!(t instanceof Wolf) && t.isFood(usedItem)))) || (hand == InteractionHand.MAIN_HAND && e.getEntity() instanceof AbstractHorse h && !h.isTamed() && !minecraft.player.isSecondaryUseActive() && usedItem.isEmpty()));
    }

}
