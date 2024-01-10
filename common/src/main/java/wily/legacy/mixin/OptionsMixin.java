package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.LegacyOptions;

import java.io.File;

@Mixin(Options.class)
public abstract class OptionsMixin implements LegacyOptions {
    @Shadow public boolean hideGui;

    @Shadow protected Minecraft minecraft;

    @Shadow public abstract void load();


    @Shadow
    public static Component genericValueLabel(Component arg, Component arg2) {
        return null;
    }

    private OptionInstance<Double> hudDistance;

    private OptionInstance<Double> hudOpacity;
    private OptionInstance<Integer> autoSaveInterval;

    private OptionInstance<Boolean> legacyCreativeTab;

    private OptionInstance<Boolean> displayHUD;

    private OptionInstance<Boolean> animatedCharacter;

    private OptionInstance<Boolean> classicCrafting;
    private OptionInstance<Boolean> autoSaveWhenPause;
    private OptionInstance<Boolean> showVanillaRecipeBook;
    private OptionInstance<Boolean> legacyGamma;

    @Redirect(method = "<init>", at = @At( value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    protected void init(Options instance) {

    }
    @Inject(method = "<init>", at = @At( "RETURN"))
    protected void init(Minecraft minecraft, File file, CallbackInfo ci) {
        animatedCharacter = OptionInstance.createBoolean("legacy.options.animatedCharacter",true);
        classicCrafting = OptionInstance.createBoolean("legacy.options.classicCrafting",true);
        legacyGamma = OptionInstance.createBoolean("legacy.options.gamma",true);
        displayHUD = OptionInstance.createBoolean("legacy.options.displayHud",!hideGui, b-> hideGui = !b);
        legacyCreativeTab = OptionInstance.createBoolean("legacy.options.creativeTab", true);
        autoSaveWhenPause = OptionInstance.createBoolean("legacy.options.autoSaveWhenPause", false);
        autoSaveInterval = new OptionInstance<>("legacy.options.autoSaveInterval", OptionInstance.noTooltip(), (c,i)-> i == 0 ? genericValueLabel(c,Component.translatable("options.off")) :Component.translatable( "legacy.options.mins_value",c, i * 5), new OptionInstance.IntRange(0,24), 1, d -> {});
        showVanillaRecipeBook = OptionInstance.createBoolean("legacy.options.showVanillaRecipeBook", false);
        hudOpacity = new OptionInstance<>("legacy.options.hudOpacity", OptionInstance.noTooltip(), (c, d) -> Component.translatable("options.percent_value", c, (int) (d * 100.0)), OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {});
        hudDistance = new OptionInstance<>("legacy.options.hudDistance", OptionInstance.noTooltip(), (c, d) -> Component.translatable("options.percent_value", c, (int) (d * 100.0)), OptionInstance.UnitDouble.INSTANCE, 1.0, d -> {});
        if(LegacyMinecraftClient.canLoadVanillaOptions)
            load();
    }
    @Inject(method = "processOptions",at = @At("HEAD"))
    private void processOptions(Options.FieldAccess fieldAccess, CallbackInfo ci){
        fieldAccess.process("hudDistance", this.hudDistance);
        fieldAccess.process("hudOpacity", this.hudOpacity);
        fieldAccess.process("autoSaveWhenPause", this.autoSaveWhenPause);
        fieldAccess.process("autoSaveInterval", this.autoSaveInterval);
        fieldAccess.process("showVanillaRecipeBook", this.showVanillaRecipeBook);
        fieldAccess.process("displayHUD", displayHUD);
        fieldAccess.process("legacyCreativeTab", this.legacyCreativeTab);
        fieldAccess.process("animatedCharacter", animatedCharacter);
        fieldAccess.process("classicCrafting", classicCrafting);
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
}
