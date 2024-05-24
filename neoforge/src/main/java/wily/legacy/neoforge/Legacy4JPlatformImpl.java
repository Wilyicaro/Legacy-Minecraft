package wily.legacy.neoforge;

import dev.architectury.platform.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.nio.file.Path;
import java.util.Arrays;

public class Legacy4JPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static TagKey<Item> getCommonItemTag(String commonTag) {
        return ItemTags.create(new ResourceLocation("forge", commonTag));
    }

    public static boolean isHiddenMod(Mod mod) {
        return false;
    }

    public static boolean isLoadingMod(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    public static Ingredient getComponentsIngredient(ItemStack... stacks) {
        return stacks[0].getComponents().isEmpty() ? Ingredient.of(stacks) : DataComponentIngredient.of(false,stacks[0].getComponents(), Arrays.stream(stacks).map(ItemStack::getItem).toArray(ItemLike[]::new));
    }
    public static Ingredient getStrictComponentsIngredient(ItemStack stack) {
        return DataComponentIngredient.of(true, stack.getComponents(), stack.getItem());
    }
    public static Screen getConfigScreen(Mod mod, Screen screen) {
        return ModList.get().getModContainerById(mod.getModId()).flatMap(m->  IConfigScreenFactory.getForMod(m.getModInfo())).map(s -> s.createScreen(Minecraft.getInstance(), screen)).orElse(null);
    }
}
