package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
//? if <1.21.2 {
/*import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
*///?}
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
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
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> {
    //? if <1.21.2 {
    /*@Shadow private boolean widthTooNarrow;

    @Shadow @Final public AbstractFurnaceRecipeBookComponent recipeBookComponent;

    @Unique
    private ImageButton recipeButton;
    *///?}
    public AbstractFurnaceScreenMixin(T abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).replace(3,i-> i, c-> hoveredSlot == null || hoveredSlot.getItem().isEmpty() || hoveredSlot.container != minecraft.player.getInventory() ? c : menu.canSmelt(hoveredSlot.getItem()) ? LegacyComponents.MOVE_INGREDIENT : /*? if <1.21.2 {*//*AbstractFurnaceBlockEntity.isFuel*//*?} else {*/minecraft.level.fuelValues().isFuel/*?}*/(hoveredSlot.getItem()) ? LegacyComponents.MOVE_FUEL : c);
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
        imageWidth = 214;
        imageHeight = 215;
        inventoryLabelX = 14;
        inventoryLabelY = 98;
        titleLabelX = 14;
        titleLabelY = 11;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 77, 25);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 77,72);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 155, 44,new LegacySlotDisplay(){
                    public int getWidth() {return 32;}
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,111 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,180);
            }
        }
        //? if <1.21.2 {
        /*this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        if (LegacyOption.showVanillaRecipeBook.get()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 49, topPos + 49, 20, 18, /^? if >1.20.1 {^/RecipeBookComponent.RECIPE_BUTTON_SPRITES/^?} else {^//^0, 19, RECIPE_BUTTON_LOCATION^//^?}^/, (button) -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + 49, topPos + 49);
            }));
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        *///?}
    }

    //? if >=1.21.2 {
    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    protected void getRecipeBookButtonPosition(CallbackInfoReturnable<ScreenPosition> cir){
        cir.setReturnValue(new ScreenPosition(this.leftPos + 49, topPos + 49));
    }
    //?}

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        Component ingredient = Component.translatable("legacy.container.ingredient");
        guiGraphics.drawString(this.font, ingredient, 70 - font.width(ingredient), 32, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        Component fuel = Component.translatable("legacy.container.fuel");
        guiGraphics.drawString(this.font, fuel, 70 - font.width(fuel), 79, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 77,topPos + 48,0);
        guiGraphics.pose().scale(19/13f,19/13f,1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.LIT,0,0, 13, 13);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 75.5,topPos + 46.5,0);
        guiGraphics.pose().scale(19/39f,19/39f,1.0f);
        if (menu.isLit()) {
            int n = Mth.ceil(menu.getLitProgress() * 39.0f) + 1;
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.LIT_PROGRESS, 42, 42, 0, 42 - n, 0, 42 - n,0, 42, n);
        }
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 48,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW,0,0,22,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 46.5,0);
        guiGraphics.pose().scale(0.5f,0.5f,1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.FULL_ARROW,66,48,0,0,0,0,2, (int) Math.ceil(menu.getBurnProgress() * 66), 48);
        guiGraphics.pose().popPose();
        //? <1.21.2 {
        /*if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
        *///?}
    }
}
