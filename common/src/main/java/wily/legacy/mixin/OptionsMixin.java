package wily.legacy.mixin;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.gson.Gson;
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
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.Assort;
import wily.legacy.network.CommonNetwork;
import wily.legacy.network.PlayerInfoSync;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(Options.class)
public abstract class OptionsMixin implements LegacyOptions {

    @Shadow protected Minecraft minecraft;

    @Shadow public abstract void load();


    @Shadow
    public static Component genericValueLabel(Component arg, int i) {
        return null;
    }

    @Shadow
    protected static Component percentValueLabel(Component component, double d) {
        return null;
    }

    @Shadow @Final public KeyMapping[] keyMappings;

    @Shadow public abstract OptionInstance<Integer> renderDistance();

    @Shadow
    public static Component genericValueLabel(Component arg, Component arg2) {
        return null;
    }

    @Shadow protected abstract void processOptions(Options.FieldAccess arg);

    @Shadow @Final private static Splitter OPTION_SPLITTER;
    @Shadow @Final private static Gson GSON;
    @Shadow @Final private File optionsFile;
    private OptionInstance<Double> hudDistance;
    private OptionInstance<Double> hudOpacity;
    private OptionInstance<Double> interfaceResolution;
    private OptionInstance<Double> interfaceSensitivity;
    private OptionInstance<Integer> terrainFogStart;
    private OptionInstance<Double> terrainFogEnd;
    private OptionInstance<Boolean> overrideTerrainFogStart;
    private OptionInstance<Boolean> legacyCreativeTab;
    private OptionInstance<Boolean> searchCreativeTab;
    private OptionInstance<Boolean> displayHUD;
    private OptionInstance<Boolean> displayHand;
    private OptionInstance<Boolean> animatedCharacter;
    private OptionInstance<Boolean> classicCrafting;
    private OptionInstance<Boolean> vanillaTabs;
    private OptionInstance<Integer> autoSaveInterval;
    private OptionInstance<Boolean> autoSaveWhenPaused;
    private OptionInstance<Integer> hudScale;
    private OptionInstance<Boolean> showVanillaRecipeBook;
    private OptionInstance<Boolean> displayLegacyGamma;
    private OptionInstance<Double> legacyGamma;
    private OptionInstance<Boolean> inGameTooltips;
    private OptionInstance<Boolean> tooltipBoxes;
    private OptionInstance<Boolean> hints;
    private OptionInstance<Boolean> flyingViewRolling;
    private OptionInstance<Boolean> directSaveLoad;
    private OptionInstance<Boolean> vignette;
    private OptionInstance<Boolean> displayNameTagBorder;
    private OptionInstance<Boolean> legacyItemTooltips;
    private OptionInstance<Boolean> caveSounds;
    private OptionInstance<Boolean> minecartSounds;
    private OptionInstance<Boolean> invertYController;
    private OptionInstance<Boolean> invertControllerButtons;
    private OptionInstance<Double> leftStickDeadZone;
    private OptionInstance<Double> rightStickDeadZone;
    private OptionInstance<Double> leftTriggerDeadZone;
    private OptionInstance<Double> rightTriggerDeadZone;
    private OptionInstance<Integer> selectedController;
    private OptionInstance<String> selectedControlIcons;
    private OptionInstance<Boolean> smoothMovement;
    private OptionInstance<Boolean> legacyCreativeBlockPlacing;
    private OptionInstance<Integer> cursorMode;
    private OptionInstance<Boolean> controllerVirtualCursor;
    private OptionInstance<Boolean> unfocusedInputs;
    private OptionInstance<Difficulty> createWorldDifficulty;
    private OptionInstance<Boolean> smoothAnimatedCharacter;
    private OptionInstance<Integer> selectedControllerHandler;
    private OptionInstance<Boolean> autoResolution;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;)V", ordinal = 5),index = 0)
    protected String initKeyCraftingName(String string) {
        return "legacy.key.inventory";
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;)V", ordinal = 5),index = 1)
    protected int initKeyCrafting(int i) {
        return 73;
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Ljava/lang/Object;Ljava/util/function/Consumer;)V", ordinal = 6),index = 4)
    protected Object initChatSpacingOption(Object object) {
        return 1.0d;
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ToggleKeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/BooleanSupplier;)V", ordinal = 0),index = 3)
    protected BooleanSupplier initKeyShift(BooleanSupplier booleanSupplier) {
        return ()-> (minecraft == null || minecraft.player == null || (!minecraft.player.getAbilities().flying && minecraft.player.getVehicle() == null && (!minecraft.player.isInWater() || minecraft.player.onGround()))) && booleanSupplier.getAsBoolean();
    }
    @Redirect(method = "<init>", at = @At( value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    protected void init(Options instance) {

    }
    @Inject(method = "<init>", at = @At( "RETURN"))
    protected void init(Minecraft minecraft, File file, CallbackInfo ci) {
        animatedCharacter = OptionInstance.createBoolean("legacy.options.animatedCharacter",true);
        classicCrafting = OptionInstance.createBoolean("legacy.options.classicCrafting",false, b-> {
            if (minecraft.player != null) CommonNetwork.sendToServer(new PlayerInfoSync(b ? 1 : 2, minecraft.player));
        });
        vanillaTabs = OptionInstance.createBoolean("legacy.options.vanillaTabs",OptionInstance.cachedConstantTooltip(Component.translatable("legacy.options.vanillaTabs.description")),false);
        displayLegacyGamma = OptionInstance.createBoolean("legacy.options.displayGamma", true);
        legacyGamma = new OptionInstance<>("legacy.options.gamma", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {});
        displayHUD = OptionInstance.createBoolean("legacy.options.displayHud",true);
        displayHand = OptionInstance.createBoolean("legacy.options.displayHand",true);
        legacyCreativeTab = OptionInstance.createBoolean("legacy.options.creativeTab", true);
        searchCreativeTab = OptionInstance.createBoolean("legacy.options.searchCreativeTab", false);
        autoSaveInterval = new OptionInstance<>("legacy.options.autoSaveInterval", OptionInstance.noTooltip(),(c,i)-> i == 0 ? genericValueLabel(c,Component.translatable("options.off")) :Component.translatable( "legacy.options.mins_value",c, i * 5),new OptionInstance.IntRange(0,24),1, i->{});
        autoSaveWhenPaused = OptionInstance.createBoolean("legacy.options.autoSaveWhenPaused",false);
        inGameTooltips = OptionInstance.createBoolean("legacy.options.gameTooltips", true);
        tooltipBoxes = OptionInstance.createBoolean("legacy.options.tooltipBoxes", true);
        hints = OptionInstance.createBoolean("legacy.options.hints", true);
        flyingViewRolling = OptionInstance.createBoolean("legacy.options.flyingViewRolling", true);
        directSaveLoad = OptionInstance.createBoolean("legacy.options.directSaveLoad", false);
        vignette = OptionInstance.createBoolean("legacy.options.vignette", false);
        minecartSounds = OptionInstance.createBoolean("legacy.options.minecartSounds", true);
        caveSounds = OptionInstance.createBoolean("legacy.options.caveSounds", true);
        showVanillaRecipeBook = OptionInstance.createBoolean("legacy.options.showVanillaRecipeBook", false);
        displayNameTagBorder = OptionInstance.createBoolean("legacy.options.displayNameTagBorder", true);
        legacyItemTooltips = OptionInstance.createBoolean("legacy.options.legacyItemTooltips", true);
        invertYController = OptionInstance.createBoolean("legacy.options.invertYController", false);
        invertControllerButtons = OptionInstance.createBoolean("legacy.options.invertControllerButtons", false, (b)-> ControllerBinding.RIGHT_BUTTON.bindingState.block(2));
        selectedController = new OptionInstance<>("legacy.controls.controller", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.literal(i+1 + (Legacy4JClient.controllerManager.connectedController == null ? "" : " (%s)".formatted(Legacy4JClient.controllerManager.connectedController.getName())))),  new OptionInstance.IntRange(0, 15), 0, d -> { if (Legacy4JClient.controllerManager.connectedController!= null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);});
        selectedControllerHandler =  new OptionInstance<>("legacy.controls.controllerHandler", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.literal(ControllerManager.handlers.get(i).getName())), new OptionInstance.IntRange(0, ControllerManager.handlers.size() - 1), Minecraft.ON_OSX ? 0 : 1, d -> {
            ControllerBinding.LEFT_STICK.bindingState.block(2);
            if (Legacy4JClient.controllerManager.connectedController!= null) Legacy4JClient.controllerManager.connectedController.disconnect(Legacy4JClient.controllerManager);
            ControllerManager.getHandler().init();
        });
        cursorMode =  new OptionInstance<>("legacy.options.cursorMode", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.translatable(i == 0 ? "options.guiScale.auto" : i == 1 ? "team.visibility.always" : "team.visibility.never")), new OptionInstance.IntRange(0, 2), 0, d -> {});
        controllerVirtualCursor = OptionInstance.createBoolean("legacy.options.controllerVirtualCursor", false);
        unfocusedInputs = OptionInstance.createBoolean("legacy.options.unfocusedInputs", false);
        leftStickDeadZone = new OptionInstance<>("legacy.options.leftStickDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.25, d -> {});
        rightStickDeadZone = new OptionInstance<>("legacy.options.rightStickDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.34, d -> {});
        leftTriggerDeadZone = new OptionInstance<>("legacy.options.leftTriggerDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.2, d -> {});
        rightTriggerDeadZone = new OptionInstance<>("legacy.options.rightTriggerDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.2, d -> {});
        hudScale = new OptionInstance<>("legacy.options.hudScale", OptionInstance.noTooltip(), OptionsMixin::genericValueLabel,  new OptionInstance.IntRange(1,3), 2, d -> {});
        hudOpacity = new OptionInstance<>("legacy.options.hudOpacity", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.8, d -> {});
        hudDistance = new OptionInstance<>("legacy.options.hudDistance", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {});
        interfaceResolution = new OptionInstance<>("legacy.options.interfaceResolution", OptionInstance.noTooltip(), (c, d) -> percentValueLabel(c, 0.25 + d * 1.5), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> minecraft.resizeDisplay());
        interfaceSensitivity = new OptionInstance<>("legacy.options.interfaceSensitivity", OptionInstance.noTooltip(), (c, d) -> percentValueLabel(c, d*2), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {});
        overrideTerrainFogStart = OptionInstance.createBoolean("legacy.options.overrideTerrainFogStart", true);
        terrainFogStart = new OptionInstance<>("legacy.options.terrainFogStart", OptionInstance.noTooltip(),(c,i)-> Component.translatable("options.chunks", i), new OptionInstance.ClampingLazyMaxIntRange(2, () -> renderDistance().get(), 0x7FFFFFFE), 4, d -> {});
        terrainFogEnd = new OptionInstance<>("legacy.options.terrainFogEnd", OptionInstance.noTooltip(),(c, d) -> percentValueLabel(c, d*2), OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {});
        selectedControlIcons = new OptionInstance<>("legacy.options.controlIcons", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,i.equals("auto")? Component.translatable("legacy.options.auto_value", ControlType.getActiveType().getDisplayName()) : ControlType.typesMap.get(i).getDisplayName()),  new OptionInstance.ClampingLazyMaxIntRange(0, ControlType.types::size,Integer.MAX_VALUE).xmap(i-> i == 0 ? "auto" : ControlType.types.get(i - 1).getId().toString(), s-> s.equals("auto") ? 0 : (1 + ControlType.types.indexOf(ControlType.typesMap.get(s)))), "auto", d -> {});
        createWorldDifficulty = new OptionInstance<>("options.difficulty", d->Tooltip.create(d.getInfo()), (c, d) -> d.getDisplayName(), new OptionInstance.Enum<>(Arrays.asList(Difficulty.values()), Codec.INT.xmap(Difficulty::byId, Difficulty::getId)), Difficulty.NORMAL, d -> {});
        smoothMovement = OptionInstance.createBoolean("legacy.options.smoothMovement",true);
        legacyCreativeBlockPlacing = OptionInstance.createBoolean("legacy.options.legacyCreativeBlockPlacing",true);
        smoothAnimatedCharacter = OptionInstance.createBoolean("legacy.options.smoothAnimatedCharacter",false);
        autoResolution = OptionInstance.createBoolean("legacy.options.autoResolution", true, b -> minecraft.resizeDisplay());
        if(Legacy4JClient.canLoadVanillaOptions)
            load();
    }
    @Unique
    private void load(File optionsFile){
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
                        dataResult.error().ifPresent((partialResult) -> {
                            Legacy4J.LOGGER.error("Error parsing option value " + string2 + " for option " + optionInstance + ": " + partialResult.message());
                        });
                        Optional<T> var10000 = dataResult.result();
                        Objects.requireNonNull(optionInstance);
                        var10000.ifPresent(optionInstance::set);
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
    @Inject(method = "loadSelectedResourcePacks",at = @At("HEAD"), cancellable = true)
    private void loadSelectedResourcePacks(PackRepository packRepository, CallbackInfo ci){
        Assort.init();
        packRepository.setSelected(Assort.getDefaultResourceAssort().packs());
        Assort.updateSavedResourcePacks();
        ci.cancel();
    }
    @Inject(method = "load",at = @At("RETURN"))
    private void load(CallbackInfo ci){
        load(optionsFile);
        load(legacyOptionsFile);
    }
    @Inject(method = "save",at = @At("RETURN"))
    private void save(CallbackInfo ci){
        try {
            final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(legacyOptionsFile), StandardCharsets.UTF_8));
            try {
                printWriter.println("lastLoadedVersion:" + Legacy4J.VERSION.get());
                this.processLegacyOptions(
                        new Options.FieldAccess() {
                            public void writePrefix(String string) {
                                printWriter.print(string);
                                printWriter.print(':');
                            }

                            @Override
                            public <T> void process(String string, OptionInstance<T> optionInstance) {
                                DataResult<JsonElement> dataResult = optionInstance.codec().encodeStart(JsonOps.INSTANCE, optionInstance.get());
                                dataResult.error().ifPresent((partialResult) -> {
                                    Legacy4J.LOGGER.error("Error saving option " + optionInstance + ": " + partialResult);
                                });
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
    private void processLegacyOptions(Options.FieldAccess fieldAccess){
        fieldAccess.process("hudDistance", hudDistance);
        fieldAccess.process("hudOpacity", hudOpacity);
        fieldAccess.process("autoResolution", autoResolution);
        fieldAccess.process("interfaceResolution", interfaceResolution);
        fieldAccess.process("interfaceSensitivity", interfaceSensitivity);
        fieldAccess.process("terrainFogStart", terrainFogStart);
        fieldAccess.process("terrainFogEnd", terrainFogEnd);
        fieldAccess.process("overrideTerrainFogStart", overrideTerrainFogStart);
        autoSaveInterval.set(fieldAccess.process("autoSave", autoSaveInterval.get(), s-> s.equals("true") ? 1 : s.equals("false") ? 0 : Integer.parseInt(s), String::valueOf));
        fieldAccess.process("autoSaveWhenPausing", autoSaveWhenPaused);
        fieldAccess.process("gameTooltips", inGameTooltips);
        fieldAccess.process("tooltipBoxes", tooltipBoxes);
        fieldAccess.process("hints", hints);
        fieldAccess.process("flyingViewRolling", flyingViewRolling);
        fieldAccess.process("directSaveLoad", directSaveLoad);
        fieldAccess.process("vignette", vignette);
        fieldAccess.process("caveSounds", caveSounds);
        fieldAccess.process("minecartSounds", minecartSounds);
        fieldAccess.process("createWorldDifficulty", createWorldDifficulty);
        fieldAccess.process("showVanillaRecipeBook", showVanillaRecipeBook);
        fieldAccess.process("displayNameTagBorder", displayNameTagBorder);
        fieldAccess.process("legacyItemTooltips", legacyItemTooltips);
        fieldAccess.process("displayHUD", displayHUD);
        fieldAccess.process("displayHand", displayHand);
        fieldAccess.process("invertYController", invertYController);
        fieldAccess.process("invertControllerButtons", invertControllerButtons);
        fieldAccess.process("selectedController", selectedController);
        selectedControllerHandler.set(fieldAccess.process("selectedControllerHandler", selectedControllerHandler.get(), s-> ControllerManager.handlers.indexOf(ControllerManager.handlerById(s)), i-> ControllerManager.handlers.get(i).getId()));
        fieldAccess.process("cursorMode", cursorMode);
        fieldAccess.process("controllerVirtualCursor", controllerVirtualCursor);
        fieldAccess.process("unfocusedInputs", unfocusedInputs);
        fieldAccess.process("leftStickDeadZone", leftStickDeadZone);
        fieldAccess.process("rightStickDeadZone", rightStickDeadZone);
        fieldAccess.process("leftTriggerDeadZone", leftTriggerDeadZone);
        fieldAccess.process("rightTriggerDeadZone", rightTriggerDeadZone);
        fieldAccess.process("hudScale", hudScale);
        String icon = fieldAccess.process("controllerIcons", selectedControlIcons.get(), s->{
            Integer oldOrdinal;
            try {
                oldOrdinal = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                oldOrdinal = null;
            }
            return oldOrdinal == null ? s : oldOrdinal == 0 ? "auto" : ControlType.defaultTypes.get(oldOrdinal).getId().toString();
        }, s-> s);
        if (Legacy4JClient.isGameLoadFinished) selectedControlIcons.set(icon) ;
        else Legacy4JClient.SECURE_EXECUTOR.executeWhen(()->{
            if (Legacy4JClient.isGameLoadFinished){
                selectedControlIcons.set(icon);
                return true;
            } return false;
        });
        fieldAccess.process("legacyCreativeTab", legacyCreativeTab);
        fieldAccess.process("searchCreativeTab", searchCreativeTab);
        fieldAccess.process("animatedCharacter", animatedCharacter);
        fieldAccess.process("classicCrafting", classicCrafting);
        fieldAccess.process("vanillaTabs", vanillaTabs);
        fieldAccess.process("displayLegacyGamma", displayLegacyGamma);
        fieldAccess.process("legacyGamma", legacyGamma);
        fieldAccess.process("smoothAnimatedCharacter", smoothAnimatedCharacter);
        fieldAccess.process("smoothMovement", smoothMovement);
        fieldAccess.process("legacyCreativeBlockPlacing", legacyCreativeBlockPlacing);
        for (KeyMapping keyMapping : keyMappings) {
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
            if (mapping.getBinding() != binding) mapping.setBinding(binding != null && !binding.isBindable ? mapping.getDefaultBinding() : binding);
        }
    }

    public OptionInstance<Double> hudDistance() {return hudDistance;}
    public OptionInstance<Double> hudOpacity() {return hudOpacity;}
    public OptionInstance<Double> interfaceResolution() {return interfaceResolution;}
    public OptionInstance<Double> interfaceSensitivity() {
        return interfaceSensitivity;
    }
    public OptionInstance<Boolean> overrideTerrainFogStart() {
        return overrideTerrainFogStart;
    }
    public OptionInstance<Integer> terrainFogStart() {
        return terrainFogStart;
    }
    public OptionInstance<Double> terrainFogEnd() {
        return terrainFogEnd;
    }
    public OptionInstance<Boolean> legacyCreativeTab() {
        return legacyCreativeTab;
    }
    public OptionInstance<Boolean> searchCreativeTab() {
        return searchCreativeTab;
    }
    public OptionInstance<Boolean> displayHUD() {
        return displayHUD;
    }
    public OptionInstance<Boolean> displayHand() {
        return displayHand;
    }
    public OptionInstance<Boolean> animatedCharacter() {
        return animatedCharacter;
    }
    public OptionInstance<Boolean> classicCrafting() {return classicCrafting;}
    public OptionInstance<Integer> autoSaveInterval() {return autoSaveInterval;}
    public OptionInstance<Boolean> showVanillaRecipeBook() {
        return showVanillaRecipeBook;
    }
    public OptionInstance<Boolean> displayLegacyGamma() {
        return displayLegacyGamma;
    }
    public OptionInstance<Double> legacyGamma() {return legacyGamma;}
    public OptionInstance<Boolean> inGameTooltips() {
        return inGameTooltips;
    }
    public OptionInstance<Boolean> tooltipBoxes() {
        return tooltipBoxes;
    }
    public OptionInstance<Boolean> hints() {
        return hints;
    }
    public OptionInstance<Boolean> flyingViewRolling() {
        return flyingViewRolling;
    }
    public OptionInstance<Boolean> directSaveLoad() {
        return directSaveLoad;
    }
    public OptionInstance<Difficulty> createWorldDifficulty() {return createWorldDifficulty;}
    public OptionInstance<Boolean> vignette() {
        return vignette;
    }
    public OptionInstance<Boolean> minecartSounds() {
        return minecartSounds;
    }
    public OptionInstance<Boolean> caveSounds() {
        return caveSounds;
    }
    public OptionInstance<Integer> hudScale() {
        return hudScale;
    }
    public OptionInstance<Boolean> vanillaTabs() {
        return vanillaTabs;
    }
    public OptionInstance<Boolean> displayNameTagBorder() {
        return displayNameTagBorder;
    }
    public OptionInstance<Boolean> invertYController() {
        return invertYController;
    }
    public OptionInstance<String> selectedControlIcons() {
        return selectedControlIcons;
    }
    public OptionInstance<Boolean> invertControllerButtons() {
        return invertControllerButtons;
    }
    public OptionInstance<Integer> selectedController() {
        return selectedController;
    }
    public OptionInstance<Boolean> legacyItemTooltips() {
        return legacyItemTooltips;
    }
    public OptionInstance<Boolean> smoothMovement() {
        return smoothMovement;
    }
    public OptionInstance<Boolean> legacyCreativeBlockPlacing() {
        return legacyCreativeBlockPlacing;
    }
    public OptionInstance<Boolean> smoothAnimatedCharacter() {
        return smoothAnimatedCharacter;
    }
    public OptionInstance<Integer> selectedControllerHandler() {
        return selectedControllerHandler;
    }
    public OptionInstance<Integer> cursorMode() {
        return cursorMode;
    }
    public OptionInstance<Boolean> controllerVirtualCursor() {
        return controllerVirtualCursor;
    }
    public OptionInstance<Boolean> unfocusedInputs() {
        return unfocusedInputs;
    }
    public OptionInstance<Boolean> autoResolution() {
        return autoResolution;
    }
    public OptionInstance<Double> leftStickDeadZone() {
        return leftStickDeadZone;
    }
    public OptionInstance<Double> rightStickDeadZone() {
        return rightStickDeadZone;
    }
    public OptionInstance<Double> leftTriggerDeadZone() {
        return leftTriggerDeadZone;
    }
    public OptionInstance<Double> rightTriggerDeadZone() {
        return rightTriggerDeadZone;
    }
    public OptionInstance<Boolean> autoSaveWhenPaused() {
        return autoSaveWhenPaused;
    }
}
