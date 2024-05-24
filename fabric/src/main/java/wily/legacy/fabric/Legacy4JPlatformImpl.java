package wily.legacy.fabric;

import dev.architectury.platform.Mod;
import dev.architectury.platform.fabric.PlatformImpl;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.NbtIngredient;
import net.fabricmc.fabric.impl.tag.convention.TagRegistration;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.fabric.compat.ModMenuCompat;

import java.nio.file.Path;
import java.util.Optional;

public class Legacy4JPlatformImpl {

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static TagKey<Item> getCommonItemTag(String tag) {
        return TagRegistration.ITEM_TAG_REGISTRATION.registerCommon(tag);
    }

    public static boolean isHiddenMod(Mod mod) {
        Optional<ModMetadata> container = FabricLoader.getInstance().getModContainer(mod.getModId()).map(ModContainer::getMetadata);
        return container.isPresent() && container.get().containsCustomValue("fabric-api:module-lifecycle");
    }

    public static Screen getConfigScreen(Mod mod, Screen screen) {
        return FabricLoader.getInstance().isModLoaded("modmenu") ? ModMenuCompat.getConfigScreen(mod.getModId(),screen) : PlatformImpl.CONFIG_SCREENS.containsKey(mod.getModId()) ? PlatformImpl.CONFIG_SCREENS.get(mod.getModId()).provide(screen) : null;
    }

    public static boolean isLoadingMod(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Ingredient getNBTIngredient(ItemStack... stacks) {
        return stacks[0].getTag() == null ? Ingredient.of(stacks) : new NbtIngredient(Ingredient.of(stacks),stacks[0].getTag(),false).toVanilla();
    }
    public static Ingredient getStrictNBTIngredient(ItemStack stack) {
        return new NbtIngredient(Ingredient.of(stack),stack.getTag(),true).toVanilla();
    }

}
