package wily.legacy.mixin.base.client.container;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

@Mixin({ContainerScreen.class, ShulkerBoxScreen.class, HopperScreen.class, DispenserScreen.class})
public abstract class ContainerScreenMixin extends AbstractContainerScreen {

    public ContainerScreenMixin(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    //? if >1.20.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }
    //?} else {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor) {
    }
    *///?}

    @Override
    protected void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.extractLabels(GuiGraphicsExtractor, i, j));
    }

    public void init() {
        int rows = menu instanceof ChestMenu m ? m.getRowCount() : menu instanceof HopperMenu ? 1 : 3;
        int columns = menu instanceof HopperMenu ? 5 : menu instanceof DispenserMenu ? 3 : 9;
        boolean sd = LegacyOptions.getUIMode().isSD();
        int slotsWidth = sd ? 13 : 21;
        int yDiff = (rows - 3) * slotsWidth;
        boolean centeredTitle = menu instanceof HopperMenu || menu instanceof DispenserMenu;
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 130 : 215);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight((sd ? 128 : 207) + yDiff);
        if (sd)
            LegacyFontUtil.defaultFontOverride = LegacyFontUtil.MOJANGLES_11_FONT;
        titleLabelX = centeredTitle ? (imageWidth - font.width(title)) / 2 : sd ? 7 : 14;
        LegacyFontUtil.defaultFontOverride = null;
        titleLabelY = sd ? 5 : 11;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = (sd ? 56 : 94) + yDiff;
        LegacySlotDisplay display = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsWidth;
            }
        };
        int slotsAmount = rows * columns;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < slotsAmount) {
                LegacySlotDisplay.override(s, (centeredTitle ? (imageWidth - columns * slotsWidth) / 2 : inventoryLabelX) + s.getContainerSlot() % columns * slotsWidth, (sd ? 15 : 26) + s.getContainerSlot() / columns * slotsWidth, display);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsWidth, (sd ? 66 : 107) + (s.getContainerSlot() - 9) / 9 * slotsWidth + yDiff, display);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsWidth, (sd ? 111 : 177) + yDiff, display);
            }
        }
        super.init();
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", LegacyOptions.getUIMode().isSD() ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
    }
}
