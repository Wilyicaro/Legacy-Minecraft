package wily.legacy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Mod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.nio.file.Path;

public class LegacyMinecraftPlatform {
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
    public static Screen getConfigScreen(Mod mod, Screen screen) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static boolean isLoadingMod(String modId) {
        throw new AssertionError();
    }
}

