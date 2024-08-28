package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> {
    @Shadow private boolean widthTooNarrow;

    @Shadow @Final public AbstractFurnaceRecipeBookComponent recipeBookComponent;
    @Shadow @Final private static ResourceLocation RECIPE_BUTTON_LOCATION;
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
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 49, this.topPos + 49, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, button -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + 49, this.topPos + 49);
            }));
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
    }
    @Override
    protected void renderLabels(PoseStack poseStack, int i, int j) {
        super.renderLabels(poseStack, i, j);
        Component ingredient = Component.translatable("legacy.container.ingredient");
        poseStack.drawString(this.font, ingredient, 70 - font.width(ingredient), 32, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        Component fuel = Component.translatable("legacy.container.fuel");
        poseStack.drawString(this.font, fuel, 70 - font.width(fuel), 79, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(PoseStack poseStack, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL,leftPos,topPos,imageWidth,imageHeight);
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 77,topPos + 48,0);
        poseStack.pose().scale(19/13f,19/13f,1.0f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.LIT,0,0, 13, 13);
        poseStack.pose().popPose();
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 75.5,topPos + 46.5,0);
        poseStack.pose().scale(19/39f,19/39f,1.0f);
        if (menu.isLit()) {
            int n = Mth.ceil(Mth.clamp(menu.getLitProgress()/ 13f,0,1) * 39.0f) + 1;
            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.LIT_PROGRESS, 42, 42, 0, 42 - n, 0, 42 - n, 42, n);
        }
        poseStack.pose().popPose();
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 114,topPos + 48,0);
        poseStack.pose().scale(1.5f,1.5f,1.0f);
        LegacyGuiGraphics.of(poseStack).blitSprite(ARROW,0,0,22,15);
        poseStack.pose().popPose();
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 114,topPos + 46.5,0);
        poseStack.pose().scale(0.5f,0.5f,1.0f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.FULL_ARROW,66,48,0,0,0,0,2, (int) Math.ceil(Mth.clamp(menu.getBurnProgress() / 24f ,0,1)* 66), 48);
        poseStack.pose().popPose();
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
    }
}
