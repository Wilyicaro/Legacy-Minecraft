package wily.legacy.client;

import net.minecraft.client.OptionInstance;

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
}
