package wily.legacy.client;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.vehicle./*? if <1.21.2 {*/Boat/*?} else {*//*AbstractBoat*//*?}*/;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.CompoundTagUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.*;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.*;
import java.util.*;
import java.util.function.*;

import static wily.legacy.util.LegacyComponents.optionName;


public class LegacyOptions {
    public enum UIMode implements StringRepresentable {
        AUTO("auto"),
        FHD("fhd"),
        HD("hd"),
        SD("sd");

        public static final EnumCodec<UIMode> CODEC = StringRepresentable.fromEnum(UIMode::values);
        private final String name;
        public final Component displayName;

        UIMode(String name) {
            this.name = name;
            this.displayName = Component.translatable("legacy.options.uiMode." + name);
        }

        public boolean isFHD() {
            return this == FHD;
        }

        public boolean isHD() {
            return this == HD;
        }

        public boolean isSD() {
            return this == SD;
        }

        public boolean isHDOrLower() {
            return isHD() || isSD();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Deprecated
    private static final File deprecatedLegacyOptionssFile = FactoryAPI.getConfigDirectory().resolve("legacy_options.txt").toFile();
    @Deprecated
    private static final Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);
    private static final String DEFAULT_SKIN_CLOUD_RELAY_URL = "https://legacy4j-skins-relay.creepereater201.workers.dev";
    private static boolean suppressPlayerInfoSync = false;

    public static final Function<OptionInstance<?>,FactoryConfig<?>> LEGACY_OPTION_OPTION_INSTANCE_CACHE = Util.memoize(LegacyOptions::create);

    public static final Map<Component, Component> vanillaCaptionOverrideMap = new HashMap<>(Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak")));

    
    public static final FactoryConfig.StorageHandler CLIENT_STORAGE = new FactoryConfig.StorageHandler(){
        @Override
        public void load() {
            for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                LegacyKeyMapping mapping = LegacyKeyMapping.of(keyMapping);
                register(FactoryConfig.create("component_" + keyMapping.getName(), null, Optional.ofNullable(((LegacyKeyMapping) keyMapping).getDefaultBinding()), Bearer.of(()->Optional.ofNullable(mapping.getBinding()),o->mapping.setBinding(o.filter(b -> b.isBindable).orElse(null))), ()->ControllerBinding.OPTIONAL_CODEC, m->{}, this));
            }
            loadDeprecated();
            super.load();
            Legacy4JClient.isNewerVersion = Legacy4J.isNewerVersion(Legacy4J.VERSION.get(), lastLoadedVersion.get());
            Legacy4JClient.isNewerMinecraftVersion = Legacy4J.isNewerVersion(SharedConstants.getCurrentVersion().getName(), lastLoadedMinecraftVersion.get());
        }
    }.withFile("legacy/client_options.json");

    public static final FactoryConfig.StorageAccess VANILLA_STORAGE_ACCESS = ()-> Minecraft.getInstance().options.save();

    public static <T> FactoryConfig<T> of(OptionInstance<T> optionInstance) {
        return (FactoryConfig<T>) LEGACY_OPTION_OPTION_INSTANCE_CACHE.apply(optionInstance);
    }

    public static FactoryConfig<Double> ofSound(OptionInstance<Double> optionInstance, String captionKey) {
        return FactoryConfig.create(OptionInstanceAccessor.of(optionInstance).getKey(), new FactoryConfigDisplay.Instance<>(
                Component.translatable(captionKey),
                v-> componentFromTooltip(OptionInstanceAccessor.of(optionInstance).tooltip().apply(v)),
                (c,v)-> v <= 0.0 ? CommonComponents.optionNameValue(c, CommonComponents.OPTION_OFF) : Component.translatable("options.percent_value", c, Mth.floor(v * 100.0))
        ), OptionInstanceAccessor.of(optionInstance).defaultValue(), Bearer.of(optionInstance::get, optionInstance::set), FactoryConfigControl.createDouble(), v->{}, VANILLA_STORAGE_ACCESS);
    }

    public static <T> FactoryConfig<T> create(OptionInstance<T> optionInstance) {
        FactoryConfigControl<T> control;
        if (optionInstance.values().equals(OptionInstance.BOOLEAN_VALUES)){
            control = (FactoryConfigControl<T>) FactoryConfigControl.TOGGLE;
        } else if (optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> set) {
            control = new FactoryConfigControl.FromInt<>(optionInstance.codec(), i-> set.valueListSupplier().getSelectedList().get(i), v-> set.valueListSupplier().getSelectedList().indexOf(v), ()->set.valueListSupplier().getSelectedList().size());
        } else if (optionInstance.values() instanceof OptionInstance.SliderableValueSet<T> set) {
            control = new FactoryConfigControl.FromDouble<>(optionInstance.codec(), set::fromSliderValue, set::toSliderValue);
        } else return null;
        return FactoryConfig.create(OptionInstanceAccessor.of(optionInstance).getKey(), new FactoryConfigDisplay.Instance<>(vanillaCaptionOverrideMap.getOrDefault(optionInstance.caption,optionInstance.caption), v-> componentFromTooltip(OptionInstanceAccessor.of(optionInstance).tooltip().apply(v)), (c,v)-> optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> ? CommonComponents.optionNameValue(c,optionInstance.toString.apply(optionInstance.get())) : optionInstance.toString.apply(optionInstance.get())), OptionInstanceAccessor.of(optionInstance).defaultValue(), Bearer.of(optionInstance::get, v->{
            if (optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> set) {
                set.valueSetter().set(optionInstance,v);
            } else optionInstance.set(v);
        }), control, v->{}, VANILLA_STORAGE_ACCESS);
    }

    public static Component componentFromTooltip(Tooltip tooltip){
        return tooltip == null ? null : tooltip.message;
    }

    public static UIMode getUIMode() {
        if (uiMode.get() == UIMode.AUTO) {
            if (ScreenUtil.getStandardHeight() == 1080) return UIMode.FHD;
            return ScreenUtil.getStandardHeight() > 540 ? UIMode.HD : UIMode.SD;
        }
        return uiMode.get();
    }

