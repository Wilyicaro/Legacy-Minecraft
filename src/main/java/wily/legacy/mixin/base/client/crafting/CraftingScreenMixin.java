package wily.legacy.mixin.base.client.crafting;

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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {
    public CraftingScreenMixin(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 215;
        imageHeight = sd ? 140 : 202;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 66 : 90;

        LegacyFontUtil.applySDFont(b -> titleLabelX = (imageWidth - font.width(title)) / 2);
        titleLabelY = sd ? 5 : 11;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        int slotsAmount = menu.getGridHeight() * menu.getGridWidth();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 90 : 150, sd ? 28 : 39, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 21 : 32;
                    }
                });
            } else if (i < slotsAmount + 1) {
                LegacySlotDisplay.override(s, (sd ? 19 : 34) + s.getContainerSlot() % 3 * slotsSize, (sd ? 18 : 23) + s.getContainerSlot() / 3 * slotsSize, defaultDisplay);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 76 : 102) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 120 : 171, defaultDisplay);
            }
        }
    }

    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    protected void getRecipeBookButtonPosition(CallbackInfoReturnable<ScreenPosition> cir) {
        cir.setReturnValue(new ScreenPosition(this.leftPos + (LegacyOptions.getUIMode().isSD() ? 63 : 9), topPos + (LegacyOptions.getUIMode().isSD() ? 48 : 44)));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        if (sd) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_ARROW, leftPos + 65, topPos + 31, 16, 13);
        } else {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(leftPos + 105, topPos + 43);
            guiGraphics.pose().scale(1.5f, 1.5f);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW, 0, 0, 22, 16);
            guiGraphics.pose().popMatrix();
        }
    }

}
