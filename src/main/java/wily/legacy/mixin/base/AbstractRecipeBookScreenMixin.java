//? if >=1.21.2 {
package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
    @Shadow @Final private RecipeBookComponent<?> recipeBookComponent;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;Z)V", shift = At.Shift.AFTER), cancellable = true)
    protected void init(CallbackInfo ci) {
        if (!LegacyOption.showVanillaRecipeBook.get() || ((Object)this) instanceof InventoryScreen && !ScreenUtil.hasClassicCrafting()) {
            ci.cancel();
            if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        }
    }
}
//?}
