//? if >=1.21.2 {
package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyMenuAccess;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu> implements LegacyMenuAccess<RecipeBookMenu> {
    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    public AbstractRecipeBookScreenMixin(RecipeBookMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;Z)V", shift = At.Shift.AFTER), cancellable = true)
    protected void init(CallbackInfo ci) {
        if (!LegacyOptions.showVanillaRecipeBook.get() || ((Object) this) instanceof InventoryScreen && !LegacyOptions.hasClassicCrafting()) {
            ci.cancel();
            if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        }
    }

    @Override
    public ScreenRectangle getMenuRectangleLimit() {
        return recipeBookComponent.isVisible() ? LegacyMenuAccess.createMenuRectangleLimit(this, leftPos - 160, topPos + (imageHeight - 166) / 2, imageWidth + 160, Math.max(166, imageHeight)) : LegacyMenuAccess.createMenuRectangleLimit(this, leftPos, topPos, imageWidth, imageHeight);
    }

}
//?}
