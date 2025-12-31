package wily.legacy.client;

import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.vehicle./*? if <1.21.2 {*//*Boat*//*?} else {*/AbstractBoat/*?}*/;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.*;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.util.IOUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.function.*;

import static wily.legacy.util.LegacyComponents.optionName;


public class LegacyOptions {
    public static final Function<OptionInstance<?>, FactoryConfig<?>> LEGACY_OPTION_OPTION_INSTANCE_CACHE = Util.memoize(LegacyOptions::create);

    public static final Map<Component, Component> vanillaCaptionOverrideMap = new HashMap<>(Map.of(
            Component.translatable("key.sprint"), Component.translatable("options.key.toggleSprint"),
            Component.translatable("key.sneak"), Component.translatable("options.key.toggleSneak"),
            Component.translatable("key.use"), Component.translatable("options.key.toggleUse"),
            Component.translatable("key.attack"), Component.translatable("options.key.toggleAttack")));

    
    public static final FactoryConfig.StorageHandler CLIENT_STORAGE = new FactoryConfig.StorageHandler() {
        @Override
        public void load() {
            for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                LegacyKeyMapping mapping = LegacyKeyMapping.of(keyMapping);
                register(FactoryConfig.create("component_" + keyMapping.getName(), null, Optional.ofNullable(((LegacyKeyMapping) keyMapping).getDefaultBinding()), Bearer.of(()->Optional.ofNullable(mapping.getBinding()),o->mapping.setBinding(o.filter(b -> b.isBindable).orElse(null))), ()->ControllerBinding.OPTIONAL_CODEC, m->{}, this));
            }
            super.load();
            Legacy4JClient.isNewerVersion = Legacy4J.isNewerVersion(Legacy4J.VERSION.get(), lastLoadedVersion.get());
            Legacy4JClient.isNewerMinecraftVersion = Legacy4J.isNewerVersion(SharedConstants.getCurrentVersion().name(), lastLoadedMinecraftVersion.get());
        }
    }.withFile("legacy/client_options.json");

    public static final FactoryConfig.StorageAccess VANILLA_STORAGE_ACCESS = ()-> Minecraft.getInstance().options.save();

    public static <T> FactoryConfig<T> of(OptionInstance<T> optionInstance) {
        return (FactoryConfig<T>) LEGACY_OPTION_OPTION_INSTANCE_CACHE.apply(optionInstance);
    }

    public static <T> FactoryConfig<T> create(OptionInstance<T> optionInstance) {
        FactoryConfigControl<T> control;
        if (optionInstance.values().equals(OptionInstance.BOOLEAN_VALUES)) {
            control = (FactoryConfigControl<T>) FactoryConfigControl.TOGGLE;
        } else if (optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> set) {
            control = new FactoryConfigControl.FromInt<>(optionInstance.codec(), i -> set.valueListSupplier().getSelectedList().get(i), v-> set.valueListSupplier().getSelectedList().indexOf(v), ()->set.valueListSupplier().getSelectedList().size());
        } else if (optionInstance.values() instanceof OptionInstance.SliderableValueSet<T> set) {
            control = new FactoryConfigControl.FromDouble<>(optionInstance.codec(), set::fromSliderValue, set::toSliderValue);
        } else return null;
        return FactoryConfig.create(OptionInstanceAccessor.of(optionInstance).getKey(), FactoryConfigDisplay.<T>builder().tooltip(v -> componentFromTooltip(OptionInstanceAccessor.of(optionInstance).tooltip().apply(v))).valueToComponent(optionInstance.toString).messageFunctionLabel((c, v) -> optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> ? CommonComponents.optionNameValue(c, v) : v).build(vanillaCaptionOverrideMap.getOrDefault(optionInstance.caption, optionInstance.caption)), OptionInstanceAccessor.of(optionInstance).defaultValue(), Bearer.of(optionInstance::get, v->{
            if (optionInstance.values() instanceof OptionInstance.CycleableValueSet<T> set) {
                set.valueSetter().set(optionInstance,v);
            } else optionInstance.set(v);
        }), control, v->{}, VANILLA_STORAGE_ACCESS);
    }

    public static Component componentFromTooltip(Tooltip tooltip) {
        return tooltip == null ? null : tooltip.message;
    }

    public static FactoryConfig<Boolean> createBoolean(String key, boolean defaultValue) {
        return createBoolean(key, defaultValue, b -> {});
    }

    public static FactoryConfig<Boolean> createBoolean(String key, boolean defaultValue, Consumer<Boolean> consumer) {
        return createBoolean(key, b -> null, defaultValue, consumer);
    }

    public static FactoryConfig<Boolean> createBoolean(String key, Function<Boolean,Component> tooltipFunction, boolean defaultValue) {
        return createBoolean(key, tooltipFunction, defaultValue, b -> {});
    }

    public static FactoryConfig<Boolean> createBoolean(String key, Function<Boolean,Component> tooltipFunction, boolean defaultValue, Consumer<Boolean> consumer) {
        return FactoryConfig.createBoolean(key, FactoryConfigDisplay.createToggle(optionName(key), tooltipFunction), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Integer> createInteger(String key, Function<FactoryConfigDisplay.Builder<Integer>, FactoryConfigDisplay.Builder<Integer>> display, int min, IntSupplier max, int defaultValue) {
        return createInteger(key, display, min, max, defaultValue, i -> {});
    }

    public static FactoryConfig<Integer> createInteger(String key, Function<FactoryConfigDisplay.Builder<Integer>, FactoryConfigDisplay.Builder<Integer>> display, int min, IntSupplier max, int defaultValue, Consumer<Integer> consumer) {
        return createInteger(key, display, min, max, defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Integer> createInteger(String key, Function<FactoryConfigDisplay.Builder<Integer>, FactoryConfigDisplay.Builder<Integer>> display, int min, IntSupplier max, int defaultValue, Consumer<Integer> consumer, FactoryConfig.StorageAccess access) {
        return FactoryConfig.createInteger(key, display.apply(FactoryConfigDisplay.intBuilder()).build(optionName(key)), new FactoryConfigControl.Int(min, max, Integer.MAX_VALUE), defaultValue, consumer, access);
    }

    public static <T> FactoryConfig<T> create(String key, Function<FactoryConfigDisplay.Builder<T>, FactoryConfigDisplay.Builder<T>> builderFunction, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize, Codec<T> codec, T defaultValue, Consumer<T> consumer, FactoryConfig.StorageAccess access) {
        return FactoryConfig.create(key, builderFunction.apply(FactoryConfigDisplay.builder()).build(optionName(key)), new FactoryConfigControl.FromInt<>(IOUtil.createFallbackCodec(codec, Codec.INT.xmap(valueGetter, valueSetter)), valueGetter, valueSetter, valuesSize), defaultValue, consumer, access);
    }

    public static <T> FactoryConfig<T> create(String key, Function<FactoryConfigDisplay.Builder<T>, FactoryConfigDisplay.Builder<T>> builderFunction, Supplier<List<T>> listSupplier, T defaultValue, Consumer<T> consumer) {
        return FactoryConfig.create(key, builderFunction.apply(FactoryConfigDisplay.builder()).build(optionName(key)), new FactoryConfigControl.FromInt<>(i -> listSupplier.get().get(i), v-> listSupplier.get().indexOf(v), ()-> listSupplier.get().size()), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static FactoryConfig<Double> createDouble(String key, Function<FactoryConfigDisplay.Builder<Double>, FactoryConfigDisplay.Builder<Double>> builderFunction, double defaultValue) {
        return createDouble(key, builderFunction, defaultValue, b -> {});
    }

    public static FactoryConfig<Double> createDouble(String key, Function<FactoryConfigDisplay.Builder<Double>, FactoryConfigDisplay.Builder<Double>> builderFunction, double defaultValue, Consumer<Double> consumer) {
        return FactoryConfig.create(key, builderFunction.apply(FactoryConfigDisplay.percentBuilder()).build(optionName(key)), FactoryConfigControl.createDouble(), defaultValue, consumer, CLIENT_STORAGE);
    }

    public static final FactoryConfig<String> lastLoadedVersion = FactoryConfig.<String>builder().key("lastLoadedVersion").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<String> lastLoadedMinecraftVersion = FactoryConfig.<String>builder().key("lastLoadedMinecraftVersion").control(FactoryConfigControl.of(Codec.STRING)).defaultValue("").buildAndRegister(CLIENT_STORAGE);
    public static final FactoryConfig<Boolean> animatedCharacter = CLIENT_STORAGE.register(createBoolean("animatedCharacter",true));
    public static final FactoryConfig<Boolean> classicCrafting = CLIENT_STORAGE.register(createBoolean("classicCrafting",false, b -> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(PlayerInfoSync.classicCrafting(b  || LegacyOptions.forceMixedCrafting.get(), Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> vanillaTabs = CLIENT_STORAGE.register(createBoolean("vanillaTabs",false));
    public static final FactoryConfig<Boolean> modCraftingTabs = CLIENT_STORAGE.register(createBoolean("modCraftingTabs",false));
    public static final FactoryConfig<Boolean> displayLegacyGamma = CLIENT_STORAGE.register(createBoolean("displayGamma", true));
    public static final FactoryConfig<Double> legacyGamma = CLIENT_STORAGE.register(createDouble("gamma", Function.identity(), 0.5));
    public static final FactoryConfig<Boolean> displayHUD = CLIENT_STORAGE.register(createBoolean("displayHUD",true));
    public static final FactoryConfig<Boolean> displayHand = CLIENT_STORAGE.register(createBoolean("displayHand",true));
    public static final FactoryConfig<Boolean> legacyCreativeTab = CLIENT_STORAGE.register(createBoolean("creativeTab", true));
    public static final FactoryConfig<Boolean> legacyAdvancements = CLIENT_STORAGE.register(createBoolean("advancements", true));
    public static final FactoryConfig<Boolean> legacyLeaderboards = CLIENT_STORAGE.register(createBoolean("leaderboards", true));
    public static final FactoryConfig<Boolean> searchCreativeTab = CLIENT_STORAGE.register(createBoolean("searchCreativeTab", false));
    public static final FactoryConfig<Integer> autoSaveInterval = CLIENT_STORAGE.register(createInteger("autoSaveInterval", builder -> builder.messageFunction((display, value) -> value == 0 ? CommonComponents.optionNameValue(display.name(), CommonComponents.OPTION_OFF) : Component.translatable("legacy.options.mins_value", display.name(), value * 5)),0, ()-> 24,1, i -> {/*? if >1.20.1 {*/if (Minecraft.getInstance().hasSingleplayerServer()) Minecraft.getInstance().getSingleplayerServer().onTickRateChanged();/*?}*/}));
    public static final FactoryConfig<Boolean> autoSaveWhenPaused = CLIENT_STORAGE.register(createBoolean("autoSaveWhenPaused",false));
    public static final FactoryConfig<Boolean> inGameTooltips = CLIENT_STORAGE.register(createBoolean("gameTooltips", true));
    public static final FactoryConfig<Boolean> tooltipBoxes = CLIENT_STORAGE.register(createBoolean("tooltipBoxes", true));
    public static final FactoryConfig<Boolean> hints = CLIENT_STORAGE.register(createBoolean("hints", true));
    public static final FactoryConfig<Boolean> flyingViewRolling = CLIENT_STORAGE.register(createBoolean("flyingViewRolling", true));
    public static final FactoryConfig<Boolean> directSaveLoad = CLIENT_STORAGE.register(createBoolean("directSaveLoad", false));
    public static final FactoryConfig<Boolean> vignette = CLIENT_STORAGE.register(createBoolean("vignette", false));
    public static final FactoryConfig<Boolean> minecartSounds = CLIENT_STORAGE.register(createBoolean("minecartSounds", true));
    public static final FactoryConfig<Boolean> backSound = CLIENT_STORAGE.register(createBoolean("backSound", true));
    public static final FactoryConfig<Boolean> hoverFocusSound = CLIENT_STORAGE.register(createBoolean("hoverFocusSound", false));
    public static final FactoryConfig<Boolean> caveSounds = CLIENT_STORAGE.register(createBoolean("caveSounds", true));
    public static final FactoryConfig<Boolean> showVanillaRecipeBook = CLIENT_STORAGE.register(createBoolean("showVanillaRecipeBook", false));
    public static final FactoryConfig<Boolean> displayNameTagBorder = CLIENT_STORAGE.register(createBoolean("displayNameTagBorder", true));
    public static final FactoryConfig<Boolean> legacyItemTooltips = CLIENT_STORAGE.register(createBoolean("legacyItemTooltips", true));
    public static final FactoryConfig<Boolean> legacyItemTooltipScaling = CLIENT_STORAGE.register(createBoolean("legacyItemTooltipsScaling", true));
    public static final FactoryConfig<Boolean> invertYController = CLIENT_STORAGE.register(createBoolean("invertYController", false));
    public static final FactoryConfig<Boolean> invertControllerButtons = CLIENT_STORAGE.register(createBoolean("invertControllerButtons", false, (b)-> ControllerBinding.RIGHT_BUTTON.state().block(2)));
    public static final FactoryConfig<Integer> selectedController = CLIENT_STORAGE.register(createInteger("selectedController", builder -> builder.valueToComponent(i -> Component.literal(i + 1 + (Legacy4JClient.controllerManager.connectedController == null ? "" : " (%s)".formatted(Legacy4JClient.controllerManager.connectedController.getName())))), 0, () -> 15, 0, Legacy4JClient.controllerManager::connectTo));
    public static final FactoryConfig<Controller.Handler> selectedControllerHandler = CLIENT_STORAGE.register(create("selectedControllerHandler", builder -> builder.valueToComponent(Controller.Handler::getName), ()->((List<Controller.Handler>)ControllerManager.handlers.values()), SDLControllerHandler.getInstance(), Legacy4JClient.controllerManager::updateHandler));
    public static final FactoryConfig<Boolean> controllerVirtualCursor = CLIENT_STORAGE.register(createBoolean("controllerVirtualCursor", true, b -> {}));
    public static final FactoryConfig<CursorMode> cursorMode = CLIENT_STORAGE.register(create("cursorMode", builder -> builder.valueToComponent(v -> v.displayName), i -> CursorMode.values()[i], CursorMode::ordinal, ()->CursorMode.values().length, CursorMode.CODEC, CursorMode.AUTO, d -> Legacy4JClient.controllerManager.updateCursorMode(), CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> unfocusedInputs = CLIENT_STORAGE.register(createBoolean("unfocusedInputs", b -> Component.translatable("legacy.options.unfocusedInputs.tooltip"), false));
    public static final FactoryConfig<Double> leftStickDeadZone = CLIENT_STORAGE.register(createDouble("leftStickDeadZone", Function.identity(), 0.25));
    public static final FactoryConfig<Double> rightStickDeadZone = CLIENT_STORAGE.register(createDouble("rightStickDeadZone", Function.identity(), 0.34));
    public static final FactoryConfig<Double> leftTriggerDeadZone = CLIENT_STORAGE.register(createDouble("leftTriggerDeadZone", Function.identity(), 0.2));
    public static final FactoryConfig<Double> rightTriggerDeadZone = CLIENT_STORAGE.register(createDouble("rightTriggerDeadZone", Function.identity(), 0.2));
    public static final FactoryConfig<Integer> hudSize = CLIENT_STORAGE.register(createInteger("hudScale", Function.identity(), 1, () -> 3, 2));
    public static final FactoryConfig<Double> hudOpacity = CLIENT_STORAGE.register(createDouble("hudOpacity", Function.identity(), 0.8));
    public static final FactoryConfig<Double> hudDistance = CLIENT_STORAGE.register(createDouble("hudDistance", Function.identity(), 1.0));
    public static final FactoryConfig<Double> interfaceSensitivity = CLIENT_STORAGE.register(createDouble("interfaceSensitivity", builder -> builder.valueToComponent(d -> Component.literal(String.valueOf((int)(d * 200)))), 0.5, d -> {}));
    public static final FactoryConfig<Double> controllerSensitivity = CLIENT_STORAGE.register(FactoryConfig.create("controllerSensitivity", FactoryConfigDisplay.percentBuilder().valueToComponent(d -> Component.literal(String.valueOf((int)(d * 200)))).build(Component.translatable("options.sensitivity")), FactoryConfigControl.createDouble(), 0.5, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> overrideTerrainFogStart = CLIENT_STORAGE.register(createBoolean("overrideTerrainFogStart", true));
    public static final FactoryConfig<Integer> terrainFogStart = CLIENT_STORAGE.register(createInteger("terrainFogStart", builder -> builder.valueToComponent(i -> Component.translatable("options.chunks", Math.min(i, Minecraft.getInstance().options.renderDistance().get()))), 2, ()-> Minecraft.getInstance().options.renderDistance().get(), 4, d -> {}));
    public static final FactoryConfig<Boolean> overrideTerrainFogEnd = CLIENT_STORAGE.register(createBoolean("overrideTerrainFogEnd", true));
    public static final FactoryConfig<Integer> terrainFogEnd = CLIENT_STORAGE.register(createInteger("terrainFogEnd", builder -> builder.valueToComponent(i -> Component.translatable("options.chunks", i)), 2, () -> 32, 16, d -> {}));
    public static final FactoryConfig<OptionHolder<ControlType>> selectedControlType = CLIENT_STORAGE.register(FactoryConfig.create("controlType", FactoryConfigDisplay.<OptionHolder<ControlType>>builder().valueToComponent(i -> i.isAuto() ? Component.translatable("legacy.options.auto_value", ControlType.getActiveType().nameOrEmpty()) : i.get().nameOrEmpty()).build(optionName("controlType")), new FactoryConfigControl.FromInt<>(ControlType.OPTION_CODEC, i -> i == 0 || Legacy4JClient.controlTypesManager.map().size() < i ? OptionHolder.auto() : OptionHolder.of(Legacy4JClient.controlTypesManager.map().getByIndex(i - 1)), s1-> 1 + Legacy4JClient.controlTypesManager.map().indexOf(s1.get()), ()-> Legacy4JClient.controlTypesManager.map().size() + 1), OptionHolder.auto(), v-> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Difficulty> createWorldDifficulty = CLIENT_STORAGE.register(FactoryConfig.create("createWorldDifficulty", FactoryConfigDisplay.<Difficulty>builder().tooltip(Difficulty::getInfo).valueToComponent(Difficulty::getDisplayName).build(Component.translatable("options.difficulty")), new FactoryConfigControl.FromInt<>(Difficulty::byId, Difficulty::getId, ()->Difficulty.values().length), Difficulty.NORMAL, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> smoothMovement = CLIENT_STORAGE.register(createBoolean("smoothMovement",true));
    public static final FactoryConfig<Boolean> forceSmoothMovement = CLIENT_STORAGE.register(createBoolean("forceSmoothMovement", b -> LegacyComponents.MAY_BE_A_CHEAT, false));
    public static final FactoryConfig<Boolean> legacyCreativeBlockPlacing = CLIENT_STORAGE.register(createBoolean("legacyCreativeBlockPlacing",true));
    public static final FactoryConfig<Boolean> smoothAnimatedCharacter = CLIENT_STORAGE.register(createBoolean("smoothAnimatedCharacter",false));
    public static final FactoryConfig<Boolean> invertedCrosshair = CLIENT_STORAGE.register(createBoolean("invertedCrosshair",false));
    public static final FactoryConfig<Boolean> legacyDrownedAnimation = CLIENT_STORAGE.register(createBoolean("legacyDrownedAnimation",true));
    public static final FactoryConfig<Boolean> merchantTradingIndicator = CLIENT_STORAGE.register(createBoolean("merchantTradingIndicator",true));
    public static final FactoryConfig<Boolean> itemLightingInHand = CLIENT_STORAGE.register(createBoolean("itemLightingInHand",true));
    public static final FactoryConfig<Boolean> loyaltyLines = CLIENT_STORAGE.register(createBoolean("loyaltyLines",true));
    public static final FactoryConfig<Boolean> controllerToggleCrouch = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleCrouch", FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleSneak")), true, b -> {}, CLIENT_STORAGE));;
    public static final FactoryConfig<Boolean> controllerToggleSprint = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleSprint", FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleSprint")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> controllerToggleUse = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleUse", FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleUse")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> controllerToggleAttack = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerToggleAttack", FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleAttack")), false, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> lockControlTypeChange = CLIENT_STORAGE.register(createBoolean("lockControlTypeChange",false));
    public static final FactoryConfig<Integer> selectedItemTooltipLines = CLIENT_STORAGE.register(createInteger("selectedItemTooltipLines", Function.identity(), 0, () -> 6, 4));
    public static final FactoryConfig<Boolean> itemTooltipEllipsis = CLIENT_STORAGE.register(createBoolean("itemTooltipEllipsis",true));
    public static final FactoryConfig<Integer> selectedItemTooltipSpacing = CLIENT_STORAGE.register(createInteger("selectedItemTooltipSpacing", Function.identity(), 8, () -> 12, 12));
    public static final FactoryConfig<VehicleCameraRotation> vehicleCameraRotation = CLIENT_STORAGE.register(create("vehicleCameraRotation", builder -> builder.valueToComponent(v -> v.displayName), i -> VehicleCameraRotation.values()[i], VehicleCameraRotation::ordinal, ()->VehicleCameraRotation.values().length, VehicleCameraRotation.CODEC, VehicleCameraRotation.ONLY_NON_LIVING_ENTITIES, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> defaultParticlePhysics = CLIENT_STORAGE.register(createBoolean("defaultParticlePhysics", true));
    public static final FactoryConfig<Boolean> linearCameraMovement = CLIENT_STORAGE.register(createBoolean("linearCameraMovement", false));
    public static final FactoryConfig<Boolean> legacyOverstackedItems = CLIENT_STORAGE.register(createBoolean("legacyOverstackedItems", true));
    public static final FactoryConfig<Boolean> displayMultipleControlsFromAction = CLIENT_STORAGE.register(createBoolean("displayMultipleControlsFromAction", false));
    public static final FactoryConfig<Boolean> enhancedPistonMovingRenderer = CLIENT_STORAGE.register(createBoolean("enhancedPistonMovingRenderer", true));
    public static final FactoryConfig<Boolean> legacyEntityFireTint = CLIENT_STORAGE.register(createBoolean("legacyEntityFireTint", true));
    public static final FactoryConfig<Boolean> advancedHeldItemTooltip = CLIENT_STORAGE.register(createBoolean("advancedHeldItemTooltip", false));
    public static final FactoryConfig<AdvancedOptionsMode> advancedOptionsMode = CLIENT_STORAGE.register(create("advancedOptionsMode", builder -> builder.valueToComponent(v -> v.displayName), i -> AdvancedOptionsMode.values()[i], AdvancedOptionsMode::ordinal, () -> AdvancedOptionsMode.values().length, AdvancedOptionsMode.CODEC, AdvancedOptionsMode.DEFAULT, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> saveCache = CLIENT_STORAGE.register(createBoolean("saveCache", true));
    public static final FactoryConfig<Boolean> autoSaveCountdown = CLIENT_STORAGE.register(createBoolean("autoSaveCountdown", false));
    public static final FactoryConfig<Boolean> displayControlTooltips = CLIENT_STORAGE.register(createBoolean("displayControlTooltips", true));
    public static final FactoryConfig<Boolean> systemMessagesAsOverlay = CLIENT_STORAGE.register(createBoolean("systemMessagesAsOverlay", true));
    public static final FactoryConfig<Boolean> forceMixedCrafting = CLIENT_STORAGE.register(createBoolean("forceMixedCrafting", false, b -> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(PlayerInfoSync.classicCrafting(classicCrafting.get() || b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicTrading = CLIENT_STORAGE.register(createBoolean("classicTrading",false, b -> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(PlayerInfoSync.classicTrading(b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicStonecutting = CLIENT_STORAGE.register(createBoolean("classicStonecutting",false, b -> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(PlayerInfoSync.classicStonecutting(b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> classicLoom = CLIENT_STORAGE.register(createBoolean("classicLoom",false, b -> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(PlayerInfoSync.classicLoom(b, Minecraft.getInstance().player));
    }));
    public static final FactoryConfig<Boolean> headFollowsTheCamera = CLIENT_STORAGE.register(createBoolean("headFollowsTheCamera", true));
    public static final FactoryConfig<Boolean> fastLeavesWhenBlocked = CLIENT_STORAGE.register(createBoolean("fastLeavesWhenBlocked", true, b -> Legacy4JClient.updateChunks()));
    public static final FactoryConfig<Boolean> invertedFrontCameraPitch = CLIENT_STORAGE.register(createBoolean("invertedFrontCameraPitch", true, b -> {}));
    public static final FactoryConfig<Boolean> legacySkyShape = CLIENT_STORAGE.register(createBoolean("legacySkyShape", true, b -> Legacy4JClient.updateSkyShape()));
    public static final FactoryConfig<Boolean> fastLeavesCustomModels = CLIENT_STORAGE.register(createBoolean("fastLeavesCustomModels", true, b -> Legacy4JClient.updateChunks()));
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
    public static final FactoryConfig<Boolean> cursorAtFirstInventorySlot = CLIENT_STORAGE.register(createBoolean("cursorAtFirstInventorySlot", false));
    public static final FactoryConfig<Boolean> controllerCursorAtFirstInventorySlot = CLIENT_STORAGE.register(FactoryConfig.createBoolean("controllerCursorAtFirstInventorySlot", FactoryConfigDisplay.createToggle(Component.translatable("legacy.options.cursorAtFirstInventorySlot")),true, b -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> systemCursor = CLIENT_STORAGE.register(createBoolean("systemCursor", false, b -> Legacy4JClient.controllerManager.updateCursorInputMode()));
    public static final FactoryConfig<ControlTooltipDisplay> controlTooltipDisplay = CLIENT_STORAGE.register(create("controlTooltipDisplay", builder -> builder.valueToComponent(v -> v.displayName), i -> ControlTooltipDisplay.values()[i], ControlTooltipDisplay::ordinal, () -> ControlTooltipDisplay.values().length, ControlTooltipDisplay.CODEC, ControlTooltipDisplay.AUTO, d -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> legacyLoadingAndConnecting = CLIENT_STORAGE.register(createBoolean("legacyLoadingAndConnecting", true));
    public static final FactoryConfig<Boolean> unbindConflictingKeys = CLIENT_STORAGE.register(createBoolean("unbindConflictingKeys", true));
    public static final FactoryConfig<Boolean> unbindConflictingButtons = CLIENT_STORAGE.register(createBoolean("unbindConflictingButtons", true));
    public static final FactoryConfig<Integer> hudDelay = CLIENT_STORAGE.register(createInteger("hudDelay", builder -> builder.messageFunction((display, value) -> value == 0 ? CommonComponents.optionNameValue(display.name(), CommonComponents.OPTION_OFF) : Component.translatable("options.percent_value", display.name(), value)), 0, () -> 200, 100));
    public static final FactoryConfig<Boolean> legacyBabyVillagerHead = CLIENT_STORAGE.register(createBoolean("legacyBabyVillagerHead", true));
    public static final FactoryConfig<Boolean> bubblesOutsideWater = CLIENT_STORAGE.register(createBoolean("bubblesOutsideWater", true));
    public static final FactoryConfig<Boolean> legacyItemPickup = CLIENT_STORAGE.register(createBoolean("legacyItemPickup", true));
    public static final FactoryConfig<Boolean> controllerToasts = CLIENT_STORAGE.register(createBoolean("controllerToasts", true));
    public static final FactoryConfig<Boolean> legacyHearts = CLIENT_STORAGE.register(createBoolean("legacyHearts", true));
    public static final FactoryConfig<Boolean> controllerDoubleClick = CLIENT_STORAGE.register(createBoolean("controllerDoubleClick", false));
    public static final FactoryConfig<Boolean> inventoryHoverFocusSound = CLIENT_STORAGE.register(createBoolean("inventoryHoverFocusSound", false));
    public static final FactoryConfig<Boolean> legacyCursor = CLIENT_STORAGE.register(createBoolean("legacyCursor", true));
    public static final FactoryConfig<Boolean> limitCursor = CLIENT_STORAGE.register(createBoolean("limitCursor", true));
    public static final FactoryConfig<Boolean> enhancedItemTranslucency = CLIENT_STORAGE.register(createBoolean("enhancedItemTranslucency", false));
    public static final FactoryConfig<Boolean> legacyFireworks = CLIENT_STORAGE.register(createBoolean("legacyFireworks", true));
    public static final FactoryConfig<UIMode> uiMode = CLIENT_STORAGE.register(create("uiMode", builder -> builder.valueToComponent(v -> v.displayName), i -> UIMode.values()[i], UIMode::ordinal, () -> UIMode.values().length, UIMode.CODEC, UIMode.AUTO, d -> Minecraft.getInstance().execute(Minecraft.getInstance()::resizeDisplay), CLIENT_STORAGE));
    public static final FactoryConfig<OptionHolder<OptionsPreset>> optionsPreset = CLIENT_STORAGE.register(FactoryConfig.create("optionsPreset", FactoryConfigDisplay.<OptionHolder<OptionsPreset>>builder().valueToComponent(i -> i.isNone() ? LegacyComponents.NONE : i.get().isApplied() ? i.get().nameOrEmpty() : Component.translatable("legacy.options.not_applied_value", i.get().nameOrEmpty())).tooltip(holder -> holder.isNone() ? null : holder.get().tooltip().orElse(null)).build(optionName("optionsPreset")), new FactoryConfigControl.FromInt<>(OptionsPreset.OPTION_CODEC, i -> i == 0 || Legacy4JClient.optionPresetsManager.map().size() < i ? OptionHolder.none() : OptionHolder.of(Legacy4JClient.optionPresetsManager.map().getByIndex(i - 1)), s1 -> 1 + Legacy4JClient.optionPresetsManager.map().indexOf(s1.get()), () -> Legacy4JClient.optionPresetsManager.map().size() + 1), OptionHolder.none(), v -> {}, CLIENT_STORAGE));
    public static final FactoryConfig<Boolean> fakeAutosaveScreen = CLIENT_STORAGE.register(createBoolean("fakeAutosaveScreen", false));
    public static final FactoryConfig<Boolean> legacyPotionsBar = CLIENT_STORAGE.register(createBoolean("legacyPotionsBar", false));
    public static final FactoryConfig<Boolean> defaultShowCraftableRecipes = CLIENT_STORAGE.register(createBoolean("defaultShowCraftableRecipes", false));

    public static int getTerrainFogStart() {
        return Math.min(terrainFogStart.get(), Minecraft.getInstance().options.renderDistance().get());
    }

    public static boolean hasSystemCursor() {
        return systemCursor.get() && !Legacy4JClient.controllerManager.isControllerTheLastInput();
    }

    public static float getLeftStickDeadZone() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null && minecraft.player != null && minecraft.player.getControlledVehicle() instanceof /*? if <1.21.2 {*//*Boat*//*?} else {*/AbstractBoat/*?}*/ ? 0.5f + leftStickDeadZone.get().floatValue() / 2 : leftStickDeadZone.get().floatValue();
    }

    public static boolean hasClassicCrafting() {
        return classicCrafting.get();
    }

    public static boolean hasMixedCrafting() {
        return (forceMixedCrafting.get() || !Legacy4JClient.hasModOnServer()) && !classicCrafting.get();
    }

    public static UIMode getUIMode() {
        if (uiMode.get() == UIMode.AUTO) {
            if (LegacyRenderUtil.getStandardHeight() == 1080) return UIMode.FHD;
            return LegacyRenderUtil.getStandardHeight() > 540 ? UIMode.HD : UIMode.SD;
        }

        return uiMode.get();
    }

    public enum VehicleCameraRotation implements StringRepresentable {
        NONE("none", LegacyComponents.NONE),ALL_ENTITIES("all_entities"),ONLY_NON_LIVING_ENTITIES("only_non_living_entities"),ONLY_LIVING_ENTITIES("only_living_entities");
        public static final EnumCodec<VehicleCameraRotation> CODEC = StringRepresentable.fromEnum(VehicleCameraRotation::values);
        private final String name;
        public final Component displayName;

        VehicleCameraRotation(String name, Component displayName) {
            this.name = name;
            this.displayName = displayName;
        }
        VehicleCameraRotation(String name) {
            this(name,Component.translatable("legacy.options.vehicleCameraRotation."+name));
        }
        public boolean isForLivingEntities() {
            return this == ALL_ENTITIES || this == ONLY_LIVING_ENTITIES;
        }
        public boolean isForNonLivingEntities() {
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

        AdvancedOptionsMode(String name, Component displayName) {
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

        CursorMode(String name, Component displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        CursorMode(String name) {
            this(name, Component.translatable("legacy.options.cursorMode."+name));
        }

        public boolean isAuto() {
            return this == AUTO;
        }

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isNever() {
            return this == NEVER;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum UIMode implements StringRepresentable {
        AUTO("auto"),FHD("fhd"),HD("hd"), SD("sd");
        public static final EnumCodec<UIMode> CODEC = StringRepresentable.fromEnum(UIMode::values);
        private final String name;
        public final Component displayName;

        UIMode(String name, Component displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        UIMode(String name) {
            this(name, Component.translatable("legacy.options.uiMode."+name));
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

    public enum ControlTooltipDisplay implements StringRepresentable {
        AUTO("auto"),LEFT("left", HumanoidArm.LEFT.getCaption()),RIGHT("right", HumanoidArm.RIGHT.getCaption());
        public static final EnumCodec<ControlTooltipDisplay> CODEC = StringRepresentable.fromEnum(ControlTooltipDisplay::values);
        private final String name;
        public final Component displayName;

        ControlTooltipDisplay(String name, Component displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        ControlTooltipDisplay(String name) {
            this(name, Component.translatable("legacy.options.controlTooltipDisplay."+name));
        }

        public boolean isRight() {
            return this == RIGHT || this == AUTO && Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT;
        }

        public boolean isLeft() {
            return this == LEFT || this == AUTO && Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
