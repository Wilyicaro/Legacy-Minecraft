//? if >=1.20.2 {
package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

@Mixin(CrafterScreen.class)
public abstract class CrafterScreenMixin extends AbstractContainerScreen<CrafterMenu> {



    @Shadow @Final private static Component DISABLED_SLOT_TOOLTIP;

    @Shadow @Final private static ResourceLocation POWERED_REDSTONE_LOCATION_SPRITE;

    @Shadow @Final private static ResourceLocation UNPOWERED_REDSTONE_LOCATION_SPRITE;

    public CrafterScreenMixin(CrafterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "renderSlot",at = @At("HEAD"), cancellable = true)
    public void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        super.renderSlot(guiGraphics, slot);
    }
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
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < 9) {
                LegacySlotDisplay.override(s, 34 + s.getContainerSlot() % 3 * 21,23 + s.getContainerSlot() / 3 * 21,new LegacySlotDisplay() {
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return ()-> (menu.isSlotDisabled(s.index) ? LegacySprites.DISABLED_CRAFTER_SLOT : LegacySprites.CRAFTER_SLOT);
                    }
                });
            } else if (i < menu.slots.size() - 10) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,102 + (s.getContainerSlot() - 9) / 9 * 21);
            } else if (i < menu.slots.size() - 1){
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,171);
            } else {
                LegacySlotDisplay.override(s, 150, 39,new LegacySlotDisplay(){
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return ()-> LegacySprites.NON_INTERACTIVE_RESULT_SLOT;
                    }
                    public int getWidth() {return 32;}
                });
            }
        }
    }

    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
        if (this.hoveredSlot instanceof CrafterSlot && !this.menu.isSlotDisabled(this.hoveredSlot.index) && this.menu.getCarried().isEmpty() && !this.hoveredSlot.hasItem()) guiGraphics.renderTooltip(this.font, DISABLED_SLOT_TOOLTIP, i, j);
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(menu.isPowered() ? POWERED_REDSTONE_LOCATION_SPRITE : UNPOWERED_REDSTONE_LOCATION_SPRITE,leftPos + 105,topPos + 43,24,24);
    }

}
//?}
