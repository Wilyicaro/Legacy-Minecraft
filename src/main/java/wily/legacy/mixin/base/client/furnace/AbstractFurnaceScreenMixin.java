package wily.legacy.mixin.base.client.furnace;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
//? if <1.21.2 {
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
//?}
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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> {
    //? if <1.21.2 {
    @Shadow private boolean widthTooNarrow;

    @Shadow @Final public AbstractFurnaceRecipeBookComponent recipeBookComponent;

    @Unique
    private ImageButton recipeButton;
    //?}
    public AbstractFurnaceScreenMixin(T abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).replace(3,i-> i, c-> hoveredSlot == null || hoveredSlot.getItem().isEmpty() || hoveredSlot.container != minecraft.player.getInventory() ? c : menu.canSmelt(hoveredSlot.getItem()) ? LegacyComponents.MOVE_INGREDIENT : /*? if <1.21.2 {*/AbstractFurnaceBlockEntity.isFuel/*?} else {*//*minecraft.level.fuelValues().isFuel*//*?}*/(hoveredSlot.getItem()) ? LegacyComponents.MOVE_FUEL : c);
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
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 214;
        imageHeight = sd ? 145 : 215;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 71 : 98;
        titleLabelX = sd ? 7 : 14;
        titleLabelY = sd ? 5 : 11;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        LegacySlotDisplay furnaceDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return sd ? 17 : 21;
            }
        };
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 62 : 77, sd ? 16 : 25, furnaceDisplay);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 62 : 77, sd ? 50 : 72, furnaceDisplay);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 102 : 155, sd ? 30 : 44,new LegacySlotDisplay(){
                    public int getWidth() {return sd ? 21 : 32;}
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 81 : 111) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 125 : 180, defaultDisplay);
            }
        }
        //? if <1.21.2 {
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        if (LegacyOptions.showVanillaRecipeBook.get()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            int buttonX = sd ? 39 : 49;
            int buttonY = sd ? 30 : 49;
            recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + buttonX, topPos + buttonY, 20, 18, /*? if >1.20.1 {*/RecipeBookComponent.RECIPE_BUTTON_SPRITES/*?} else {*//*0, 19, RECIPE_BUTTON_LOCATION*//*?}*/, (button) -> {
                this.recipeBookComponent.toggleVisibility();
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                button.setPosition(this.leftPos + buttonX, topPos + buttonY);
            }));
            if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
        }
        else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        //?}
    }

    //? if >=1.21.2 {
    /*@Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    protected void getRecipeBookButtonPosition(CallbackInfoReturnable<ScreenPosition> cir){
        cir.setReturnValue(new ScreenPosition(this.leftPos + (LegacyOptions.getUIMode().isSD() ? 39 : 49), topPos + (LegacyOptions.getUIMode().isSD() ? 30 : 49)));
    }
    *///?}

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        ScreenUtil.applySDFont(sd -> {
            super.renderLabels(guiGraphics, i, j);
            Component ingredient = Component.translatable("legacy.container.ingredient");
            guiGraphics.drawString(this.font, ingredient, (sd ? 57 : 70) - font.width(ingredient), sd ? 19 : 32, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            Component fuel = Component.translatable("legacy.container.fuel");
            guiGraphics.drawString(this.font, fuel, (sd ? 57 : 70) - font.width(fuel), sd ? 53 : 79, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int guiScale = Math.max(1, (int) minecraft.getWindow().getGuiScale());
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + (sd ? 63 : ScreenUtil.hasHorizontalArtifacts() ? 75.4f : 75.5f), topPos + (sd ? 34 : 46.4f),0);
        int flameSize = sd ? 14 : 21;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.LIT,0,0, flameSize, flameSize);
        if (menu.isLit()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1.0f);
            int scaledSize = flameSize * guiScale;
            int n = Mth.ceil(/*? if >1.20.1 {*/menu.getLitProgress()/*?} else {*//*Mth.clamp(menu.getLitProgress()/ 13f,0,1)*//*?}*/ * (scaledSize - guiScale)) + guiScale;
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.LIT_PROGRESS, scaledSize, scaledSize, 0, scaledSize - n, 0, scaledSize - n, scaledSize, n);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().popPose();
        boolean fhd = LegacyOptions.getUIMode().isFHD();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + (sd ? 82 : fhd ? 109 : 114), topPos + (sd ? 33 : 47), 0);
        int arrowWidth = sd ? 16 : 33;
        int arrowHeight = sd ? 14 : 24;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, arrowWidth, arrowHeight);
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.FULL_SMALL_ARROW : fhd ? LegacySprites.FULL_ARROW_1080 : LegacySprites.FULL_ARROW, arrowWidth * guiScale, arrowHeight * guiScale, 0, 0, 0, 0, (int) Math.ceil(/*? if >1.20.1 {*/menu.getBurnProgress()/*?} else {*//*Mth.clamp(menu.getBurnProgress() / 24f ,0,1)*//*?}*/ * arrowWidth * guiScale), arrowHeight * guiScale);
        guiGraphics.pose().popPose();
        //? <1.21.2 {
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
        //?}
    }
}
