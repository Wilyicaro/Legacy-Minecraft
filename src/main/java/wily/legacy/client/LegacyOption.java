package wily.legacy.client;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.*;
import wily.legacy.client.screen.LegacyKeyBindsScreen;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.TopMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minecraft.client.Options.genericValueLabel;

public interface LegacyOption<T> {
    File legacyOptionsFile = FactoryAPIPlatform.getConfigDirectory().resolve("legacy_options.txt").toFile();
    Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);

    List<LegacyOption<?>> list = new ArrayList<>();
    Gson GSON = new GsonBuilder().create();

    OptionInstance<T> getInstance();
    T defaultValue();

    default void reset(){
        getInstance().set(defaultValue());
    }

    void process(Options.FieldAccess access);

    static <T> OptionInstance<T> register(LegacyOption<T> option){
        list.add(option);
        return option.getInstance();
    }
    static <T> LegacyOption<T> create(String name, Function<String,OptionInstance<T>> optionInstance){
        return create(name,optionInstance.apply("legacy.options."+name));
    }
    static <T> LegacyOption<T> create(String name, Function<String,OptionInstance<T>> optionInstance,BiConsumer<LegacyOption<T>,Options.FieldAccess> process){
        return create(optionInstance.apply("legacy.options."+name),process);
    }
    static <T> LegacyOption<T> create(String name, OptionInstance<T> optionInstance){
        return create(optionInstance,(o,a)->a.process(name,o.getInstance()));
    }
    static <T> LegacyOption<T> create(OptionInstance<T> optionInstance, BiConsumer<LegacyOption<T>,Options.FieldAccess> process){
        T defaultValue = optionInstance.get();
        return new LegacyOption<>() {
            @Override
            public OptionInstance<T> getInstance() {
                return optionInstance;
            }

            @Override
            public T defaultValue() {
                return defaultValue;
            }

            @Override
            public void process(Options.FieldAccess access) {
                process.accept(this, access);
            }
        };
    }
    static Component percentValueLabel(Component component, double d) {
        return Component.translatable("options.percent_value", component, (int)(d * 100.0));
    }
    OptionInstance<Boolean> animatedCharacter = register(create("animatedCharacter",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> classicCrafting = register(create("classicCrafting",s->OptionInstance.createBoolean(s,false, b-> {
        if (Minecraft.getInstance().player != null) CommonNetwork.sendToServer(new PlayerInfoSync(b ? 1 : 2, Minecraft.getInstance().player));
    })));
    OptionInstance<Boolean> vanillaTabs = register(create("vanillaTabs",s->OptionInstance.createBoolean(s,OptionInstance.cachedConstantTooltip(Component.translatable("legacy.options.vanillaTabs.description")),false)));
    OptionInstance<Boolean> displayLegacyGamma = register(create("displayGamma",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Double> legacyGamma = register(create("gamma",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {})));
    OptionInstance<Boolean> displayHUD = register(create("displayHUD",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> displayHand = register(create("displayHand",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> legacyCreativeTab = register(create("creativeTab",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> searchCreativeTab = register(create("searchCreativeTab",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Integer> autoSaveInterval = register(create("autoSaveInterval",s->new OptionInstance<>(s, OptionInstance.noTooltip(),(c,i)-> i == 0 ? genericValueLabel(c,Component.translatable("options.off")) :Component.translatable( "legacy.options.mins_value",c, i * 5),new OptionInstance.IntRange(0,24),1, i->{/*? if >1.20.1 {*/if (Minecraft.getInstance().hasSingleplayerServer()) Minecraft.getInstance().getSingleplayerServer().onTickRateChanged();/*?}*/})));
    OptionInstance<Boolean> autoSaveWhenPaused = register(create("autoSaveWhenPaused",s->OptionInstance.createBoolean(s,false)));
    OptionInstance<Boolean> inGameTooltips = register(create("gameTooltips",s->OptionInstance.createBoolean("legacy.options.gameTooltips", true)));
    OptionInstance<Boolean> tooltipBoxes = register(create("tooltipBoxes",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> hints = register(create("hints",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> flyingViewRolling = register(create("flyingViewRolling",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> directSaveLoad = register(create("directSaveLoad",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Boolean> vignette = register(create("vignette",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Boolean> minecartSounds = register(create("minecartSounds",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> caveSounds = register(create("caveSounds",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> showVanillaRecipeBook = register(create("showVanillaRecipeBook",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Boolean> displayNameTagBorder = register(create("displayNameTagBorder",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> legacyItemTooltips = register(create("legacyItemTooltips",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> legacyItemTooltipScaling = register(create("legacyItemTooltipsScaling",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> invertYController = register(create("invertYController",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Boolean> invertControllerButtons = register(create("invertControllerButtons",s->OptionInstance.createBoolean("legacy.options.invertControllerButtons", false, (b)-> ControllerBinding.RIGHT_BUTTON.bindingState.block(2))));
    OptionInstance<Integer> selectedController = register(create("selectedController",s->new OptionInstance<>(s, OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.literal(i+1 + (Legacy4JClient.controllerManager.connectedController == null ? "" : " (%s)".formatted(Legacy4JClient.controllerManager.connectedController.getName())))),  new OptionInstance.IntRange(0, 15), 0, d -> { if (Legacy4JClient.controllerManager.connectedController!= null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);})));
    OptionInstance<Controller.Handler> selectedControllerHandler =  register(create("selectedControllerHandler", s-> new OptionInstance<>(s, OptionInstance.noTooltip(), (c, h)-> Component.translatable("options.generic_value",c,h.getName()), new OptionInstance.IntRange(0, ControllerManager.handlers.size() - 1).xmap(((List<Controller.Handler>)ControllerManager.handlers.values())::get,((List<Controller.Handler>)ControllerManager.handlers.values())::indexOf), SDLControllerHandler.getInstance(), d-> {
        ControllerBinding.LEFT_STICK.bindingState.block(2);
        if (Legacy4JClient.controllerManager.connectedController != null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);
    }),(o,a)->o.getInstance().set(a.process("selectedControllerHandler", o.getInstance().get(), ControllerManager.handlers::get, ControllerManager.handlers::getKey))));
    OptionInstance<Integer> cursorMode = register(create("cursorMode",s->new OptionInstance<>(s, OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.translatable(i == 0 ? "options.guiScale.auto" : i == 1 ? "team.visibility.always" : "team.visibility.never")), new OptionInstance.IntRange(0, 2), 0, d -> {})));
    OptionInstance<Boolean> unfocusedInputs = register(create("unfocusedInputs",s->OptionInstance.createBoolean(s, false)));
    OptionInstance<Double> leftStickDeadZone = register(create("leftStickDeadZone",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.25, d -> {})));
    OptionInstance<Double> rightStickDeadZone = register(create("rightStickDeadZone",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.34, d -> {})));
    OptionInstance<Double> leftTriggerDeadZone = register(create("leftTriggerDeadZone",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.2, d -> {})));
    OptionInstance<Double> rightTriggerDeadZone = register(create("rightTriggerDeadZone",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.2, d -> {})));
    OptionInstance<Integer> hudScale = register(create("hudScale",s->new OptionInstance<>(s, OptionInstance.noTooltip(), Options::genericValueLabel,  new OptionInstance.IntRange(1,3), 2, d -> {})));
    OptionInstance<Double> hudOpacity = register(create("hudOpacity",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.8, d -> {})));
    OptionInstance<Double> hudDistance = register(create("hudDistance",s->new OptionInstance<>(s, OptionInstance.noTooltip(), LegacyOption::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {})));
    OptionInstance<Double> interfaceResolution = register(create("interfaceResolution",s->new OptionInstance<>(s, OptionInstance.noTooltip(), (c, d) -> percentValueLabel(c, 0.25 + d * 1.5), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> Minecraft.getInstance().resizeDisplay())));
    OptionInstance<Double> interfaceSensitivity = register(create("interfaceSensitivity",s->new OptionInstance<>(s, OptionInstance.noTooltip(), (c, d) -> percentValueLabel(c, d*2), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {})));
    OptionInstance<Double> controllerSensitivity = register(create("controllerSensitivity",new OptionInstance<>("options.sensitivity", OptionInstance.noTooltip(), (c, d) -> percentValueLabel(c, d*2), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {})));
    OptionInstance<Boolean> overrideTerrainFogStart = register(create("overrideTerrainFogStart",s->OptionInstance.createBoolean(s, true)));
    OptionInstance<Integer> terrainFogStart = register(create("terrainFogStart",s->new OptionInstance<>(s, OptionInstance.noTooltip(),(c,i)-> Component.translatable("options.chunks", i), new OptionInstance.ClampingLazyMaxIntRange(2, () -> Minecraft.getInstance().options.renderDistance().get(), 0x7FFFFFFE), 4, d -> {})));
    OptionInstance<Double> terrainFogEnd = register(create("terrainFogEnd",s->new OptionInstance<>(s, OptionInstance.noTooltip(),(c, d) -> percentValueLabel(c, d*2), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {})));
    OptionInstance<String> selectedControlType = register(create("controlType", s->new OptionInstance<>(s, OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,i.equals("auto")? Component.translatable("legacy.options.auto_value", ControlType.getActiveType().getDisplayName()) : ControlType.typesMap.get(i).getDisplayName()), new OptionInstance.ClampingLazyMaxIntRange(0, ControlType.types::size,Integer.MAX_VALUE).xmap(i-> i == 0 ? "auto" : ControlType.types.get(i - 1).getId().toString(), s1-> s1.equals("auto") ? 0 : (1 + ControlType.types.indexOf(ControlType.typesMap.get(s1)))), "auto", d -> {}),(o, a)-> FactoryAPIClient.SECURE_EXECUTOR.executeNowIfPossible(()->o.getInstance().set(a.process("controllerIcons", o.getInstance().get())), MinecraftAccessor.getInstance()::hasGameLoaded)));
    OptionInstance<Difficulty> createWorldDifficulty = register(create("createWorldDifficulty",new OptionInstance<>("options.difficulty", d->Tooltip.create(d.getInfo()), (c, d) -> d.getDisplayName(), new OptionInstance.Enum<>(Arrays.asList(Difficulty.values()), Codec.INT.xmap(Difficulty::byId, Difficulty::getId)), Difficulty.NORMAL, d -> {})));
    OptionInstance<Boolean> smoothMovement = register(create("smoothMovement",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> legacyCreativeBlockPlacing = register(create("legacyCreativeBlockPlacing",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> smoothAnimatedCharacter = register(create("smoothAnimatedCharacter",s->OptionInstance.createBoolean(s,false)));
    OptionInstance<Boolean> autoResolution = register(create("autoResolution",s->OptionInstance.createBoolean(s, true, b -> Minecraft.getInstance().resizeDisplay())));
    OptionInstance<Boolean> invertedCrosshair = register(create("invertedCrosshair",s->OptionInstance.createBoolean(s,false)));
    OptionInstance<Boolean> legacyDrownedAnimation = register(create("legacyDrownedAnimation",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> merchantTradingIndicator = register(create("merchantTradingIndicator",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> itemLightingInHand = register(create("itemLightingInHand",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> loyaltyLines = register(create("loyaltyLines",s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Boolean> controllerToggleCrouch = register(create("controllerToggleCrouch", OptionInstance.createBoolean("options.key.toggleSneak",true)));
    OptionInstance<Boolean> controllerToggleSprint = register(create("controllerToggleSprint",OptionInstance.createBoolean("options.key.toggleSprint",false)));
    OptionInstance<Boolean> lockControlTypeChange = register(create("lockControlTypeChange", s->OptionInstance.createBoolean(s,false)));
    OptionInstance<Integer> selectedItemTooltipLines = register(create("selectedItemTooltipLines", s->new OptionInstance<>(s, OptionInstance.noTooltip(), Options::genericValueLabel, new OptionInstance.IntRange(0,6), 4, d -> {})));
    OptionInstance<Boolean> itemTooltipEllipsis = register(create("itemTooltipEllipsis", s->OptionInstance.createBoolean(s,true)));
    OptionInstance<Integer> selectedItemTooltipSpacing = register(create("selectedItemTooltipSpacing", s->new OptionInstance<>(s, OptionInstance.noTooltip(), Options::genericValueLabel, new OptionInstance.IntRange(8,12), 12, d -> {})));
    OptionInstance<VehicleCameraRotation> vehicleCameraRotation = register(create("vehicleCameraRotation",s->new OptionInstance<>(s, d->null, (c, d) -> d.displayName, new OptionInstance.Enum<>(Arrays.asList(VehicleCameraRotation.values()), Codec.INT.xmap(i-> VehicleCameraRotation.values()[i], VehicleCameraRotation::ordinal)), VehicleCameraRotation.ONLY_NON_LIVING_ENTITIES, d -> {})));
    OptionInstance<Boolean> defaultParticlePhysics = register(create("defaultParticlePhysics",s-> OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> linearCameraMovement = register(create("linearCameraMovement", s-> OptionInstance.createBoolean(s, false)));
    OptionInstance<Boolean> legacyOverstackedItems = register(create("legacyOverstackedItems", s-> OptionInstance.createBoolean(s, true)));
    OptionInstance<Boolean> displayMultipleControlsFromAction = register(create("displayMultipleControlsFromAction", s-> OptionInstance.createBoolean(s, false)));

    enum VehicleCameraRotation implements StringRepresentable {
        NONE("none", LegacyKeyBindsScreen.NONE),ALL_ENTITIES("all_entities"),ONLY_NON_LIVING_ENTITIES("only_non_living_entities"),ONLY_LIVING_ENTITIES("only_living_entities");
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

    static void saveAll(){
        saveAll(legacyOptionsFile);
    }
    static void saveAll(File optionsFile){
        try {
            final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(optionsFile), StandardCharsets.UTF_8));
            try {
                printWriter.println("lastLoadedVersion:" + Legacy4J.VERSION.get());
                processLegacyOptions(
                        new Options.FieldAccess() {
                            public void writePrefix(String string) {
                                printWriter.print(string);
                                printWriter.print(':');
                            }

                            @Override
                            public <T> void process(String string, OptionInstance<T> optionInstance) {
                                DataResult<JsonElement> dataResult = optionInstance.codec().encodeStart(JsonOps.INSTANCE, optionInstance.get());
                                dataResult.error().ifPresent((partialResult) -> Legacy4J.LOGGER.error("Error saving option " + optionInstance + ": " + partialResult));
                                dataResult.result().ifPresent((jsonElement) -> {
                                    this.writePrefix(string);
                                    printWriter.println(GSON.toJson(jsonElement));
                                });
                            }

                            @Override
                            public int process(String string, int i) {
                                this.writePrefix(string);
                                printWriter.println(i);
                                return i;
                            }

                            @Override
                            public boolean process(String string, boolean bl) {
                                this.writePrefix(string);
                                printWriter.println(bl);
                                return bl;
                            }

                            @Override
                            public String process(String string, String string2) {
                                this.writePrefix(string);
                                printWriter.println(string2);
                                return string2;
                            }

                            @Override
                            public float process(String string, float f) {
                                this.writePrefix(string);
                                printWriter.println(f);
                                return f;
                            }

                            @Override
                            public <T> T process(String string, T object, Function<String, T> function, Function<T, String> function2) {
                                this.writePrefix(string);
                                printWriter.println(function2.apply(object));
                                return object;
                            }
                        }
                );
            } catch (Throwable var5) {
                try {
                    printWriter.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }

                throw var5;
            }

            printWriter.close();
        } catch (Exception var6) {
            Legacy4J.LOGGER.error("Failed to save options", var6);
        }

    }

    static void loadAll(){
        loadAll(legacyOptionsFile);
    }
    static void loadAll(File optionsFile){
        if (!optionsFile.exists()) return;
        try {
            CompoundTag compoundTag = new CompoundTag();
            BufferedReader bufferedReader = Files.newReader(optionsFile, Charsets.UTF_8);

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
            Legacy4JClient.lastLoadedVersion = compoundTag.getString("lastLoadedVersion");

            bufferedReader.close();
            processLegacyOptions(new Options.FieldAccess() {
                @Nullable
                private String getValueOrNull(String string) {
                    return compoundTag.contains(string) ? compoundTag.get(string).getAsString() : null;
                }

                @Override
                public <T> void process(String string, OptionInstance<T> optionInstance) {
                    String string2 = this.getValueOrNull(string);
                    if (string2 != null) {
                        JsonReader jsonReader = new JsonReader(new StringReader(string2.isEmpty() ? "\"\"" : string2));
                        JsonElement jsonElement = JsonParser.parseReader(jsonReader);
                        DataResult<T> dataResult = optionInstance.codec().parse(JsonOps.INSTANCE, jsonElement);
                        dataResult.error()
                                .ifPresent(error -> Legacy4J.LOGGER.error("Error parsing option value " + string2 + " for option " + optionInstance + ": " + error.message()));
                        dataResult.result().ifPresent(optionInstance::set);
                    }
                }

                @Override
                public int process(String string, int i) {
                    String string2 = this.getValueOrNull(string);
                    if (string2 != null) {
                        try {
                            return Integer.parseInt(string2);
                        } catch (NumberFormatException var5) {
                            Legacy4J.LOGGER.warn("Invalid integer value for option {} = {}", string, string2, var5);
                        }
                    }

                    return i;
                }

                @Override
                public boolean process(String string, boolean bl) {
                    String string2 = this.getValueOrNull(string);
                    return string2 != null ? string2.equals("true") : bl;
                }

                @Override
                public String process(String string, String string2) {
                    return MoreObjects.firstNonNull(this.getValueOrNull(string), string2);
                }

                @Override
                public float process(String string, float f) {
                    String string2 = this.getValueOrNull(string);
                    if (string2 == null) {
                        return f;
                    } else if (string2.equals("true")) {
                        return 1.0F;
                    } else if (string2.equals("false")) {
                        return 0.0F;
                    } else {
                        try {
                            return Float.parseFloat(string2);
                        } catch (NumberFormatException var5) {
                            Legacy4J.LOGGER.warn("Invalid floating point value for option {} = {}", string, string2, var5);
                            return f;
                        }
                    }
                }

                @Override
                public <T> T process(String string, T object, Function<String, T> function, Function<T, String> function2) {
                    String string2 = this.getValueOrNull(string);
                    return string2 == null ? object : function.apply(string2);
                }
            });
        } catch (IOException e) {
            Legacy4J.LOGGER.error("Failed to load options", e);
        }
    }
    static void processLegacyOptions(Options.FieldAccess fieldAccess){
        list.forEach(o->o.process(fieldAccess));

        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            LegacyKeyMapping mapping = (LegacyKeyMapping) keyMapping;
            ControllerBinding binding = fieldAccess.process("component_" + keyMapping.getName(), mapping.getBinding(), s-> {
                Integer oldOrdinal;
                try {
                    oldOrdinal = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    oldOrdinal = null;
                }
                return oldOrdinal != null ? oldOrdinal < 0 || oldOrdinal > ControllerBinding.values().length ? null : ControllerBinding.values()[oldOrdinal] : s.equals("none") ? null : ControllerBinding.CODEC.byName(s);
            }, b-> b == null ? "none" : b.getSerializedName());
            if (mapping.getBinding() != binding) {
                mapping.setBinding(binding != null && !binding.isBindable ? mapping.getDefaultBinding() : binding);
            }
        }
    }
}
