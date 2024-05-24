package wily.legacy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Mod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.nio.file.Path;

public class Legacy4JPlatform {
    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static TagKey<Item> getCommonItemTag(String tag) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isHiddenMod(Mod mod) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isLoadingMod(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Ingredient getComponentsIngredient(ItemStack... stacks) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static Ingredient getStrictComponentsIngredient(ItemStack stack) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static Screen getConfigScreen(Mod mod, Screen screen) {
        throw new AssertionError();
    }
}

