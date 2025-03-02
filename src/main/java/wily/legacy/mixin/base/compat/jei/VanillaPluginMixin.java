//? if <=1.21.1 {
package wily.legacy.mixin.base.compat.jei;

import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.library.plugins.vanilla.VanillaPlugin;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyOptions;

import java.util.Collection;
import java.util.Collections;

@Mixin(value = VanillaPlugin.class, remap = false)
public class VanillaPluginMixin {
    @Redirect(method = "registerGuiHandlers", at = @At(value = "INVOKE", target = "Lmezz/jei/api/registration/IGuiHandlerRegistration;addRecipeClickArea(Ljava/lang/Class;IIII[Lmezz/jei/api/recipe/RecipeType;)V"))
    public void fixRecipeClickAreas(IGuiHandlerRegistration instance, Class<? extends AbstractContainerScreen<?>> containerScreenClass, int xPos, int yPos, int width, int height, RecipeType<?>[] recipeTypes) {
        if (containerScreenClass == CraftingScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 105, 43, 33, 22, recipeTypes);
        }
        //? if >1.20.2 {
        else if (containerScreenClass == CrafterScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 105, 43, 24, 24, recipeTypes);
        }
        //?}
        else if (containerScreenClass == InventoryScreen.class) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return LegacyOptions.classicCrafting.get() ? Collections.singleton(IGuiClickableArea.createBasic(158, 43, 16, 13, recipeTypes)) : Collections.emptyList();
                }
            });
        }
        else if (containerScreenClass == BrewingStandScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 121, 22, 13, 42, recipeTypes);
        }
        else if (containerScreenClass == FurnaceScreen.class || containerScreenClass == SmokerScreen.class || containerScreenClass == BlastFurnaceScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 114, 48, 33, 22, recipeTypes);
        }
        else if (containerScreenClass == AnvilScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 122, 59, 33, 22, recipeTypes);
        }
        else if (containerScreenClass == SmithingScreen.class) {
            instance.addRecipeClickArea(containerScreenClass, 82, 59, 33, 22, recipeTypes);
        }
        else {
            instance.addRecipeClickArea(containerScreenClass, xPos, yPos, width, height, recipeTypes);
        }
    }
}
//?}
