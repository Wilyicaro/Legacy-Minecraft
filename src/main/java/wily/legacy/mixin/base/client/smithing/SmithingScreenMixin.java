package wily.legacy.mixin.base.client.smithing;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {

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
    //? if >1.20.1
    @Shadow
    @Final
    private static Vector3f ARMOR_STAND_TRANSLATION;
    @Shadow
    @Final
    private static Quaternionf ARMOR_STAND_ANGLE;
    @Shadow
    private ArmorStand armorStandPreview;
    @Shadow
    @Final
    private CyclingSlotBackground templateIcon;
    @Shadow
    @Final
    private CyclingSlotBackground baseIcon;
    @Shadow
    @Final
    private CyclingSlotBackground additionalIcon;

    public SmithingScreenMixin(SmithingMenu itemCombinerMenu, Inventory inventory, Component component, ResourceLocation resourceLocation) {
        super(itemCombinerMenu, inventory, component, resourceLocation);
    }

    @Shadow
    protected abstract boolean hasRecipeError();

    @Override
    public void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 207;
        imageHeight = sd ? 140 : 215;
        inventoryLabelX = sd ? 7 : 10;
        inventoryLabelY = sd ? 67 : 105;
        titleLabelX = sd ? 54 : 56;
        titleLabelY = sd ? 6 : 25;
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
                LegacySlotDisplay.override(s, sd ? 7 : 10, sd ? 40 : 60, defaultDisplay);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 20 : 31, sd ? 40 : 60, defaultDisplay);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 33 : 52, sd ? 40 : 60, defaultDisplay);
            } else if (i == 3) {
                LegacySlotDisplay.override(s, sd ? 80 : 127, sd ? 36 : 56, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 77 : 116) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, (sd ? 122 : 185), defaultDisplay);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
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
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + (sd ? 6.5f : 13.5f), topPos + (sd ? 3.5f : 9.5f));
        guiGraphics.pose().scale(sd ? 2.0f : 2.5f, sd ? 2.0f : 2.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMITHING_HAMMER, 0, 0, 15, 15);
        guiGraphics.pose().popMatrix();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + (sd ? 54 : 82), topPos + (sd ? 38 : 59));
        if (!sd) guiGraphics.pose().scale(1.5f, 1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, sd ? 16 : 22, sd ? 14 : 15);
        if (hasRecipeError())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popMatrix();
        InventoryScreen.renderEntityInInventory(guiGraphics, this.leftPos, this.topPos, this.leftPos + (sd ? 228 : 364), this.topPos + (sd ? 100 : 150), sd ? 20 : 35, ARMOR_STAND_TRANSLATION, ARMOR_STAND_ANGLE, null, this.armorStandPreview);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/ItemCombinerScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        this.templateIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.baseIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.additionalIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
    }
}
