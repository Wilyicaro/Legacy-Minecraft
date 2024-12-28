package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.LegacyOption;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {

    //? if <1.21.2 {
    /*@Shadow private boolean widthTooNarrow;

    @Shadow @Final private RecipeBookComponent recipeBookComponent;

    @Unique
    private ImageButton recipeButton;
    *///?}
    public CraftingScreenMixin(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Shadow @Final private static ResourceLocation RECIPE_BUTTON_LOCATION;
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}
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
        //? if <1.21.2 {
        /*this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        if (LegacyOption.showVanillaRecipeBook.get()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 9, topPos + 44, 20, 18, /^? if >1.20.1 {^/RecipeBookComponent.RECIPE_BUTTON_SPRITES/^?} else {^//^0, 19, RECIPE_BUTTON_LOCATION^//^?}^/, (button) -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + 9, topPos + 44);
            }));
            this.addWidget(this.recipeBookComponent);
            this.setInitialFocus(this.recipeBookComponent);
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        *///?}
        int slotsAmount = menu.getGridHeight() * menu.getGridWidth();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0){
                LegacySlotDisplay.override(s, 150, 39,new LegacySlotDisplay(){
                    public int getWidth() {return 32;}
                });
            } else if (i < slotsAmount + 1) {
                LegacySlotDisplay.override(s, 34 + s.getContainerSlot() % 3 * 21,23 + s.getContainerSlot() / 3 * 21);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,102 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,171);
            }
        }
    }

    //? if >=1.21.2 {
    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    protected void getRecipeBookButtonPosition(CallbackInfoReturnable<ScreenPosition> cir){
        cir.setReturnValue(new ScreenPosition(this.leftPos + 9, topPos + 44));
    }
    //?}

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 105,topPos + 43,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW,0,0,22,15);
        guiGraphics.pose().popPose();
        //? if <1.21.2 {
        /*if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
        *///?}
    }

}
