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
import wily.legacy.client.LegacyMixinOptions;
import wily.legacy.client.LegacyOptions;

import java.util.Collection;
import java.util.Collections;

@Mixin(value = VanillaPlugin.class, remap = false)
public class VanillaPluginMixin {
    @Redirect(method = "registerGuiHandlers", at = @At(value = "INVOKE", target = "Lmezz/jei/api/registration/IGuiHandlerRegistration;addRecipeClickArea(Ljava/lang/Class;IIII[Lmezz/jei/api/recipe/RecipeType;)V"))
    public void fixRecipeClickAreas(IGuiHandlerRegistration instance, Class<? extends AbstractContainerScreen<?>> containerScreenClass, int xPos, int yPos, int width, int height, RecipeType<?>[] recipeTypes) {
        if (
                (containerScreenClass == CraftingScreen.class && LegacyMixinOptions.legacyClassicCraftingScreen.get())/*? if >1.20.2 {*/ ||
                        (containerScreenClass == CrafterScreen.class && LegacyMixinOptions.legacyCrafterScreen.get())/*?}*/) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return Collections.singleton(LegacyOptions.getUIMode().isSD() ? IGuiClickableArea.createBasic(65, 31, 16, 13, recipeTypes) : IGuiClickableArea.createBasic(105, 43, 33, 22, recipeTypes));
                }
            });
        }
        else if (containerScreenClass == InventoryScreen.class && LegacyMixinOptions.legacyInventoryScreen.get()) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return LegacyOptions.classicCrafting.get() ? Collections.singleton(IGuiClickableArea.createBasic((LegacyOptions.getUIMode().isSD() ? 92 : 158), (LegacyOptions.getUIMode().isSD() ? 24 : 42), 16, 14, recipeTypes)) : Collections.emptyList();
                }
            });
        }
        else if (containerScreenClass == BrewingStandScreen.class && LegacyMixinOptions.legacyBrewingStandScreen.get()) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return Collections.singleton(LegacyOptions.getUIMode().isSD() ? IGuiClickableArea.createBasic(75, 12, 9, 27, recipeTypes) : IGuiClickableArea.createBasic(121, 22, 13, 42, recipeTypes));
                }
            });
        }
        else if ((containerScreenClass == FurnaceScreen.class || containerScreenClass == SmokerScreen.class || containerScreenClass == BlastFurnaceScreen.class) && LegacyMixinOptions.legacyFurnaceScreen.get()) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return Collections.singleton(LegacyOptions.getUIMode().isSD() ? IGuiClickableArea.createBasic(82, 33, 16, 14, recipeTypes) : IGuiClickableArea.createBasic(114, 47, 33, 22, recipeTypes));
                }
            });
        }
        else if (containerScreenClass == AnvilScreen.class && LegacyMixinOptions.legacyAnvilScreen.get()) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return Collections.singleton(LegacyOptions.getUIMode().isSD() ? IGuiClickableArea.createBasic(81, 38, 16, 14, recipeTypes) : IGuiClickableArea.createBasic(122, 59, 33, 22, recipeTypes));
                }
            });
        }
        else if (containerScreenClass == SmithingScreen.class && LegacyMixinOptions.legacySmithingScreen.get()) {
            instance.addGuiContainerHandler(containerScreenClass, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
                public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
                    return Collections.singleton(LegacyOptions.getUIMode().isSD() ? IGuiClickableArea.createBasic(54, 38, 16, 14, recipeTypes) : IGuiClickableArea.createBasic(82, 59, 33, 22, recipeTypes));
                }
            });
        }
        else {
            instance.addRecipeClickArea(containerScreenClass, xPos, yPos, width, height, recipeTypes);
        }
    }
}
//?}
