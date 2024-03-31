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
    OptionInstance<Double> terrainFogOpacity();
    OptionInstance<Integer> autoSaveInterval();
    OptionInstance<Integer> hudScale();
    OptionInstance<Boolean> legacyCreativeTab();
    OptionInstance<Boolean> displayHUD();
    OptionInstance<Boolean> animatedCharacter();
    OptionInstance<Boolean> classicCrafting();
    OptionInstance<Boolean> autoSaveWhenPause();
    OptionInstance<Boolean> showVanillaRecipeBook();
    OptionInstance<Boolean> legacyGamma();
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
    OptionInstance<Difficulty> createWorldDifficulty();
}
