package wily.legacy.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.world.Difficulty;

public interface LegacyOptions {

    OptionInstance<Double> hudDistance();
    OptionInstance<Double> hudOpacity();
    OptionInstance<Integer> autoSaveInterval();
    OptionInstance<Boolean> legacyCreativeTab();
    OptionInstance<Boolean> displayHUD();
    OptionInstance<Boolean> animatedCharacter();
    OptionInstance<Boolean> classicCrafting();
    OptionInstance<Boolean> autoSaveWhenPause();
    OptionInstance<Boolean> showVanillaRecipeBook();
    OptionInstance<Boolean> legacyGamma();
    OptionInstance<Boolean> inGameTooltips();
    OptionInstance<Boolean> hints();
    OptionInstance<Boolean> directSaveLoad();
    OptionInstance<Boolean> vignette();
    OptionInstance<Boolean> caveSounds();
    OptionInstance<Boolean> minecartSounds();
    OptionInstance<Difficulty> createWorldDifficulty();
}
