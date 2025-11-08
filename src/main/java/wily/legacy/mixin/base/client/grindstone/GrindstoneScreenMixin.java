package wily.legacy.mixin.base.client.grindstone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
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

@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay() {
        public int getWidth() {
            return 30;
        }
    };
    private static final LegacySlotDisplay SD_SLOTS_DISPLAY = new LegacySlotDisplay() {
        public int getWidth() {
            return 20;
        }
    };

    public GrindstoneScreenMixin(GrindstoneMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 207;
        imageHeight = sd ? 145 : 215;
        inventoryLabelX = sd ? 7 : 10;
        inventoryLabelY = sd ? 71 : 105;
        titleLabelX = sd ? 7 : 10;
        titleLabelY = sd ? 5 : 11;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 28 : 41, sd ? 19 : 30, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 28 : 41, sd ? 44 : 65, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 90 : 138, sd ? 31 : 46, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 81 : 116) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 125 : 185, defaultDisplay);
            }
        }
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

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + (sd ? 61 : 85), topPos + (sd ? 33 : 50));
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, sd ? 16 : 33, sd ? 14 : 24);
        if (!sd) guiGraphics.pose().scale(1.5f, 1.5f);
        if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(2).hasItem())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, sd ? 0 : 2, 0, 15, 15);
        guiGraphics.pose().popMatrix();
    }
}
