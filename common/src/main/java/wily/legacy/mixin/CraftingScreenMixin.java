package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {


    @Shadow private boolean widthTooNarrow;

    @Shadow @Final private RecipeBookComponent recipeBookComponent;

    @Shadow @Final private static ResourceLocation RECIPE_BUTTON_LOCATION;
    private ImageButton recipeButton;

    public CraftingScreenMixin(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 215;
        imageHeight = 202;
        inventoryLabelX = 14;
        inventoryLabelY = 90;
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 11;
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width - 10, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        if (((LegacyOptions)minecraft.options).showVanillaRecipeBook().get()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 5, this.topPos + 44, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, button -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + 5, this.topPos + 44);
            }));
            this.addWidget(this.recipeBookComponent);
            this.setInitialFocus(this.recipeBookComponent);
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL,leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 105,topPos + 43,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(ARROW,0,0,22,15);
        guiGraphics.pose().popPose();
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
    }

}