    private static void reloadCloudRendering() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.levelRenderer == null) return;
            minecraft.levelRenderer.allChanged();
            ((LevelRendererAccessor) minecraft.levelRenderer).updateSkyBuffers();
            //? if >=1.21.2 {
            /*minecraft.levelRenderer.getCloudRenderer().markForRebuild();
            *///?}
        });
    }

    private static void updateSoundVolume() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.getSoundManager().updateSourceVolume(SoundSource.MASTER, minecraft.options.getSoundSourceVolume(SoundSource.MASTER)));
    }

    public static FactoryConfig<Boolean> createBoolean(String key, boolean defaultValue) {
        return createBoolean(key, defaultValue, b-> {});
    }

    public static FactoryConfig<Boolean> createBoolean(String key, boolean defaultValue, Consumer<Boolean> consumer) {
        return createBoolean(key, b->null, defaultValue, consumer);
    }

    public static FactoryConfig<Boolean> createBoolean(String key, Function<Boolean,Component> tooltipFunction, boolean defaultValue) {
        return createBoolean(key, tooltipFunction, defaultValue, b->{});
    }

    public static FactoryConfig<Boolean> createBoolean(String key, Function<Boolean,Component> tooltipFunction, boolean defaultValue, Consumer<Boolean> consumer) {
        return FactoryConfig.createBoolean(key, new FactoryConfigDisplay.Instance<>(optionName(key), tooltipFunction, (c, v) -> c), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Integer> createInteger(String key, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max, int defaultValue) {
        return createInteger(key, captionFunction, min, max, defaultValue, v-> {});
    }

    public static FactoryConfig<Integer> createInteger(String key, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max, int defaultValue, Consumer<Integer> consumer) {
        return createInteger(key, v-> null, captionFunction, min, max, defaultValue, consumer);
    }

    public static FactoryConfig<Integer> createInteger(String key, Function<Integer,Component> tooltipFunction, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max, int defaultValue, Consumer<Integer> consumer) {
        return createInteger(key, tooltipFunction, captionFunction, min, max, defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Integer> createInteger(String key, Function<Integer,Component> tooltipFunction, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max, int defaultValue, Consumer<Integer> consumer, FactoryConfig.StorageAccess access) {
        return FactoryConfig.createInteger(key, new FactoryConfigDisplay.Instance<>(optionName(key), tooltipFunction, captionFunction), new FactoryConfigControl.Int(min, max, Integer.MAX_VALUE), defaultValue, consumer, access);
    }

    public static <T> FactoryConfig<T> create(String key, BiFunction<Component,T,Component> captionFunction, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize, T defaultValue, Consumer<T> consumer, FactoryConfig.StorageAccess access) {
        return FactoryConfig.create(key, new FactoryConfigDisplay.Instance<>(optionName(key), captionFunction), new FactoryConfigControl.FromInt<>(valueGetter, valueSetter, valuesSize), defaultValue, consumer, access);
    }

    public static <T> FactoryConfig<T> create(String key, BiFunction<Component,T,Component> captionFunction, Supplier<List<T>> listSupplier, T defaultValue, Consumer<T> consumer) {
        return create(key, v-> null, captionFunction, listSupplier, defaultValue, consumer);
    }

    public static <T> FactoryConfig<T> create(String key, Function<T,Component> tooltipFunction, BiFunction<Component,T,Component> captionFunction, Supplier<List<T>> listSupplier, T defaultValue, Consumer<T> consumer) {
        return FactoryConfig.create(key, new FactoryConfigDisplay.Instance<>(optionName(key), tooltipFunction, captionFunction), new FactoryConfigControl.FromInt<>(i->listSupplier.get().get(i), v-> listSupplier.get().indexOf(v), ()-> listSupplier.get().size()), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Double> createDouble(String key, BiFunction<Component,Double,Component> captionFunction, double defaultValue) {
        return createDouble(key, captionFunction, defaultValue, b->{});
    }

    public static FactoryConfig<Double> createDouble(String key, BiFunction<Component,Double,Component> captionFunction, double defaultValue, Consumer<Double> consumer) {
        return createDouble(key, v-> null, captionFunction, defaultValue, consumer);
    }

    public static FactoryConfig<Double> createDouble(String key, Function<Double,Component> tooltipFunction, BiFunction<Component,Double,Component> captionFunction, double defaultValue) {
        return createDouble(key, tooltipFunction, captionFunction, defaultValue, b->{});
    }

    public static FactoryConfig<Double> createDouble(String key, Function<Double,Component> tooltipFunction, BiFunction<Component,Double,Component> captionFunction, double defaultValue, Consumer<Double> consumer) {
        return FactoryConfig.create(key, new FactoryConfigDisplay.Instance<>(optionName(key), tooltipFunction, captionFunction), FactoryConfigControl.createDouble(), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static Component percentValueLabel(Component component, double d) {
        return Component.translatable("options.percent_value", component, (int)(d * 100.0));
    }

    public static final FactoryConfig<String> lastLoadedVersion = CLIENT_STORAGE.register(FactoryConfig.create("lastLoadedVersion", null,()-> Codec.STRING,"", v-> {}, CLIENT_STORAGE));
    public static final FactoryConfig<String> lastLoadedMinecraftVersion = CLIENT_STORAGE.register(FactoryConfig.create("lastLoadedMinecraftVersion", null, ()-> Codec.STRING, "", v-> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> tu3ChangeSkinScreen = CLIENT_STORAGE.register(FactoryConfig.createBoolean("tu3ChangeSkinScreen", FactoryConfigDisplay.createToggle(Component.translatable("legacy.menu.change_skin.tu3_change_skin_screen")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> smoothPreviewScroll = CLIENT_STORAGE.register(FactoryConfig.createBoolean("smoothPreviewScroll", FactoryConfigDisplay.createToggle(Component.translatable("legacy.menu.change_skin.smooth_preview_scroll")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> hideArmorOnAllBoxSkins = CLIENT_STORAGE.register(FactoryConfig.createBoolean("hideArmorOnAllBoxSkins", FactoryConfigDisplay.createToggle(Component.translatable("legacy.menu.change_skin.hide_armor")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> showCustomPackOptionsTooltip = CLIENT_STORAGE.register(FactoryConfig.createBoolean("showCustomPackOptionsTooltip", FactoryConfigDisplay.createToggle(Component.translatable("legacy.menu.change_skin.custom_pack_options")), true, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> skinSelectionInitialized = FactoryConfig.<Boolean>builder().key("skinSelectionInitialized").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> legacyEntityDistance = CLIENT_STORAGE.register(createBoolean("legacyEntityDistance", true));
    public static final FactoryConfig<Boolean> legacyEntityDistanceInitialized = FactoryConfig.<Boolean>builder().key("legacyEntityDistanceInitialized").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> lastUsedCustomPackId = FactoryConfig.<String>builder().key("lastUsedCustomPackId").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> selectedSkinUserId = FactoryConfig.<String>builder().key("selectedSkinUserId").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> selectedSkinId = FactoryConfig.<String>builder().key("selectedSkinId").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> selectedSkinPackId = FactoryConfig.<String>builder().key("selectedSkinPackId").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<List<String>> favoriteSkinIds = FactoryConfig.<List<String>>builder().key("favoriteSkinIds").control(FactoryConfigControl.of(Codec.STRING.listOf())).defaultValue(List.of()).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> skinCloudRelayUrl = FactoryConfig.<String>builder().key("skinCloudRelayUrl").control(FactoryConfigControl.of(Codec.STRING)).defaultValue(DEFAULT_SKIN_CLOUD_RELAY_URL).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> animatedCharacter = CLIENT_STORAGE.register(createBoolean("animatedCharacter",true));
    public static final FactoryConfig<Boolean> classicCrafting = CLIENT_STORAGE.register(createBoolean("classicCrafting",false, b-> {
        syncLegacyClassicWorkstations(b);
        if (canSendPlayerInfoSync()) CommonNetwork.sendToServer(PlayerInfoSync.classicCrafting(b  || LegacyOptions.forceMixedCrafting.get(), Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> vanillaTabs = CLIENT_STORAGE.register(createBoolean("vanillaTabs",false));
    public static final FactoryConfig<Boolean> modCraftingTabs = CLIENT_STORAGE.register(createBoolean("modCraftingTabs",false));
    public static final FactoryConfig<Boolean> displayLegacyGamma = CLIENT_STORAGE.register(createBoolean("displayGamma", true));
    public static final FactoryConfig<Double> legacyGamma = CLIENT_STORAGE.register(createDouble("gamma", LegacyOptions::percentValueLabel, 0.5));
    public static final FactoryConfig<Boolean> displayHUD = CLIENT_STORAGE.register(createBoolean("displayHUD",true));
    public static final FactoryConfig<Boolean> displayHand = CLIENT_STORAGE.register(createBoolean("displayHand",true));
    public static final FactoryConfig<Boolean> legacyCreativeTab = CLIENT_STORAGE.register(createBoolean("creativeTab", true));
    public static final FactoryConfig<Boolean> legacyLeaderboards = CLIENT_STORAGE.register(createBoolean("leaderboards", true));
    public static final FactoryConfig<Boolean> globalLeaderboardsOptOut = FactoryConfig.<Boolean>builder().key("globalLeaderboardsOptOut").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> globalLeaderboardsEndpoint = FactoryConfig.<String>builder().key("globalLeaderboardsEndpoint").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("https://l4j-global-leaderboards-api.creepereater201.workers.dev").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> globalLeaderboardsSyncOnLaunch = FactoryConfig.<Boolean>builder().key("globalLeaderboardsSyncOnLaunch").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(true).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> globalLeaderboardsPrefetchAroundOnLaunch = FactoryConfig.<Boolean>builder().key("globalLeaderboardsPrefetchAroundOnLaunch").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> globalLeaderboardsPrefetchTopOnLaunch = FactoryConfig.<Boolean>builder().key("globalLeaderboardsPrefetchTopOnLaunch").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsAroundWindow = FactoryConfig.<Integer>builder().key("globalLeaderboardsAroundWindow").control(FactoryConfigControl.of(Codec.INT)).defaultValue(5).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsTopLimit = FactoryConfig.<Integer>builder().key("globalLeaderboardsTopLimit").control(FactoryConfigControl.of(Codec.INT)).defaultValue(100).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsSyncCooldownSeconds = FactoryConfig.<Integer>builder().key("globalLeaderboardsSyncCooldownSeconds").control(FactoryConfigControl.of(Codec.INT)).defaultValue(21600).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsFetchCooldownSeconds = FactoryConfig.<Integer>builder().key("globalLeaderboardsFetchCooldownSeconds").control(FactoryConfigControl.of(Codec.INT)).defaultValue(900).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsConnectTimeoutSeconds = FactoryConfig.<Integer>builder().key("globalLeaderboardsConnectTimeoutSeconds").control(FactoryConfigControl.of(Codec.INT)).defaultValue(10).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Integer> globalLeaderboardsReadTimeoutSeconds = FactoryConfig.<Integer>builder().key("globalLeaderboardsReadTimeoutSeconds").control(FactoryConfigControl.of(Codec.INT)).defaultValue(20).buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> searchCreativeTab = CLIENT_STORAGE.register(createBoolean("searchCreativeTab", false));
    public static final FactoryConfig<Integer> autoSaveInterval = CLIENT_STORAGE.register(createInteger("autoSaveInterval",(c,i)-> i == 0 ? Options.genericValueLabel(c,Component.translatable("options.off")) : Component.translatable( "legacy.options.mins_value",c, i * 5),0, ()-> 24,1, i->{/*? if >1.20.1 {*/if (Minecraft.getInstance().hasSingleplayerServer()) Minecraft.getInstance().getSingleplayerServer().onTickRateChanged();/*?}*/}));
    public static final FactoryConfig<Boolean> autoSaveWhenPaused = CLIENT_STORAGE.register(createBoolean("autoSaveWhenPaused",false));
    public static final FactoryConfig<Boolean> inGameTooltips = CLIENT_STORAGE.register(createBoolean("gameTooltips", true));
    public static final FactoryConfig<Boolean> tooltipBoxes = CLIENT_STORAGE.register(createBoolean("tooltipBoxes", true));
    public static final FactoryConfig<Boolean> hideAdvancedOptionsTooltip = CLIENT_STORAGE.register(createBoolean("hideAdvancedOptionsTooltip", false));
    public static final FactoryConfig<Boolean> displayPackManagementTooltips = CLIENT_STORAGE.register(createBoolean("displayPackManagementTooltips", true));
    public static final FactoryConfig<Boolean> legacySettingsMenus = CLIENT_STORAGE.register(createBoolean("legacySettingsMenus", false));
    public static final FactoryConfig<Boolean> displayGameMessages = CLIENT_STORAGE.register(createBoolean("displayGameMessages", true));
    public static final FactoryConfig<Boolean> deathMessages = CLIENT_STORAGE.register(createBoolean("deathMessages", true));
    public static final FactoryConfig<Boolean> hints = CLIENT_STORAGE.register(createBoolean("hints", true));
    public static final FactoryConfig<Boolean> flyingViewRolling = CLIENT_STORAGE.register(createBoolean("flyingViewRolling", true));
    public static final FactoryConfig<Boolean> directSaveLoad = CLIENT_STORAGE.register(createBoolean("directSaveLoad", false));
    public static final FactoryConfig<Boolean> vignette = CLIENT_STORAGE.register(createBoolean("vignette", false));
    public static final FactoryConfig<Boolean> minecartSounds = CLIENT_STORAGE.register(createBoolean("minecartSounds", true));
    public static final FactoryConfig<Boolean> backSound = CLIENT_STORAGE.register(createBoolean("backSound", true));
    public static final FactoryConfig<Boolean> hoverFocusSound = CLIENT_STORAGE.register(createBoolean("hoverFocusSound", false));
    public static final FactoryConfig<Boolean> unlinkMusicFromMasterVolume = CLIENT_STORAGE.register(createBoolean("unlinkMusicFromMasterVolume", true, b-> updateSoundVolume()));
    public static final FactoryConfig<Boolean> caveSounds = CLIENT_STORAGE.register(createBoolean("caveSounds", true));
    public static final FactoryConfig<Boolean> legacyPotionsBar = CLIENT_STORAGE.register(createBoolean("legacyPotionsBar", false));
    public static final FactoryConfig<Boolean> showVanillaRecipeBook = CLIENT_STORAGE.register(createBoolean("showVanillaRecipeBook", false));
    public static final FactoryConfig<Boolean> displayNameTagBorder = CLIENT_STORAGE.register(createBoolean("displayNameTagBorder", true));
    public static final FactoryConfig<Boolean> inGameOnlineIds = CLIENT_STORAGE.register(createBoolean("inGameOnlineIds", true));
    public static final FactoryConfig<Boolean> displayChatIndicators = CLIENT_STORAGE.register(createBoolean("displayChatIndicators", true));
    public static final FactoryConfig<Boolean> legacyItemTooltips = CLIENT_STORAGE.register(createBoolean("legacyItemTooltips", true));
    public static final FactoryConfig<Boolean> legacyItemTooltipScaling = CLIENT_STORAGE.register(createBoolean("legacyItemTooltipsScaling", true));
    public static final FactoryConfig<Boolean> invertYController = CLIENT_STORAGE.register(createBoolean("invertYController", false));
    public static final FactoryConfig<Boolean> invertControllerButtons = CLIENT_STORAGE.register(createBoolean("invertControllerButtons", false, (b)-> ControllerBinding.RIGHT_BUTTON.state().block(2)));
    public static final FactoryConfig<Integer> controllerLedRed = CLIENT_STORAGE.register(createInteger("controllerLedRed", (c, i) -> Component.translatable("options.generic_value", c, Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0xFF0000 | (i << 16)))), 0, () -> 255, 255, i -> Legacy4JClient.controllerManager.updateControllerLed()));
    public static final FactoryConfig<Integer> controllerLedGreen = CLIENT_STORAGE.register(createInteger("controllerLedGreen", (c, i) -> Component.translatable("options.generic_value", c, Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0x00FF00 | (i << 8)))), 0, () -> 255, 255, i -> Legacy4JClient.controllerManager.updateControllerLed()));
    public static final FactoryConfig<Integer> controllerLedBlue = CLIENT_STORAGE.register(createInteger("controllerLedBlue", (c, i) -> Component.translatable("options.generic_value", c, Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0x0000FF | i))), 0, () -> 255, 255, i -> Legacy4JClient.controllerManager.updateControllerLed()));
    public static final FactoryConfig<Integer> selectedController = CLIENT_STORAGE.register(createInteger("selectedController", (c, i)-> Component.translatable("options.generic_value",c,Component.literal(i+1 + (Legacy4JClient.controllerManager.connectedController == null ? "" : " (%s)".formatted(Legacy4JClient.controllerManager.connectedController.getName())))),  0, ()->15, 0, d -> { if (Legacy4JClient.controllerManager.connectedController!= null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);}));
    public static final FactoryConfig<Controller.Handler> selectedControllerHandler = CLIENT_STORAGE.register(create("selectedControllerHandler", (c, h)-> Component.translatable("options.generic_value",c,h.getName()), ()->((List<Controller.Handler>)ControllerManager.handlers.values()), SDLControllerHandler.getInstance(), d-> {
        ControllerBinding.LEFT_STICK.state().block(2);
        if (Legacy4JClient.controllerManager.connectedController != null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);
    }));
    public static final FactoryConfig<Integer> controllerPollingRate = CLIENT_STORAGE.register(createInteger("controllerPollingRate", i -> Component.translatable("legacy.options.controllerPollingRate.tooltip"), (c, i) -> CommonComponents.optionNameValue(c, Component.literal(i + " ms")), 1, () -> 16, 8, i -> Legacy4JClient.controllerManager.restartPoller()));
    public static final FactoryConfig<Boolean> controllerVirtualCursor = CLIENT_STORAGE.register(createBoolean("controllerVirtualCursor", true, b-> {}));
    public static final FactoryConfig<CursorMode> cursorMode = CLIENT_STORAGE.register(create("cursorMode", (c, d) -> CommonComponents.optionNameValue(c, d.displayName), i-> CursorMode.values()[i], CursorMode::ordinal, ()->CursorMode.values().length, CursorMode.AUTO, d -> Legacy4JClient.controllerManager.updateCursorMode(), CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> unfocusedInputs = CLIENT_STORAGE.register(createBoolean("unfocusedInputs", false));
    public static final FactoryConfig<Double> leftStickDeadZone = CLIENT_STORAGE.register(createDouble("leftStickDeadZone", LegacyOptions::percentValueLabel, 0.25));
    public static final FactoryConfig<Double> rightStickDeadZone = CLIENT_STORAGE.register(createDouble("rightStickDeadZone", LegacyOptions::percentValueLabel, 0.34));
    public static final FactoryConfig<Double> leftTriggerDeadZone = CLIENT_STORAGE.register(createDouble("leftTriggerDeadZone", LegacyOptions::percentValueLabel, 0.2));
    public static final FactoryConfig<Double> rightTriggerDeadZone = CLIENT_STORAGE.register(createDouble("rightTriggerDeadZone", LegacyOptions::percentValueLabel, 0.2));
    public static final FactoryConfig<Integer> hudScale = CLIENT_STORAGE.register(createInteger("hudScale", Options::genericValueLabel, 1, ()->3, 2));
    public static final FactoryConfig<UIMode> uiMode = CLIENT_STORAGE.register(create("uiMode", (c, d) -> CommonComponents.optionNameValue(c, d.displayName), i -> UIMode.values()[i], UIMode::ordinal, () -> UIMode.values().length, UIMode.AUTO, d -> Minecraft.getInstance().execute(Minecraft.getInstance()::resizeDisplay), CLIENT_STORAGE));
    public static final FactoryConfig<Double> hudOpacity = CLIENT_STORAGE.register(createDouble("hudOpacity", LegacyOptions::percentValueLabel, 0.8));
    public static final FactoryConfig<Double> hudDistance = CLIENT_STORAGE.register(createDouble("hudDistance", LegacyOptions::percentValueLabel, 1.0));
    public static final FactoryConfig<Double> interfaceSensitivity = CLIENT_STORAGE.register(createDouble("interfaceSensitivity", (c, d)-> percentValueLabel(c, d*2), 0.5, d -> {}));
    public static final FactoryConfig<Double> controllerSensitivity = CLIENT_STORAGE.register(FactoryConfig.create("controllerSensitivity", new FactoryConfigDisplay.Instance<>(Component.translatable("options.sensitivity"), (c, d)-> percentValueLabel(c, d*2)), FactoryConfigControl.createDouble(), 0.5, d -> {}, CLIENT_STORAGE));
    public static FactoryConfig<Double> combinedLookSensitivity() {
        FactoryConfig<Double> mouseSensitivity = of(Minecraft.getInstance().options.sensitivity());
        return FactoryConfig.create(
                "combinedLookSensitivity",
                mouseSensitivity.getDisplay(),
                mouseSensitivity.get(),
                Bearer.of(mouseSensitivity::get, d -> {
                    mouseSensitivity.set(d);
                    controllerSensitivity.set(d);
                }),
                FactoryConfigControl.createDouble(),
                d -> {},
                () -> {
                    mouseSensitivity.save();
                    controllerSensitivity.save();
                });
    }
    public static final FactoryConfig<Boolean> overrideTerrainFogStart = CLIENT_STORAGE.register(createBoolean("overrideTerrainFogStart", true));
    public static final FactoryConfig<Integer> terrainFogStart = CLIENT_STORAGE.register(createInteger("terrainFogStart", (c,i)-> CommonComponents.optionNameValue(c, Component.translatable("options.chunks", getTerrainFogStart())), 2, ()-> Minecraft.getInstance().options.renderDistance().get(), 4, d -> {}));
    public static final FactoryConfig<Boolean> overrideTerrainFogEnd = CLIENT_STORAGE.register(createBoolean("overrideTerrainFogEnd", true));
    public static final FactoryConfig<Integer> terrainFogEnd = CLIENT_STORAGE.register(createInteger("terrainFogEnd", (c,i)-> CommonComponents.optionNameValue(c, Component.translatable("options.chunks", i)), 2, ()-> 32, 16, d -> {}));
    public static final FactoryConfig<ControlType.Holder> selectedControlType = CLIENT_STORAGE.register(FactoryConfig.create("controlType", new FactoryConfigDisplay.Instance<>(optionName("controlType"), (c, i)-> Component.translatable("options.generic_value",c, i.isAuto() ? Component.translatable("legacy.options.auto_value", ControlType.getActiveType().getDisplayName()) : i.get().getDisplayName())), new FactoryConfigControl.FromInt<>(ControlType.Holder.CODEC, i-> i == 0 || ControlType.types.size() < i ? ControlType.Holder.AUTO : ControlType.Holder.of(ControlType.types.getByIndex(i - 1)), s1-> 1 + ControlType.types.indexOf(s1.get()), ()-> ControlType.types.size() + 1), ControlType.Holder.AUTO, v-> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Difficulty> createWorldDifficulty = CLIENT_STORAGE.register(FactoryConfig.create("createWorldDifficulty", new FactoryConfigDisplay.Instance<>(Component.translatable("options.difficulty"), Difficulty::getInfo, (c, d)-> CommonComponents.optionNameValue(c, d.getDisplayName())), new FactoryConfigControl.FromInt<>(Difficulty::byId, Difficulty::getId, ()->Difficulty.values().length), Difficulty.NORMAL, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> smoothMovement = CLIENT_STORAGE.register(createBoolean("smoothMovement",true));
    public static final FactoryConfig<Boolean> forceSmoothMovement = CLIENT_STORAGE.register(createBoolean("forceSmoothMovement", b-> LegacyComponents.MAY_BE_A_CHEAT,false));
    public static final FactoryConfig<Boolean> forceLegacyFlight = CLIENT_STORAGE.register(createBoolean("forceLegacyFlight", b-> LegacyComponents.MAY_BE_A_CHEAT,false));
    public static final FactoryConfig<Boolean> forceLegacySwimming = CLIENT_STORAGE.register(createBoolean("forceLegacySwimming", b-> LegacyComponents.MAY_BE_A_CHEAT,false));
    public static final FactoryConfig<Boolean> forceLegacyShieldControls = CLIENT_STORAGE.register(createBoolean("forceLegacyShieldControls", b-> LegacyComponents.MAY_BE_A_CHEAT,false));
    public static final FactoryConfig<Boolean> forceLegacyOffhandLimits = CLIENT_STORAGE.register(createBoolean("forceLegacyOffhandLimits", b-> LegacyComponents.MAY_BE_A_CHEAT,false));
    public static final FactoryConfig<Boolean> legacyCreativeBlockPlacing = CLIENT_STORAGE.register(createBoolean("legacyCreativeBlockPlacing",true));
    public static final FactoryConfig<Boolean> smoothAnimatedCharacter = CLIENT_STORAGE.register(createBoolean("smoothAnimatedCharacter",false));
    public static final FactoryConfig<Boolean> customSkinAnimation = CLIENT_STORAGE.register(createBoolean("customSkinAnimation", true));
    public static final FactoryConfig<Boolean> invertedCrosshair = CLIENT_STORAGE.register(createBoolean("invertedCrosshair",false));
    public static final FactoryConfig<Boolean> legacyDrownedHeight = CLIENT_STORAGE.register(createBoolean("legacyDrownedHeight",true));
    public static final FactoryConfig<Boolean> legacyDrownedAnimation = CLIENT_STORAGE.register(createBoolean("legacyDrownedAnimation",true));
    public static final FactoryConfig<Boolean> legacyZombieAggressionAnimation = CLIENT_STORAGE.register(createBoolean("legacyZombieAggressionAnimation", b -> Component.translatable("legacy.options.legacyZombieAggressionAnimation.tooltip"), false));
    public static final FactoryConfig<Boolean> merchantTradingIndicator = CLIENT_STORAGE.register(createBoolean("merchantTradingIndicator",true));
    public static final FactoryConfig<Boolean> itemLightingInHand = CLIENT_STORAGE.register(createBoolean("itemLightingInHand",true));
    public static final FactoryConfig<Boolean> loyaltyLines = CLIENT_STORAGE.register(createBoolean("loyaltyLines",true));
    public static final FactoryConfig<Boolean> controllerToggleCrouch = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleCrouch", new FactoryConfigDisplay.Instance<>(Component.translatable("options.key.toggleSneak")),true, b->{}, CLIENT_STORAGE));;
    public static final FactoryConfig<Boolean> controllerToggleSprint = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleSprint", new FactoryConfigDisplay.Instance<>(Component.translatable("options.key.toggleSprint")),false, b-> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> lockControlTypeChange = CLIENT_STORAGE.register(createBoolean("lockControlTypeChange",false));
    public static final FactoryConfig<Integer> selectedItemTooltipLines = CLIENT_STORAGE.register(createInteger("selectedItemTooltipLines", Options::genericValueLabel, 0,()->6, 4));
    public static final FactoryConfig<Boolean> itemTooltipEllipsis = CLIENT_STORAGE.register(createBoolean("itemTooltipEllipsis",true));
    public static final FactoryConfig<Integer> selectedItemTooltipSpacing = CLIENT_STORAGE.register(createInteger("selectedItemTooltipSpacing", Options::genericValueLabel, 8,()->12, 12));
    public static final FactoryConfig<VehicleCameraRotation> vehicleCameraRotation = CLIENT_STORAGE.register(create("vehicleCameraRotation", (c, d) -> CommonComponents.optionNameValue(c, d.displayName), i-> VehicleCameraRotation.values()[i], VehicleCameraRotation::ordinal, ()->VehicleCameraRotation.values().length, VehicleCameraRotation.ONLY_NON_LIVING_ENTITIES, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> defaultParticlePhysics = CLIENT_STORAGE.register(createBoolean("defaultParticlePhysics", true));
    public static final FactoryConfig<Boolean> linearCameraMovement = CLIENT_STORAGE.register(createBoolean("linearCameraMovement", false));
    public static final FactoryConfig<Boolean> legacyOverstackedItems = CLIENT_STORAGE.register(createBoolean("legacyOverstackedItems", true));
    public static final FactoryConfig<Boolean> displayMultipleControlsFromAction = CLIENT_STORAGE.register(createBoolean("displayMultipleControlsFromAction", false));
    public static final FactoryConfig<Boolean> enhancedPistonMovingRenderer = CLIENT_STORAGE.register(createBoolean("enhancedPistonMovingRenderer", true));
    public static final FactoryConfig<Boolean> legacyEntityFireTint = CLIENT_STORAGE.register(createBoolean("legacyEntityFireTint", true));
    public static final FactoryConfig<Boolean> advancedHeldItemTooltip = CLIENT_STORAGE.register(createBoolean("advancedHeldItemTooltip", false));
    public static final FactoryConfig<AdvancedOptionsMode> advancedOptionsMode = CLIENT_STORAGE.register(create("advancedOptionsMode", (c, d) -> CommonComponents.optionNameValue(c, d.displayName), i-> AdvancedOptionsMode.values()[i], AdvancedOptionsMode::ordinal, ()->AdvancedOptionsMode.values().length, AdvancedOptionsMode.DEFAULT, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> saveCache = CLIENT_STORAGE.register(createBoolean("saveCache", true));
    public static final FactoryConfig<Boolean> alwaysClearSaveCache = CLIENT_STORAGE.register(createBoolean("alwaysClearSaveCache", false));
    public static final FactoryConfig<Boolean> autoSaveCountdown = CLIENT_STORAGE.register(createBoolean("autoSaveCountdown", true));
    public static final FactoryConfig<Boolean> fakeAutosaveScreen = CLIENT_STORAGE.register(createBoolean("fakeAutosaveScreen", false));
    public static final FactoryConfig<Boolean> fakeManualSaveScreen = CLIENT_STORAGE.register(createBoolean("fakeManualSaveScreen", false));
    public static final FactoryConfig<Boolean> displayControlTooltips = CLIENT_STORAGE.register(createBoolean("displayControlTooltips", true));
    public static final FactoryConfig<Boolean> systemMessagesAsOverlay = CLIENT_STORAGE.register(createBoolean("systemMessagesAsOverlay", true));
    public static final FactoryConfig<Boolean> screenshotToasts = CLIENT_STORAGE.register(createBoolean("screenshotToasts", true));
    public static final FactoryConfig<Boolean> forceMixedCrafting = CLIENT_STORAGE.register(createBoolean("forceMixedCrafting", false, b-> {
        if (canSendPlayerInfoSync()) CommonNetwork.sendToServer(PlayerInfoSync.classicCrafting(classicCrafting.get() || b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicTrading = CLIENT_STORAGE.register(createBoolean("classicTrading",false, b-> {
        if (canSendPlayerInfoSync()) CommonNetwork.sendToServer(PlayerInfoSync.classicTrading(b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicStonecutting = CLIENT_STORAGE.register(createBoolean("classicStonecutting",false, b-> {
        if (canSendPlayerInfoSync()) CommonNetwork.sendToServer(PlayerInfoSync.classicStonecutting(b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicLoom = CLIENT_STORAGE.register(createBoolean("classicLoom",false, b-> {
        if (canSendPlayerInfoSync()) CommonNetwork.sendToServer(PlayerInfoSync.classicLoom(b, Minecraft.getInstance().player));
    }));
    private static void syncLegacyClassicWorkstations(boolean enabled) {
        if (!legacySettingsMenus.get()) return;
        FactoryConfig.saveOptionAndConsume(classicStonecutting, enabled, v -> {});
        FactoryConfig.saveOptionAndConsume(classicLoom, enabled, v -> {});
        FactoryConfig.saveOptionAndConsume(classicTrading, enabled, v -> {});
    }

    private static boolean advancedWorldOptionsVisible;

    public static boolean useLegacyWorldOptions() {
        return legacySettingsMenus.get() && !advancedWorldOptionsVisible;
    }

    public static boolean revealAdvancedWorldOptions() {
        if (!useLegacyWorldOptions()) return false;
        advancedWorldOptionsVisible = true;
        return true;
    }

    public static void resetAdvancedWorldOptions() {
        advancedWorldOptionsVisible = false;
    }

    public static String getLastUsedCustomPackIdOrNull() {
        String packId = lastUsedCustomPackId.get();
        return packId == null || packId.isBlank() ? null : packId;
    }

    public static String getSkinCloudRelayUrl() {
        String value = skinCloudRelayUrl.get();
        if (value == null) return DEFAULT_SKIN_CLOUD_RELAY_URL;
        value = value.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? DEFAULT_SKIN_CLOUD_RELAY_URL : value;
    }

    public static boolean canSendPlayerInfoSync() {
        Minecraft minecraft = Minecraft.getInstance();
        return !suppressPlayerInfoSync && minecraft.player != null && Legacy4JClient.hasModOnServer();
    }

    public static void runWithoutPlayerInfoSync(Runnable runnable) {
        boolean previous = suppressPlayerInfoSync;
        suppressPlayerInfoSync = true;
        try {
            runnable.run();
        } finally {
            suppressPlayerInfoSync = previous;
        }
    }

    public static final FactoryConfig<Boolean> headFollowsTheCamera = CLIENT_STORAGE.register(createBoolean("headFollowsTheCamera", true));
    public static final FactoryConfig<Boolean> fastLeavesWhenBlocked = CLIENT_STORAGE.register(createBoolean("fastLeavesWhenBlocked", true, b-> Legacy4JClient.updateChunks()));
    public static final FactoryConfig<Boolean> invertedFrontCameraPitch = CLIENT_STORAGE.register(createBoolean("invertedFrontCameraPitch", true, b-> {}));
    public static final FactoryConfig<Boolean> lceClouds = CLIENT_STORAGE.register(createBoolean("lceClouds", true, b-> reloadCloudRendering()));
    public static final FactoryConfig<Boolean> legacyCloudHeightAndTexture = CLIENT_STORAGE.register(createBoolean("legacyCloudHeightAndTexture", b-> Component.translatable("legacy.options.legacyCloudHeightAndTexture.tooltip"), false, b-> reloadCloudRendering()));
    public static final FactoryConfig<Boolean> legacySkyShape = CLIENT_STORAGE.register(createBoolean("legacySkyShape", true, b-> Legacy4JClient.updateSkyShape()));
    public static final FactoryConfig<Boolean> slowChunkLoading = CLIENT_STORAGE.register(createBoolean("slowChunkLoading", false, b-> LegacyChunkLoading.reset()));
    public static final FactoryConfig<Boolean> fastLeavesCustomModels = CLIENT_STORAGE.register(createBoolean("fastLeavesCustomModels", true, b-> Legacy4JClient.updateChunks()));
    public static final FactoryConfig<Boolean> skipIntro = CLIENT_STORAGE.register(createBoolean("skipIntro", false));
    public static final FactoryConfig<Boolean> legacyIntroAndReloading = CLIENT_STORAGE.register(createBoolean("legacyIntroAndReloading", true));
    public static final FactoryConfig<Boolean> skipInitialSaveWarning = CLIENT_STORAGE.register(createBoolean("skipInitialSaveWarning", false));
    public static final FactoryConfig<Boolean> titleScreenFade = CLIENT_STORAGE.register(createBoolean("titleScreenFade", false));
    public static final FactoryConfig<Boolean> titleScreenVersionText = CLIENT_STORAGE.register(createBoolean("titleScreenVersionText", false));
    public static final FactoryConfig<Boolean> legacyEvokerFangs = CLIENT_STORAGE.register(createBoolean("legacyEvokerFangs", true));
    public static final FactoryConfig<Boolean> vanillaTutorial = CLIENT_STORAGE.register(createBoolean("vanillaTutorial", false, o->{
        if (Minecraft.getInstance().level != null && !o) {
            Minecraft.getInstance().getTutorial().stop();
        }
    }));
    public static final FactoryConfig<Boolean> mapsWithCoords = CLIENT_STORAGE.register(createBoolean("mapsWithCoords", true));
    public static final FactoryConfig<Boolean> menusWithBackground = CLIENT_STORAGE.register(createBoolean("menusWithBackground", false));
    public static final FactoryConfig<Boolean> legacyPanorama = CLIENT_STORAGE.register(createBoolean("legacyPanorama", true));
    public static final FactoryConfig<Boolean> displayRealmsButton = CLIENT_STORAGE.register(createBoolean("displayRealmsButton", true));
    public static final FactoryConfig<Boolean> displayReinstallContentButton = CLIENT_STORAGE.register(createBoolean("displayReinstallContentButton", true));
    public static final FactoryConfig<Boolean> hideSodiumSettings = CLIENT_STORAGE.register(createBoolean("hideSodiumSettings", false));
    public static final FactoryConfig<Boolean> hideExperimentalWorldWarning = CLIENT_STORAGE.register(createBoolean("hideExperimentalWorldWarning", false));
    public static final FactoryConfig<Boolean> cursorAtFirstInventorySlot = CLIENT_STORAGE.register(createBoolean("cursorAtFirstInventorySlot", false));
    public static final FactoryConfig<Boolean> controllerCursorAtFirstInventorySlot = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerCursorAtFirstInventorySlot", new FactoryConfigDisplay.Instance<>(Component.translatable("legacy.options.cursorAtFirstInventorySlot")),true, b->{}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> systemCursor = CLIENT_STORAGE.register(createBoolean("systemCursor", false, b-> Legacy4JClient.controllerManager.updateCursorInputMode()));
    public static final FactoryConfig<ControlTooltipDisplay> controlTooltipDisplay = CLIENT_STORAGE.register(create("controlTooltipDisplay", (c, d) -> CommonComponents.optionNameValue(c, d.displayName), i-> ControlTooltipDisplay.values()[i], ControlTooltipDisplay::ordinal, ()->ControlTooltipDisplay.values().length, ControlTooltipDisplay.AUTO, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> legacyLoadingAndConnecting = CLIENT_STORAGE.register(createBoolean("legacyLoadingAndConnecting", true));
    public static final FactoryConfig<Boolean> unbindConflictingKeys = CLIENT_STORAGE.register(createBoolean("unbindConflictingKeys", true));
    public static final FactoryConfig<Boolean> unbindConflictingButtons = CLIENT_STORAGE.register(createBoolean("unbindConflictingButtons", true));
    public static final FactoryConfig<Integer> hudDelay = CLIENT_STORAGE.register(createInteger("hudDelay", (c, i)-> i == 0 ? Options.genericValueLabel(c,Component.translatable("options.off")) : percentValueLabel(c, i / 100d), 0, ()-> 200, 100));
    public static final FactoryConfig<Boolean> legacyBabyVillagerHead = CLIENT_STORAGE.register(createBoolean("legacyBabyVillagerHead", true));
    public static final FactoryConfig<Boolean> bubblesOutsideWater = CLIENT_STORAGE.register(createBoolean("bubblesOutsideWater", true));
    public static final FactoryConfig<Boolean> legacyItemPickup = CLIENT_STORAGE.register(createBoolean("legacyItemPickup", true));
    public static final FactoryConfig<Boolean> controllerToasts = CLIENT_STORAGE.register(createBoolean("controllerToasts", true));
    public static final FactoryConfig<Boolean> legacyHearts = CLIENT_STORAGE.register(createBoolean("legacyHearts", true));
    public static final FactoryConfig<Boolean> legacyFont = CLIENT_STORAGE.register(createBoolean("legacyFont", b -> Component.translatable("legacy.options.legacyFont.tooltip"), true));
    public static final FactoryConfig<Boolean> controllerDoubleClick = CLIENT_STORAGE.register(createBoolean("controllerDoubleClick", false));
    public static final FactoryConfig<Boolean> inventoryHoverFocusSound = CLIENT_STORAGE.register(createBoolean("inventoryHoverFocusSound", false));
    public static final FactoryConfig<Boolean> legacyCursor = CLIENT_STORAGE.register(createBoolean("legacyCursor", true));
    public static final FactoryConfig<Boolean> limitCursor = CLIENT_STORAGE.register(createBoolean("limitCursor", true));

    public static int getTerrainFogStart(){
        return Math.min(terrainFogStart.get(), Minecraft.getInstance().options.renderDistance().get());
    }

    public static boolean hasSystemCursor(){
        return systemCursor.get() && !Legacy4JClient.controllerManager.isControllerTheLastInput();
    }

    public static float getLeftStickDeadZone(){
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getControlledVehicle() instanceof /*? if <1.21.2 {*/Boat/*?} else {*//*AbstractBoat*//*?}*/ ? 0.5f + leftStickDeadZone.get().floatValue() / 2 : leftStickDeadZone.get().floatValue();
    }

    public enum VehicleCameraRotation implements StringRepresentable {
        NONE("none", LegacyComponents.NONE),ALL_ENTITIES("all_entities"),ONLY_NON_LIVING_ENTITIES("only_non_living_entities"),ONLY_LIVING_ENTITIES("only_living_entities");
        public static final EnumCodec<VehicleCameraRotation> CODEC = StringRepresentable.fromEnum(VehicleCameraRotation::values);
        private final String name;
        public final Component displayName;

        VehicleCameraRotation(String name, Component displayName){
            this.name = name;
            this.displayName = displayName;
        }
        VehicleCameraRotation(String name){
            this(name,Component.translatable("legacy.options.vehicleCameraRotation."+name));
        }
        public boolean isForLivingEntities(){
            return this == ALL_ENTITIES || this == ONLY_LIVING_ENTITIES;
        }
        public boolean isForNonLivingEntities(){
            return this == ALL_ENTITIES || this == ONLY_NON_LIVING_ENTITIES;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum AdvancedOptionsMode implements StringRepresentable {
        DEFAULT("default"),MERGE("merge"),HIDE("hide");
        public static final EnumCodec<AdvancedOptionsMode> CODEC = StringRepresentable.fromEnum(AdvancedOptionsMode::values);
        private final String name;
        public final Component displayName;

        AdvancedOptionsMode(String name, Component displayName){
            this.name = name;
            this.displayName = displayName;
        }
        AdvancedOptionsMode(String name) {
            this(name, Component.translatable("legacy.options.advancedOptionsMode." + name));
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum CursorMode implements StringRepresentable {
        AUTO("auto"),ALWAYS("always"),NEVER("never");
        public static final EnumCodec<CursorMode> CODEC = StringRepresentable.fromEnum(CursorMode::values);
        private final String name;
        public final Component displayName;

        CursorMode(String name, Component displayName){
            this.name = name;
            this.displayName = displayName;
        }

        CursorMode(String name){
            this(name, Component.translatable("legacy.options.cursorMode."+name));
        }

        public boolean isAuto(){
            return this == AUTO;
        }

        public boolean isAlways(){
            return this == ALWAYS;
        }

        public boolean isNever(){
            return this == NEVER;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum ControlTooltipDisplay implements StringRepresentable {
        AUTO("auto"),LEFT("left", HumanoidArm.LEFT.getCaption()),RIGHT("right", HumanoidArm.RIGHT.getCaption());
        public static final EnumCodec<ControlTooltipDisplay> CODEC = StringRepresentable.fromEnum(ControlTooltipDisplay::values);
        private final String name;
        public final Component displayName;

        ControlTooltipDisplay(String name, Component displayName){
            this.name = name;
            this.displayName = displayName;
        }

        ControlTooltipDisplay(String name){
            this(name, Component.translatable("legacy.options.controlTooltipDisplay."+name));
        }

        public boolean isRight(){
            return this == RIGHT || this == AUTO && Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT;
        }

        public boolean isLeft(){
            return this == LEFT || this == AUTO && Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Deprecated
    private static <T> DataResult<T> parseDeprecatedOptionFromString(String string, FactoryConfig<T> config){
        JsonReader jsonReader = new JsonReader(new StringReader(string));
        JsonElement jsonElement = JsonParser.parseReader(jsonReader);
        DataResult<T> dataResult = config.decode(new Dynamic<>(JsonOps.INSTANCE, jsonElement));
        dataResult.error().ifPresent(error -> Legacy4J.LOGGER.error("Error parsing option value {} for option {}: {}",string,config,error.message()));
        return dataResult;
    }

    @Deprecated
    private static void loadDeprecated(){
        if (!deprecatedLegacyOptionssFile.exists()) return;
        try {
            CompoundTag compoundTag = new CompoundTag();
            BufferedReader bufferedReader = Files.newReader(deprecatedLegacyOptionssFile, Charsets.UTF_8);

            try {
                bufferedReader.lines().forEach(string -> {
                    try {
                        Iterator<String> iterator = OPTION_SPLITTER.split(string).iterator();
                        compoundTag.putString(iterator.next(), iterator.next());
                    } catch (Exception var3) {
                        Legacy4J.LOGGER.warn("Skipping bad option: {}", string);
                    }
                });
            } catch (Throwable var6) {
                try {
                    bufferedReader.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }
            bufferedReader.close();
            CLIENT_STORAGE.configMap.forEach((s,o)-> CompoundTagUtil.getString(compoundTag, s).ifPresent(value -> parseDeprecatedOptionFromString(value.isEmpty() ? "\"\"" : value,o)));
        } catch (IOException e) {
            Legacy4J.LOGGER.error("Failed to load options", e);
        }
        deprecatedLegacyOptionssFile.delete();
    }
}
