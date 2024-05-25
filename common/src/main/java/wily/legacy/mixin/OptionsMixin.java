package wily.legacy.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.network.PlayerInfoSync;

import java.io.File;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

@Mixin(Options.class)
public abstract class OptionsMixin implements LegacyOptions {
    @Shadow public boolean hideGui;

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

    private OptionInstance<Double> hudDistance;
    private OptionInstance<Double> hudOpacity;
    private OptionInstance<Double> interfaceResolution;
    private OptionInstance<Double> interfaceSensitivity;
    private OptionInstance<Integer> terrainFogStart;
    private OptionInstance<Double> terrainFogEnd;
    private OptionInstance<Boolean> overrideTerrainFogStart;
    private OptionInstance<Boolean> legacyCreativeTab;
    private OptionInstance<Boolean> displayHUD;
    private OptionInstance<Boolean> animatedCharacter;
    private OptionInstance<Boolean> classicCrafting;
    private OptionInstance<Boolean> vanillaTabs;
    private OptionInstance<Boolean> autoSave;
    private OptionInstance<Integer> hudScale;
    private OptionInstance<Boolean> showVanillaRecipeBook;
    private OptionInstance<Double> legacyGamma;
    private OptionInstance<Boolean> inGameTooltips;
    private OptionInstance<Boolean> tooltipBoxes;
    private OptionInstance<Boolean> hints;
    private OptionInstance<Boolean> directSaveLoad;
    private OptionInstance<Boolean> vignette;
    private OptionInstance<Boolean> forceYellowText;
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
    private OptionInstance<Integer> controllerIcons;
    private OptionInstance<Boolean> smoothMovement;
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
        return ()-> (minecraft == null || minecraft.player == null || (!minecraft.player.getAbilities().flying && minecraft.player.getVehicle() == null && (!minecraft.player.isUnderWater() || minecraft.player.onGround()))) && booleanSupplier.getAsBoolean();
    }
    @Redirect(method = "<init>", at = @At( value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    protected void init(Options instance) {

    }
    @Inject(method = "<init>", at = @At( "RETURN"))
    protected void init(Minecraft minecraft, File file, CallbackInfo ci) {
        animatedCharacter = OptionInstance.createBoolean("legacy.options.animatedCharacter",true);
        classicCrafting = OptionInstance.createBoolean("legacy.options.classicCrafting",false, b-> {
            if (minecraft.player != null) Legacy4J.NETWORK.sendToServer(new PlayerInfoSync(b ? 1 : 2, minecraft.player));
        });
        vanillaTabs = OptionInstance.createBoolean("legacy.options.vanillaTabs",OptionInstance.cachedConstantTooltip(Component.translatable("legacy.options.vanillaTabs.description")),false);
        legacyGamma = new OptionInstance<>("legacy.options.gamma", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.5, d -> {});
        displayHUD = OptionInstance.createBoolean("legacy.options.displayHud",!hideGui, b-> hideGui = !b);
        legacyCreativeTab = OptionInstance.createBoolean("legacy.options.creativeTab", true);
        autoSave = OptionInstance.createBoolean("legacy.options.autoSave", true);
        inGameTooltips = OptionInstance.createBoolean("legacy.options.gameTooltips", true);
        tooltipBoxes = OptionInstance.createBoolean("legacy.options.tooltipBoxes", true);
        hints = OptionInstance.createBoolean("legacy.options.hints", true);
        directSaveLoad = OptionInstance.createBoolean("legacy.options.directSaveLoad", false);
        vignette = OptionInstance.createBoolean("legacy.options.vignette", false);
        minecartSounds = OptionInstance.createBoolean("legacy.options.minecartSounds", true);
        caveSounds = OptionInstance.createBoolean("legacy.options.caveSounds", true);
        showVanillaRecipeBook = OptionInstance.createBoolean("legacy.options.showVanillaRecipeBook", false);
        forceYellowText = OptionInstance.createBoolean("legacy.options.forceYellowText", false);
        displayNameTagBorder = OptionInstance.createBoolean("legacy.options.displayNameTagBorder", true);
        legacyItemTooltips = OptionInstance.createBoolean("legacy.options.legacyItemTooltips", true);
        invertYController = OptionInstance.createBoolean("legacy.options.invertYController", false);
        invertControllerButtons = OptionInstance.createBoolean("legacy.options.invertControllerButtons", false, (b)-> ControllerBinding.RIGHT_BUTTON.bindingState.block());
        selectedController = new OptionInstance<>("legacy.controls.controller", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.literal(i+1 + (Legacy4JClient.controllerManager.connectedController == null ? "" : " (%s)".formatted(Legacy4JClient.controllerManager.connectedController.getName())))),  new OptionInstance.IntRange(0, 15), 0, d -> {});
        selectedControllerHandler =  new OptionInstance<>("legacy.controls.controllerHandler", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,Component.literal(ControllerManager.handlers.get(i).getName())), new OptionInstance.IntRange(0, ControllerManager.handlers.size() - 1), Minecraft.ON_OSX ? 0 : 1, d -> {
            Legacy4JClient.controllerManager.connectedController = null;
            ControllerManager.getHandler().init();
        });
        leftStickDeadZone = new OptionInstance<>("legacy.options.leftStickDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.25, d -> {});
        rightStickDeadZone = new OptionInstance<>("legacy.options.rightStickDeadZone", OptionInstance.noTooltip(), OptionsMixin::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.2, d -> {});
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
        controllerIcons = new OptionInstance<>("legacy.options.controllerIcons", OptionInstance.noTooltip(), (c, i)-> Component.translatable("options.generic_value",c,i == 0? Component.translatable("legacy.options.auto_value", ControlTooltip.getActiveControllerType().displayName) : ControlTooltip.Type.values()[i].displayName),  new OptionInstance.IntRange(0, ControlTooltip.Type.values().length - 1), 0, d -> {});
        createWorldDifficulty = new OptionInstance<>("options.difficulty", d->Tooltip.create(d.getInfo()), (c, d) -> d.getDisplayName(), new OptionInstance.Enum<>(Arrays.asList(Difficulty.values()), Codec.INT.xmap(Difficulty::byId, Difficulty::getId)), Difficulty.NORMAL, d -> {});
        smoothMovement = OptionInstance.createBoolean("legacy.options.smoothMovement",true);
        smoothAnimatedCharacter = OptionInstance.createBoolean("legacy.options.smoothAnimatedCharacter",false);
        autoResolution = OptionInstance.createBoolean("legacy.options.autoResolution", true, b -> minecraft.resizeDisplay());
        if(Legacy4JClient.canLoadVanillaOptions)
            load();
    }
    @Inject(method = "processOptions",at = @At("HEAD"))
    private void processOptions(Options.FieldAccess fieldAccess, CallbackInfo ci){
        fieldAccess.process("hudDistance", hudDistance);
        fieldAccess.process("hudOpacity", hudOpacity);
        fieldAccess.process("autoResolution", autoResolution);
        fieldAccess.process("interfaceResolution", interfaceResolution);
        fieldAccess.process("interfaceSensitivity", interfaceSensitivity);
        fieldAccess.process("terrainFogStart", terrainFogStart);
        fieldAccess.process("terrainFogEnd", terrainFogEnd);
        fieldAccess.process("overrideTerrainFogStart", overrideTerrainFogStart);
        fieldAccess.process("autoSave", autoSave);
        fieldAccess.process("gameTooltips", inGameTooltips);
        fieldAccess.process("tooltipBoxes", tooltipBoxes);
        fieldAccess.process("hints", hints);
        fieldAccess.process("directSaveLoad", directSaveLoad);
        fieldAccess.process("vignette", vignette);
        fieldAccess.process("caveSounds", caveSounds);
        fieldAccess.process("minecartSounds", minecartSounds);
        fieldAccess.process("createWorldDifficulty", createWorldDifficulty);
        fieldAccess.process("showVanillaRecipeBook", showVanillaRecipeBook);
        fieldAccess.process("forceYellowText", forceYellowText);
        fieldAccess.process("displayNameTagBorder", displayNameTagBorder);
        fieldAccess.process("legacyItemTooltips", legacyItemTooltips);
        fieldAccess.process("displayHUD", displayHUD);
        fieldAccess.process("invertYController", invertYController);
        fieldAccess.process("invertControllerButtons", invertControllerButtons);
        fieldAccess.process("selectedController", selectedController);
        fieldAccess.process("selectedControllerHandler", selectedControllerHandler);
        fieldAccess.process("leftStickDeadZone", leftStickDeadZone);
        fieldAccess.process("rightStickDeadZone", rightStickDeadZone);
        fieldAccess.process("leftTriggerDeadZone", leftTriggerDeadZone);
        fieldAccess.process("rightTriggerDeadZone", rightTriggerDeadZone);
        fieldAccess.process("hudScale", hudScale);
        fieldAccess.process("controllerIcons", controllerIcons);
        fieldAccess.process("legacyCreativeTab", legacyCreativeTab);
        fieldAccess.process("animatedCharacter", animatedCharacter);
        fieldAccess.process("classicCrafting", classicCrafting);
        fieldAccess.process("vanillaTabs", vanillaTabs);
        fieldAccess.process("legacyGamma", legacyGamma);
        fieldAccess.process("smoothAnimatedCharacter", smoothAnimatedCharacter);
        fieldAccess.process("smoothMovement", smoothMovement);
        hideGui = !displayHUD.get();
        for (KeyMapping keyMapping : keyMappings) {
            LegacyKeyMapping mapping = (LegacyKeyMapping) keyMapping;
            int i = fieldAccess.process("component_" + keyMapping.getName(), mapping.getBinding() == null ? -1 : mapping.getBinding().ordinal());
            if ((mapping.getBinding() == null && i >= 0) || (mapping.getBinding() != null && mapping.getBinding().ordinal() != i))
                mapping.setBinding(i < 0 ? null : ControllerBinding.values()[i].isBindable ? ControllerBinding.values()[i] : mapping.getDefaultBinding());
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
    public OptionInstance<Boolean> displayHUD() {
        return displayHUD;
    }
    public OptionInstance<Boolean> animatedCharacter() {
        return animatedCharacter;
    }
    public OptionInstance<Boolean> classicCrafting() {return classicCrafting;}
    public OptionInstance<Boolean> autoSave() {return autoSave;}
    public OptionInstance<Boolean> showVanillaRecipeBook() {
        return showVanillaRecipeBook;
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
    public OptionInstance<Boolean> forceYellowText() {
        return forceYellowText;
    }
    public OptionInstance<Boolean> displayNameTagBorder() {
        return displayNameTagBorder;
    }
    public OptionInstance<Boolean> invertYController() {
        return invertYController;
    }
    public OptionInstance<Integer> controllerIcons() {
        return controllerIcons;
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

    public OptionInstance<Boolean> smoothAnimatedCharacter() {
        return smoothAnimatedCharacter;
    }
    public OptionInstance<Integer> selectedControllerHandler() {
        return selectedControllerHandler;
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
}
