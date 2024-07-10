package wily.legacy.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.util.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import static wily.legacy.Legacy4JClient.gammaEffect;

public class LegacyResourceManager implements PreparableReloadListener {

    public static final ResourceLocation GAMEPAD_MAPPINGS = ResourceLocation.tryBuild(Legacy4J.MOD_ID,"gamepad_mappings.txt");
    public static final ResourceLocation INTRO_LOCATION = ResourceLocation.tryBuild(Legacy4J.MOD_ID,"intro.json");
    public static final ResourceLocation GAMMA_LOCATION = ResourceLocation.tryBuild(Legacy4J.MOD_ID,"shaders/post/gamma.json");
    public static final String CONTROL_TYPES = "control_types.json";
    public static final String COMMON_COLORS = "common_colors.json";
    public static final String DEFAULT_KBM_ICONS = "control_tooltips/icons/kbm.json";
    public static final String DEFAULT_CONTROLLER_ICONS = "control_tooltips/icons/controller.json";
    public static final String DEFAULT_KEYBOARD_LAYOUT = "keyboard_layouts/en_us.json";

    public static final List<ResourceLocation> INTROS = new ArrayList<>();

    public static final List<KeyboardScreen.CharButtonBuilder> keyboardButtonBuilders = new ArrayList<>();
    public static ControllerBinding shiftBinding;
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
        return preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            profilerFiller2.startTick();
            profilerFiller2.push("listener");
            PackRepository repo = minecraft.getResourcePackRepository();
            if ((Legacy4JPlatform.isModLoaded("sodium") || Legacy4JPlatform.isModLoaded("rubidium")) && repo.getSelectedIds().contains("legacy:legacy_waters")){
                repo.removePack("legacy:legacy_waters");
                minecraft.reloadResourcePacks();
            }
            resourceManager.getResource(GAMEPAD_MAPPINGS).ifPresent(r->{
                try {
                    ControllerManager.getHandler().applyGamePadMappingsFromBuffer(r.openAsReader());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            if (gammaEffect != null) gammaEffect.close();
            try {
                gammaEffect = new PostChain(minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(), GAMMA_LOCATION);
                gammaEffect.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (IOException iOException) {
                Legacy4J.LOGGER.warn("Failed to load gamma: {}", GAMMA_LOCATION, iOException);
            } catch (JsonSyntaxException jsonSyntaxException) {
                Legacy4J.LOGGER.warn("Failed to parse shader: {}", GAMMA_LOCATION, jsonSyntaxException);
            }
            registerIntroLocations(resourceManager);

            ControlType.typesMap.clear();
            ControlType.types.clear();
            CommonColor.COMMON_COLORS.forEach((s,c)->c.reset());
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->{
                resourceManager.getResource(ResourceLocation.tryBuild(name, CONTROL_TYPES)).ifPresent(r->{
                    try {
                        GsonHelper.parseArray(r.openAsReader()).forEach(e-> {
                            ResourceLocation id;
                            ControlType type;
                            if (e instanceof JsonPrimitive p) {
                                id = ResourceLocation.parse(p.getAsString());
                                if (ControlType.typesMap.containsKey(id.toString()) && ControlType.defaultTypes.contains(ControlType.typesMap.get(id.toString()))) return;
                                for (ControlType defaultType : ControlType.defaultTypes) {
                                    if (defaultType.getId().equals(id)){
                                        ControlType.types.add(defaultType);
                                        ControlType.typesMap.put(id.toString(), defaultType);
                                        return;
                                    }
                                }
                                type = ControlType.create(id,null,false);
                            } else {
                                JsonObject o = e.getAsJsonObject();
                                id = ResourceLocation.parse(GsonHelper.getAsString(o,"id"));
                                type = ControlType.create(id,JsonUtil.getJsonStringOrNull(o,"displayName", Component::translatable),GsonHelper.getAsBoolean(o,"isKbm",false));
                            }
                            ControlType.types.add(type);
                            ControlType.typesMap.put(id.toString(), type);
                        });
                    } catch (IOException e) {
                        Legacy4J.LOGGER.warn(e.getMessage());
                    }
                });
                resourceManager.getResource(ResourceLocation.tryBuild(name, COMMON_COLORS)).ifPresent(r-> {
                    try {
                        JsonObject obj = GsonHelper.parse(r.openAsReader());
                        obj.asMap().forEach((s,e)-> {
                            if (CommonColor.COMMON_COLORS.containsKey(s)) CommonColor.COMMON_COLORS.get(s).tryParse(e);
                        });
                    } catch (IOException e) {
                        Legacy4J.LOGGER.warn(e.getMessage());
                    }
                });
                addKbmIcons(resourceManager, ResourceLocation.tryBuild(name,DEFAULT_KBM_ICONS),(s,b)->{
                    for (ControlType value : ControlType.types) if (value.isKbm()) value.getIcons().put(s, b);
                });
                addControllerIcons(resourceManager, ResourceLocation.tryBuild(name, DEFAULT_CONTROLLER_ICONS),(s, b)->{
                    for (ControlType value : ControlType.types) if (!value.isKbm()) value.getIcons().put(s, b);
                });
                for (ControlType value : ControlType.types) {
                    ResourceLocation location = ResourceLocation.tryBuild(value.getId().getNamespace(),"control_tooltips/icons/%s.json".formatted(value.getId().getPath()));
                    if (value.isKbm()) addKbmIcons(resourceManager,location,value.getIcons()::put);
                    else addControllerIcons(resourceManager, location, value.getIcons()::put);
                }

                try {
                    JsonObject obj = GsonHelper.parse(resourceManager.getResource(ResourceLocation.tryBuild(name,"keyboard_layout/" + minecraft.getLanguageManager().getSelected()+ ".json")).orElse(resourceManager.getResourceOrThrow(ResourceLocation.tryBuild(Legacy4J.MOD_ID,DEFAULT_KEYBOARD_LAYOUT))).openAsReader());
                    keyboardButtonBuilders.clear();
                    shiftBinding = obj.has("shiftBinding") ? ControllerBinding.CODEC.byName(obj.get("shiftBinding").getAsString()) : ControllerBinding.LEFT_STICK_BUTTON;
                    obj.getAsJsonArray("layout").forEach(e->{
                        if (e instanceof JsonObject o){
                            keyboardButtonBuilders.add(new KeyboardScreen.CharButtonBuilder(GsonHelper.getAsInt(o,"width",25),GsonHelper.getAsString(o,"chars"),GsonHelper.getAsString(o,"shiftChars",null),JsonUtil.getJsonStringOrNull(o,"binding",ControllerBinding.CODEC::byName),JsonUtil.getJsonStringOrNull(o,"icon",ResourceLocation::parse),JsonUtil.getJsonStringOrNull(o,"soundEvent",s-> BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(s)))));
                        }else if (e instanceof JsonPrimitive p) keyboardButtonBuilders.add(new KeyboardScreen.CharButtonBuilder(25,p.getAsString(),null,null,null,null));
                    });
                } catch (IOException e) {
                    Legacy4J.LOGGER.warn(e.getMessage());
                }
            });

            profilerFiller2.pop();
            profilerFiller2.endTick();
        }, executor2);
    }
    public static void addIcons(ResourceManager resourceManager, ResourceLocation location, BiConsumer<String,JsonObject> addIcon){
        resourceManager.getResource(location).ifPresent(r->{
            try {
                GsonHelper.parse(r.openAsReader()).asMap().forEach((s,o)-> addIcon.accept(s,o.getAsJsonObject()));
            } catch (IOException e) {
                Legacy4J.LOGGER.warn(e.getMessage());
            }
        });
    }
    public static void addControllerIcons(ResourceManager resourceManager, ResourceLocation location, BiConsumer<String,ControlTooltip.ComponentIcon> addIcon){
        addIcons(resourceManager,location,(s,o)->{
            ControllerBinding binding = ControllerBinding.CODEC.byName(s);
            addIcon.accept(s,ControlTooltip.ComponentIcon.create(()->binding.getMapped().bindingState.pressed, JsonUtil.getJsonStringOrNull(o,"icon",String::toCharArray),JsonUtil.getJsonStringOrNull(o,"iconOverlay",String::toCharArray),JsonUtil.getJsonStringOrNull(o,"tipIcon",v-> v.charAt(0)),()-> !binding.getMapped().bindingState.isBlocked(), ControlType::getActiveControllerType));
        });
    }
    public static void addKbmIcons(ResourceManager resourceManager, ResourceLocation location, BiConsumer<String,ControlTooltip.ComponentIcon> addIcon){
        addIcons(resourceManager,location,(s,o)->{
            InputConstants.Key key = InputConstants.getKey(s);
            ControlTooltip.ComponentIcon icon = ControlTooltip.ComponentIcon.create(key, JsonUtil.getJsonStringOrNull(o,"icon",String::toCharArray),JsonUtil.getJsonStringOrNull(o,"iconOverlay",String::toCharArray),JsonUtil.getJsonStringOrNull(o,"tipIcon",v-> v.charAt(0)));
            addIcon.accept(key.getName(), icon);
        });
    }
    public static void registerIntroLocations(ResourceManager resourceManager){
        try {
            INTROS.clear();
            JsonArray array = GsonHelper.parseArray(resourceManager.getResourceOrThrow(INTRO_LOCATION).openAsReader());
            array.forEach(e-> INTROS.add(ResourceLocation.parse(e.getAsString())));
        } catch (IOException e) {
            Legacy4J.LOGGER.error(e.getMessage());
        }
    }
}
