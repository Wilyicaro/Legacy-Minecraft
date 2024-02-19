package wily.legacy.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.LegacyOptions;

import java.io.File;
import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin implements LegacyOptions {
    @Shadow public boolean hideGui;

    @Shadow protected Minecraft minecraft;

    @Shadow public abstract void load();


    @Shadow
    public static Component genericValueLabel(Component arg, Component arg2) {
        return null;
    }

    @Shadow
    public static Component genericValueLabel(Component arg, int i) {
        return null;
    }

    private OptionInstance<Double> hudDistance;
    private OptionInstance<Double> hudOpacity;
    private OptionInstance<Integer> autoSaveInterval;
    private OptionInstance<Boolean> legacyCreativeTab;
    private OptionInstance<Boolean> displayHUD;
    private OptionInstance<Boolean> animatedCharacter;
    private OptionInstance<Boolean> classicCrafting;
    private OptionInstance<Boolean> vanillaTabs;
    private OptionInstance<Boolean> autoSaveWhenPause;
    private OptionInstance<Integer> hudScale;
    private OptionInstance<Boolean> showVanillaRecipeBook;
    private OptionInstance<Boolean> legacyGamma;
    private OptionInstance<Boolean> inGameTooltips;
    private OptionInstance<Boolean> tooltipBoxes;
    private OptionInstance<Boolean> hints;
    private OptionInstance<Boolean> directSaveLoad;
    private OptionInstance<Boolean> vignette;
    private OptionInstance<Boolean> forceYellowText;
    private OptionInstance<Boolean> caveSounds;
    private OptionInstance<Boolean> minecartSounds;
    private OptionInstance<Difficulty> createWorldDifficulty;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;)V", ordinal = 5),index = 0)
    protected String initKeyCrafting(String string) {
        return "key.crafting";
    }
    @Redirect(method = "<init>", at = @At( value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    protected void init(Options instance) {

    }
    @Inject(method = "<init>", at = @At( "RETURN"))
    protected void init(Minecraft minecraft, File file, CallbackInfo ci) {
        animatedCharacter = OptionInstance.createBoolean("legacy.options.animatedCharacter",true);
        classicCrafting = OptionInstance.createBoolean("legacy.options.classicCrafting",false);
        vanillaTabs = OptionInstance.createBoolean("legacy.options.vanillaTabs",OptionInstance.cachedConstantTooltip(Component.translatable("legacy.options.vanillaTabs.description")),false);
        legacyGamma = OptionInstance.createBoolean("legacy.options.gamma",true);
        displayHUD = OptionInstance.createBoolean("legacy.options.displayHud",!hideGui, b-> hideGui = !b);
        legacyCreativeTab = OptionInstance.createBoolean("legacy.options.creativeTab", true);
        autoSaveWhenPause = OptionInstance.createBoolean("legacy.options.autoSaveWhenPause", false);
        inGameTooltips = OptionInstance.createBoolean("legacy.options.gameTooltips", true);
        tooltipBoxes = OptionInstance.createBoolean("legacy.options.tooltipBoxes", true);
        hints = OptionInstance.createBoolean("legacy.options.hints", true);
        directSaveLoad = OptionInstance.createBoolean("legacy.options.directSaveLoad", false);
        vignette = OptionInstance.createBoolean("legacy.options.vignette", false);
        minecartSounds = OptionInstance.createBoolean("legacy.options.minecartSounds", true);
        caveSounds = OptionInstance.createBoolean("legacy.options.caveSounds", true);
        autoSaveInterval = new OptionInstance<>("legacy.options.autoSaveInterval", OptionInstance.noTooltip(), (c,i)-> i == 0 ? genericValueLabel(c,Component.translatable("options.off")) :Component.translatable( "legacy.options.mins_value",c, i * 5), new OptionInstance.IntRange(0,24), 1, d -> {});
        showVanillaRecipeBook = OptionInstance.createBoolean("legacy.options.showVanillaRecipeBook", false);
        forceYellowText =  OptionInstance.createBoolean("legacy.options.forceYellowText", false);
        hudScale = new OptionInstance<>("legacy.options.hudScale", OptionInstance.noTooltip(), OptionsMixin::genericValueLabel,  new OptionInstance.IntRange(1,3), 2, d -> {});
        hudOpacity = new OptionInstance<>("legacy.options.hudOpacity", OptionInstance.noTooltip(), (c, d) -> Component.translatable("options.percent_value", c, (int) (d * 100.0)), OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {});
        hudDistance = new OptionInstance<>("legacy.options.hudDistance", OptionInstance.noTooltip(), (c, d) -> Component.translatable("options.percent_value", c, (int) (d * 100.0)), OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {});
        createWorldDifficulty = new OptionInstance<>("options.difficulty", d->Tooltip.create(d.getInfo()), (c, d) -> d.getDisplayName(), new OptionInstance.Enum<>(Arrays.asList(Difficulty.values()), Codec.INT.xmap(Difficulty::byId, Difficulty::getId)), Difficulty.NORMAL, d -> {});
        if(LegacyMinecraftClient.canLoadVanillaOptions)
            load();
    }
    @Inject(method = "processOptions",at = @At("HEAD"))
    private void processOptions(Options.FieldAccess fieldAccess, CallbackInfo ci){
        fieldAccess.process("hudDistance", hudDistance);
        fieldAccess.process("hudOpacity", hudOpacity);
        fieldAccess.process("autoSaveWhenPause", autoSaveWhenPause);
        fieldAccess.process("gameTooltips", inGameTooltips);
        fieldAccess.process("tooltipBoxes", tooltipBoxes);
        fieldAccess.process("hints", hints);
        fieldAccess.process("directSaveLoad", directSaveLoad);
        fieldAccess.process("vignette", vignette);
        fieldAccess.process("caveSounds", caveSounds);
        fieldAccess.process("minecartSounds", minecartSounds);
        fieldAccess.process("createWorldDifficulty", createWorldDifficulty);
        fieldAccess.process("autoSaveInterval", autoSaveInterval);
        fieldAccess.process("showVanillaRecipeBook", showVanillaRecipeBook);
        fieldAccess.process("forceYellowText", forceYellowText);
        fieldAccess.process("displayHUD", displayHUD);
        fieldAccess.process("hudScale", hudScale);
        fieldAccess.process("legacyCreativeTab", legacyCreativeTab);
        fieldAccess.process("animatedCharacter", animatedCharacter);
        fieldAccess.process("classicCrafting", classicCrafting);
        fieldAccess.process("vanillaTabs", vanillaTabs);
        fieldAccess.process("legacyGamma", legacyGamma);
        hideGui = !displayHUD.get();
    }


    public OptionInstance<Double> hudDistance() {return hudDistance;}
    public OptionInstance<Double> hudOpacity() {return hudOpacity;}
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
    public OptionInstance<Boolean> autoSaveWhenPause() {return autoSaveWhenPause;}
    public OptionInstance<Integer> autoSaveInterval() {
        return autoSaveInterval;
    }
    public OptionInstance<Boolean> showVanillaRecipeBook() {
        return showVanillaRecipeBook;
    }
    public OptionInstance<Boolean> legacyGamma() {return legacyGamma;}
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
}
