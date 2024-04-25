package wily.legacy.forge;

import dev.architectury.platform.Mod;
import dev.architectury.platform.forge.PlatformImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.common.crafting.NBTIngredient;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class LegacyMinecraftPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static TagKey<Item> getCommonItemTag(String commonTag) {
        return ItemTags.create(new ResourceLocation("forge", commonTag));
    }

    public static boolean isHiddenMod(Mod mod) {
        return false;
    }

    public static Screen getConfigScreen(Mod mod, Screen screen) {
        return ModList.get().getModContainerById(mod.getModId()).flatMap(m-> m.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class)).map(s -> s.screenFunction().apply(Minecraft.getInstance(), screen)).orElse(null);
    }

    public static boolean isLoadingMod(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    public static Ingredient getNBTIngredient(ItemStack... stacks) {
        return NBTIngredient.of(false,stacks[0].getTag(), Arrays.stream(stacks).map(ItemStack::getItem).toArray(ItemLike[]::new));
    }
    public static Ingredient getStrictNBTIngredient(ItemStack stack) {
        return NBTIngredient.of(true,stack);
    }
}
