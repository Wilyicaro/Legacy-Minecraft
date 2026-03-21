//? if >1.20.2 {
package wily.legacy.mixin.base.client.crafter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

@Mixin(CrafterScreen.class)
public abstract class CrafterScreenMixin extends AbstractContainerScreen<CrafterMenu> {


    @Shadow
    @Final
    private static Component DISABLED_SLOT_TOOLTIP;

    @Shadow
    @Final
    private static ResourceLocation POWERED_REDSTONE_LOCATION_SPRITE;

    @Shadow
    @Final
    private static ResourceLocation UNPOWERED_REDSTONE_LOCATION_SPRITE;

    public CrafterScreenMixin(CrafterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    //? if <1.21.11 {
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    public void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        super.renderSlot(guiGraphics, slot);
    }
    //?} else {
    /*@Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    public void renderSlot(GuiGraphics guiGraphics, Slot slot, int x, int y, CallbackInfo ci) {
        ci.cancel();
        super.renderSlot(guiGraphics, slot, x, y);
    }
    *///?}

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
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
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < 9) {
                LegacySlotDisplay.override(s, (sd ? 19 : 34) + s.getContainerSlot() % 3 * slotsSize, (sd ? 18 : 23) + s.getContainerSlot() / 3 * slotsSize, new LegacySlotDisplay() {
                    @Override
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return () -> (menu.isSlotDisabled(s.index) ? LegacySprites.DISABLED_CRAFTER_SLOT : LegacySprites.CRAFTER_SLOT);
                    }

                    @Override
                    public int getWidth() {
                        return slotsSize;
                    }
                });
            } else if (i < menu.slots.size() - 10) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 76 : 102) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else if (i < menu.slots.size() - 1) {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 120 : 171, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, sd ? 90 : 150, sd ? 28 : 39, new LegacySlotDisplay() {
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return () -> LegacySprites.NON_INTERACTIVE_RESULT_SLOT;
                    }

                    public int getWidth() {
                        return sd ? 21 : 32;
                    }
                });
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
        if (this.hoveredSlot instanceof CrafterSlot && !this.menu.isSlotDisabled(this.hoveredSlot.index) && this.menu.getCarried().isEmpty() && !this.hoveredSlot.hasItem())
            guiGraphics.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, i, j);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(menu.isPowered() ? POWERED_REDSTONE_LOCATION_SPRITE : UNPOWERED_REDSTONE_LOCATION_SPRITE, leftPos + (sd ? 65 : 105), topPos + (sd ? 28 : 43), sd ? 16 : 24, sd ? 16 : 24);
    }

}
//?}
