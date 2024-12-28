package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {

    //? if >1.20.1
    @Shadow @Final private static Vector3f ARMOR_STAND_TRANSLATION;

    @Shadow @Final private static Quaternionf ARMOR_STAND_ANGLE;

    @Shadow private ArmorStand armorStandPreview;

    @Shadow @Final private CyclingSlotBackground templateIcon;

    @Shadow @Final private CyclingSlotBackground baseIcon;

    @Shadow @Final private CyclingSlotBackground additionalIcon;

    @Shadow protected abstract boolean hasRecipeError();

    public SmithingScreenMixin(SmithingMenu itemCombinerMenu, Inventory inventory, Component component, ResourceLocation resourceLocation) {
        super(itemCombinerMenu, inventory, component, resourceLocation);
    }

    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 30;
        }
    };

    @Override
    public void init() {
        imageWidth = 207;
        imageHeight = 215;
        inventoryLabelX = 10;
        inventoryLabelY = 105;
        titleLabelX = 56;
        titleLabelY = 25;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 10,60);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 31,60);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 52,60);
            } else if (i == 3) {
                LegacySlotDisplay.override(s, 127,56, SLOTS_DISPLAY);
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
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class), leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 13.5, topPos + 9.5,0f);
        guiGraphics.pose().scale(2.5f,2.5f,2.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMITHING_HAMMER,0,0,15,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 82, topPos + 59,0f);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,0,0,22,15);
        if (hasRecipeError())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
        InventoryScreen.renderEntityInInventory(guiGraphics, this.leftPos + 182, this.topPos + 95, 35/*? if >1.20.1 {*/, ARMOR_STAND_TRANSLATION/*?}*/, ARMOR_STAND_ANGLE, null, this.armorStandPreview);
    }

    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/ItemCombinerScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        this.templateIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.baseIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.additionalIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
    }
}
