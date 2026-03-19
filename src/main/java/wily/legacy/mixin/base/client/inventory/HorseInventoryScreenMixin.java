package wily.legacy.mixin.base.client.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

//? if >=1.21.11 {
@Mixin(AbstractMountInventoryScreen.class)
//?} else {
/*@Mixin(HorseInventoryScreen.class)
*///?}
public abstract class HorseInventoryScreenMixin extends AbstractContainerScreen<HorseInventoryMenu> {
    @Shadow
    @Final
    //? if >=1.21.11 {
    protected LivingEntity mount;

    @Unique
    AbstractHorse horse = (AbstractHorse) mount;
    //?} else {
    /*private AbstractHorse horse;
     *///?}

    @Unique
    private static Vec2 SD_SLOTS_OFFSET = new Vec2(0.5f, 0.5f);

    public HorseInventoryScreenMixin(HorseInventoryMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 215;
        imageHeight = sd ? 140 : 203;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 66 : 91;
        titleLabelX = sd ? 7 : 14;
        titleLabelY = sd ? 6 : 8;
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
                LegacySlotDisplay.override(s, inventoryLabelX, sd ? 16 : 21, new LegacySlotDisplay() {
                    @Override
                    public Identifier getIconSprite() {
                        return s.getItem().isEmpty() ? LegacySprites.SADDLE_SLOT : null;
                    }

                    @Override
                    public int getWidth() {
                        return slotsSize;
                    }

                    @Override
                    public Vec2 getOffset() {
                        return sd ? SD_SLOTS_OFFSET : Vec2.ZERO;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, inventoryLabelX, sd ? 29 : 42, new LegacySlotDisplay() {
                    @Override
                    public Identifier getIconSprite() {
                        return s.getItem().isEmpty() ? horse instanceof Llama ? LegacySprites.LLAMA_ARMOR_SLOT : LegacySprites.ARMOR_SLOT : null;
                    }

                    @Override
                    public int getWidth() {
                        return slotsSize;
                    }

                    @Override
                    public Vec2 getOffset() {
                        return sd ? SD_SLOTS_OFFSET : Vec2.ZERO;
                    }
                });
            } else if (i < menu.slots.size() - 36) {
                int slotOffset = s.getContainerSlot();
                LegacySlotDisplay.override(s, (sd ? 60 : 98) + slotOffset % horse.getInventoryColumns() * slotsSize, (sd ? 18 : 21) + slotOffset / horse.getInventoryColumns() * slotsSize, defaultDisplay);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 76 : 104) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 120 : 174, defaultDisplay);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(graphics).blitSprite(UIAccessor.of(this).getIdentifier("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);

        int entityPanelX = leftPos + (sd ? 20 : 34);
        int entityPanelY = topPos + (sd ? 16 : 20);
        int entityPanelSize = sd ? 39 : 63;

        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL, entityPanelX, entityPanelY, entityPanelSize, entityPanelSize);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + (sd ? 59 : 97), topPos + (sd ? 16 : 20), sd ? 65 : 105, sd ? 39 : 63);
        LegacyRenderUtil.renderEntityInInventoryFollowsMouse(graphics, entityPanelX + 2, entityPanelY + 2, entityPanelX + entityPanelSize - 2, entityPanelY + entityPanelSize - 2, sd ? 15 : 25, 0.0625f, i, j, horse);

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
}
