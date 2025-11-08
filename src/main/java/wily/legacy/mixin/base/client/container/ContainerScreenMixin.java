package wily.legacy.mixin.base.client.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
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
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
    }

    public void init() {
        int rows = menu instanceof ChestMenu m ? m.getRowCount() : menu instanceof HopperMenu ? 1 : 3;
        int columns = menu instanceof HopperMenu ? 5 : menu instanceof DispenserMenu ? 3 : 9;
        boolean sd = LegacyOptions.getUIMode().isSD();
        int slowWidth = sd ? 13 : 21;
        int yDiff = (rows - 3) * slowWidth;
        boolean centeredTitle = menu instanceof HopperMenu || menu instanceof DispenserMenu;
        imageWidth = sd ? 130 : 215;
        imageHeight = (sd ? 128 : 207) + yDiff;
        titleLabelX = centeredTitle ? (imageWidth - font.width(title)) / 2 : sd ? 7 : 14;
        titleLabelY = sd ? 5 : 11;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = (sd ? 56 : 94) + yDiff;
        LegacySlotDisplay display = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slowWidth;
            }
        };
        int slotsAmount = rows * columns;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < slotsAmount) {
                LegacySlotDisplay.override(s, (centeredTitle ? (imageWidth - columns * slowWidth) / 2 : inventoryLabelX) + s.getContainerSlot() % columns * slowWidth, (sd ? 15 : 26) + s.getContainerSlot() / columns * slowWidth, display);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slowWidth, (sd ? 66 : 107) + (s.getContainerSlot() - 9) / 9 * slowWidth + yDiff, display);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slowWidth, (sd ? 111 : 177) + yDiff, display);
            }
        }
        super.init();
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", LegacyOptions.getUIMode().isSD() ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
    }
}
