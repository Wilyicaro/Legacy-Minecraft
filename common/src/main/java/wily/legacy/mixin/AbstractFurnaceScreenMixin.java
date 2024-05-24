package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> {
    @Shadow private boolean widthTooNarrow;

    @Shadow @Final public AbstractFurnaceRecipeBookComponent recipeBookComponent;
    private ImageButton recipeButton;

    public AbstractFurnaceScreenMixin(T abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 214;
        imageHeight = 215;
        inventoryLabelX = 14;
        inventoryLabelY = 98;
        titleLabelX = 14;
        titleLabelY = 11;
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        if (((LegacyOptions)minecraft.options).showVanillaRecipeBook().get()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 49, topPos + 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, (button) -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + 49, topPos + 49);
            }));
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        Component ingredient = Component.translatable("legacy.container.ingredient");
        guiGraphics.drawString(this.font, ingredient, 70 - font.width(ingredient), 32, 0x383838, false);
        Component fuel = Component.translatable("legacy.container.fuel");
        guiGraphics.drawString(this.font, fuel, 70 - font.width(fuel), 79, 0x383838, false);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 77,topPos + 48,0);
        guiGraphics.pose().scale(19/13f,19/13f,1.0f);
        guiGraphics.blitSprite(LegacySprites.LIT,0,0, 13, 13);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 75.5,topPos + 46.5,0);
        guiGraphics.pose().scale(19/39f,19/39f,1.0f);
        if (menu.isLit()) {
            int n = Mth.ceil(menu.getLitProgress() * 39.0f) + 1;
            guiGraphics.blitSprite(LegacySprites.LIT_PROGRESS, 42, 42, 0, 42 - n, 0, 42 - n, 42, n);
        }
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 48,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.0f);
        guiGraphics.blitSprite(ARROW,0,0,22,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 46.5,0);
        guiGraphics.pose().scale(0.5f,0.5f,1.0f);
        guiGraphics.blitSprite(LegacySprites.FULL_ARROW,66,48,0,0,0,0,2, (int) Math.ceil(menu.getBurnProgress() * 66), 48);
        guiGraphics.pose().popPose();
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
    }
}
