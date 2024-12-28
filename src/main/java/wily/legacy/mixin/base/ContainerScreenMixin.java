package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

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
    public void init() {
        int rows = menu instanceof ChestMenu m ? m.getRowCount() : menu  instanceof HopperMenu ? 1 : 3;
        int columns = menu instanceof HopperMenu ? 5 : menu instanceof DispenserMenu ? 3 : 9;
        int yDiff = (rows - 3) * 21;
        boolean centeredTitle = menu instanceof HopperMenu || menu instanceof DispenserMenu;
        imageWidth = 215;
        imageHeight = 207 + yDiff;
        titleLabelX = centeredTitle ? (imageWidth - font.width(title)) / 2: 14;
        titleLabelY = 11;
        inventoryLabelX = 14;
        inventoryLabelY = 94 + yDiff;
        int slotsAmount = rows * columns;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < slotsAmount) {
                LegacySlotDisplay.override(s, (imageWidth - columns * 21) / 2  + s.getContainerSlot() % columns * 21,26 + s.getContainerSlot() / columns * 21);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,107 + (s.getContainerSlot() - 9) / 9 * 21 + yDiff);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,177 + yDiff);
            }
        }
        super.init();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
    }
}
