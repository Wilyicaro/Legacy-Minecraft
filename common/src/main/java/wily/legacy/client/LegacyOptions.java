package wily.legacy.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.world.Difficulty;
import wily.legacy.Legacy4JPlatform;

import java.io.File;

public interface LegacyOptions {
    File legacyOptionsFile = Legacy4JPlatform.getConfigDirectory().resolve("legacy_options.txt").toFile();

    OptionInstance<Double> hudDistance();
    OptionInstance<Double> hudOpacity();
    OptionInstance<Double> interfaceResolution();
    OptionInstance<Double> interfaceSensitivity();
    OptionInstance<Boolean> overrideTerrainFogStart();
    OptionInstance<Integer> terrainFogStart();
    OptionInstance<Double> terrainFogEnd();
    OptionInstance<Integer> hudScale();
    OptionInstance<Boolean> legacyCreativeTab();
    OptionInstance<Boolean> searchCreativeTab();
    OptionInstance<Boolean> displayHUD();
    OptionInstance<Boolean> displayHand();
    OptionInstance<Boolean> animatedCharacter();
    OptionInstance<Boolean> classicCrafting();
    OptionInstance<Boolean> autoResolution();
    OptionInstance<Integer> autoSaveInterval();
    OptionInstance<Boolean> autoSaveWhenPaused();
    OptionInstance<Boolean> smoothMovement();
    OptionInstance<Boolean> showVanillaRecipeBook();
    OptionInstance<Boolean> displayLegacyGamma();
    OptionInstance<Double> legacyGamma();
    OptionInstance<Boolean> inGameTooltips();
    OptionInstance<Boolean> tooltipBoxes();
    OptionInstance<Boolean> hints();
    OptionInstance<Boolean> flyingViewRolling();
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
    OptionInstance<String> selectedControlIcons();
    OptionInstance<Integer> selectedController();
    OptionInstance<Integer> selectedControllerHandler();
    OptionInstance<Integer> cursorMode();
    OptionInstance<Boolean> controllerVirtualCursor();
    OptionInstance<Boolean> unfocusedInputs();
    OptionInstance<Boolean> legacyCreativeBlockPlacing();
    OptionInstance<Difficulty> createWorldDifficulty();
    OptionInstance<Boolean> smoothAnimatedCharacter();
    OptionInstance<Double> leftStickDeadZone();
    OptionInstance<Double> rightStickDeadZone();
    OptionInstance<Double> leftTriggerDeadZone();
    OptionInstance<Double> rightTriggerDeadZone();
}
