package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

import java.util.List;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {
    @Shadow @Final private List<BeaconScreen.BeaconButton> beaconButtons;

    @Shadow @Final private static Component PRIMARY_EFFECT_LABEL;

    @Shadow @Final private static Component SECONDARY_EFFECT_LABEL;

    public BeaconScreenMixin(BeaconMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    private BeaconScreen self(){
        return (BeaconScreen)(Object) this;
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
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 260;
        imageHeight = 255;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 141, 129);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 36 + (s.getContainerSlot() - 9) % 9 * 21,155 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 36 + s.getContainerSlot() * 21,223);
            }
        }
        this.beaconButtons.clear();
        BeaconScreen.BeaconConfirmButton confirmButton = self().new BeaconConfirmButton(this.leftPos + 202, this.topPos + 127){
            @Override
            protected void renderIcon(GuiGraphics guiGraphics) {
                RenderSystem.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_CONFIRM,this.getX() + 4, this.getY() + 4, 14, 14);
                RenderSystem.disableBlend();
            }
        };
        addRenderableWidget(confirmButton);
        beaconButtons.add(confirmButton);

        int j;
        int l;
        /*? if <1.20.5 {*//*MobEffect*//*?} else {*/Holder<MobEffect>/*?}*/ mobEffect;
        BeaconScreen.BeaconPowerButton beaconPowerButton;
        for(int i = 0; i <= 2; ++i) {
            j = getFrom(BeaconBlockEntity.BEACON_EFFECTS,i)./*? if <1.20.5 {*//*length*//*?} else {*/size()/*?}*/;
            for(l = 0; l < j; ++l) {
                mobEffect = getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS,i),l);
                beaconPowerButton = self().new BeaconPowerButton(this.leftPos + 59 + (j > 1 ? l * 27 : 13), this.topPos + 38 + i * 30, mobEffect, true, i);
                beaconPowerButton.active = false;
                addRenderableWidget(beaconPowerButton);
                beaconButtons.add(beaconPowerButton);
            }
        }

        j = getFrom(BeaconBlockEntity.BEACON_EFFECTS,3)./*? if <1.20.5 {*//*length*//*?} else {*/size()/*?}*/ + 1;
        int k = j > 1 ? 0 : 13;

        for(l = 0; l < j - 1; ++l) {
            mobEffect = getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS,3),l);
            beaconPowerButton = self().new BeaconPowerButton(this.leftPos + 164 + l * 27 + k, this.topPos + 68, mobEffect, false, 3);
            beaconPowerButton.active = false;
            addRenderableWidget(beaconPowerButton);
            beaconButtons.add(beaconPowerButton);
        }

        BeaconScreen.BeaconPowerButton beaconPowerButton2 = self().new BeaconUpgradePowerButton(this.leftPos + 165 + (j - 1) * 24 - k / 2, this.topPos + 68, getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS,0),0));
        beaconPowerButton2.visible = false;
        addRenderableWidget(beaconPowerButton2);
        beaconButtons.add(beaconPowerButton2);
    }
    @Unique
    //? if <1.20.5 {
    /*private <T> T getFrom(T[] array, int i){
        return array[i];
    }
    *///?} else {
    private <T> T getFrom(List<T> list, int i){
        return list.get(i);
    }
    //?}

    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        guiGraphics.drawString(this.font, PRIMARY_EFFECT_LABEL, 9 + (121 - font.width(PRIMARY_EFFECT_LABEL)) /2, 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.drawString(this.font, SECONDARY_EFFECT_LABEL, 133 + (121 - font.width(SECONDARY_EFFECT_LABEL)) /2, 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }
    private static final Item[] DISPLAY_ITEMS = new Item[]{Items.NETHERITE_INGOT,Items.EMERALD,Items.DIAMOND,Items.GOLD_INGOT,Items.IRON_INGOT};
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 8,topPos + 9,120, 115);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 132,topPos + 9,120, 115);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_1,leftPos + 32, topPos + 39, 20, 19);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_2,leftPos + 32, topPos + 69, 20, 19);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_3,leftPos + 32, topPos + 97, 20, 19);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_4,leftPos + 180, topPos + 42, 20, 19);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 15, topPos + 129, 100.0F);
        guiGraphics.pose().scale(7/6f, 7/6f,7/6f);
        for (Item displayItem : DISPLAY_ITEMS) {
            guiGraphics.renderItem(new ItemStack(displayItem), 0, 0);
            guiGraphics.pose().translate(18,0,0);
        }
        guiGraphics.pose().popPose();
    }
}
