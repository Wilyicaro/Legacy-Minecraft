package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

import static wily.legacy.util.LegacySprites.ARROW;
import static wily.legacy.util.LegacySprites.ERROR_CROSS;

@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
    public GrindstoneScreenMixin(GrindstoneMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 30;
        }
    };

    @Override
    protected void init() {
        imageWidth = 207;
        imageHeight = 215;
        inventoryLabelX = 10;
        inventoryLabelY = 105;
        titleLabelX = 10;
        titleLabelY = 11;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 41, 30, SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 41,65, SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 138, 46, SLOTS_DISPLAY);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 10 + (s.getContainerSlot() - 9) % 9 * 21,116 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 10 + s.getContainerSlot() * 21,185);
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

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos, imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 85,topPos + 50,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW,0,0,22,15);
        if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(2).hasItem())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(ERROR_CROSS, 2, 0, 15, 15);
        guiGraphics.pose().popPose();
    }
}
