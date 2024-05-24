package wily.legacy.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.world.Difficulty;

public interface LegacyOptions {

    OptionInstance<Double> hudDistance();
    OptionInstance<Double> hudOpacity();
    OptionInstance<Double> interfaceResolution();
    OptionInstance<Double> interfaceSensitivity();
    OptionInstance<Boolean> overrideTerrainFogStart();
    OptionInstance<Integer> terrainFogStart();
    OptionInstance<Double> terrainFogEnd();
    OptionInstance<Integer> hudScale();
    OptionInstance<Boolean> legacyCreativeTab();
    OptionInstance<Boolean> displayHUD();
    OptionInstance<Boolean> animatedCharacter();
    OptionInstance<Boolean> classicCrafting();
    OptionInstance<Boolean> autoResolution();
    OptionInstance<Boolean> autoSave();
    OptionInstance<Boolean> smoothMovement();
    OptionInstance<Boolean> showVanillaRecipeBook();
    OptionInstance<Double> legacyGamma();
    OptionInstance<Boolean> inGameTooltips();
    OptionInstance<Boolean> tooltipBoxes();
    OptionInstance<Boolean> hints();
    OptionInstance<Boolean> directSaveLoad();
    OptionInstance<Boolean> vignette();
    OptionInstance<Boolean> caveSounds();
    OptionInstance<Boolean> minecartSounds();
    OptionInstance<Boolean> vanillaTabs();
    OptionInstance<Boolean> forceYellowText();
    OptionInstance<Boolean> displayNameTagBorder();
    OptionInstance<Boolean> legacyItemTooltips();
    OptionInstance<Boolean> invertYController();
    OptionInstance<Boolean> invertControllerButtons();
    OptionInstance<Integer> controllerIcons();
    OptionInstance<Integer> selectedController();
    OptionInstance<Integer> selectedControllerHandler();
    OptionInstance<Difficulty> createWorldDifficulty();
    OptionInstance<Boolean> smoothAnimatedCharacter();
    OptionInstance<Double> leftStickDeadZone();
    OptionInstance<Double> rightStickDeadZone();
    OptionInstance<Double> leftTriggerDeadZone();
    OptionInstance<Double> rightTriggerDeadZone();
}
