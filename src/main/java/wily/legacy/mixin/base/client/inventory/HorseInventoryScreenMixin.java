package wily.legacy.mixin.base.client.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(HorseInventoryScreen.class)
public abstract class HorseInventoryScreenMixin extends AbstractContainerScreen<HorseInventoryMenu> {
    @Shadow
    @Final
    private AbstractHorse horse;

    public HorseInventoryScreenMixin(HorseInventoryMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void init() {
        imageWidth = 215;
        imageHeight = 203;
        inventoryLabelX = 14;
        inventoryLabelY = 91;
        titleLabelX = 14;
        titleLabelY = 8;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 14, 21, new LegacySlotDisplay() {
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.getItem().isEmpty() ? LegacySprites.SADDLE_SLOT : null;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 14, 42, new LegacySlotDisplay() {
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.getItem().isEmpty() ? horse instanceof Llama ? LegacySprites.LLAMA_ARMOR_SLOT : LegacySprites.ARMOR_SLOT : null;
                    }
                });
            } else if (i < menu.slots.size() - 36) {
                int slotOffset = s.getContainerSlot();
                LegacySlotDisplay.override(s, 98 + slotOffset % horse.getInventoryColumns() * 21, 21 + slotOffset / horse.getInventoryColumns() * 21);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21, 104 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21, 174);
            }
        }
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(graphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL, leftPos + 34, topPos + 20, 63, 63);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 97, topPos + 20, 105, 63);
        LegacyRenderUtil.renderEntityInInventoryFollowsMouse(graphics, leftPos + 35, topPos + 21, leftPos + 95, topPos + 81, 25, 0.0625f, i, j, horse);

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
}
