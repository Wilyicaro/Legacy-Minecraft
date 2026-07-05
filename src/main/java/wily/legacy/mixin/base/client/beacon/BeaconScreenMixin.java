package wily.legacy.mixin.base.client.beacon;

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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

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
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 160 : 260;
        imageHeight = sd ? 160 : 255;
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
                LegacySlotDisplay.override(s, sd ? 87 : 141, sd ? 82 : 129, defaultDisplay);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, (sd ? 22 : 36) + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 98 : 155) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, (sd ? 22 : 36) + s.getContainerSlot() * slotsSize, sd ? 140 : 223, defaultDisplay);
            }
        }
        this.beaconButtons.clear();
        BeaconScreen.BeaconConfirmButton confirmButton = self().new BeaconConfirmButton(this.leftPos + (sd ? 112 : 202), this.topPos + (sd ? 80 : 127)){
            @Override
            protected void renderIcon(GuiGraphics guiGraphics) {
                FactoryScreenUtil.enableBlend();
                int iconSize = LegacyOptions.getUIMode().isSD() ? 10 : 14;
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BEACON_CONFIRM,this.getX() + (this.getWidth() - iconSize) / 2, this.getY() + (this.getHeight() - iconSize) / 2, iconSize, iconSize);
                FactoryScreenUtil.disableBlend();
            }
        };
        resizeButton(confirmButton, sd ? 16 : 22);
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
                beaconPowerButton = self().new BeaconPowerButton(this.leftPos + (sd ? 29 : 59) + (j > 1 ? l * (sd ? 20 : 27) : (sd ? 10 : 13)), this.topPos + (sd ? 19 : 38) + i * (sd ? 18 : 30), mobEffect, true, i) {
                    @Override
                    protected void renderIcon(GuiGraphics guiGraphics) {
                        renderBeaconIcon(guiGraphics, this, () -> super.renderIcon(guiGraphics));
                    }
                };
                beaconPowerButton.active = false;
                resizeButton(beaconPowerButton, sd ? 16 : 22);
                addRenderableWidget(beaconPowerButton);
                beaconButtons.add(beaconPowerButton);
            }
        }

        j = getFrom(BeaconBlockEntity.BEACON_EFFECTS,3)./*? if <1.20.5 {*//*length*//*?} else {*/size()/*?}*/ + 1;
        int k = j > 1 ? 0 : sd ? 10 : 13;

        for(l = 0; l < j - 1; ++l) {
            mobEffect = getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS,3),l);
            beaconPowerButton = self().new BeaconPowerButton(this.leftPos + (sd ? 98 : 164) + l * (sd ? 20 : 27) + k, this.topPos + (sd ? 37 : 68), mobEffect, false, 3) {
                @Override
                protected void renderIcon(GuiGraphics guiGraphics) {
                    renderBeaconIcon(guiGraphics, this, () -> super.renderIcon(guiGraphics));
                }
            };
            beaconPowerButton.active = false;
            resizeButton(beaconPowerButton, sd ? 16 : 22);
            addRenderableWidget(beaconPowerButton);
            beaconButtons.add(beaconPowerButton);
        }

        BeaconScreen.BeaconPowerButton beaconPowerButton2 = self().new BeaconUpgradePowerButton(this.leftPos + (sd ? 98 : 165) + (j - 1) * 24 - k / 2, this.topPos + (sd ? 37 : 68), getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS,0),0)) {
            @Override
            protected void renderIcon(GuiGraphics guiGraphics) {
                renderBeaconIcon(guiGraphics, this, () -> super.renderIcon(guiGraphics));
            }
        };
        beaconPowerButton2.visible = false;
        resizeButton(beaconPowerButton2, sd ? 16 : 22);
        addRenderableWidget(beaconPowerButton2);
        beaconButtons.add(beaconPowerButton2);
    }

    @Unique
    private void resizeButton(AbstractWidget widget, int size) {
        widget.setWidth(size);
        //? if <=1.20.1 {
        /*((WidgetAccessor) widget).setHeight(size);
        *///?} else {
        widget.setHeight(size);
        //?}
    }

    @Unique
    private void renderBeaconIcon(GuiGraphics guiGraphics, AbstractWidget widget, Runnable render) {
        if (!LegacyOptions.getUIMode().isSD()) {
            render.run();
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(widget.getX() + widget.getWidth() / 2f, widget.getY() + widget.getHeight() / 2f, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
        guiGraphics.pose().translate(-widget.getX() - 11f, -widget.getY() - 11f, 0);
        render.run();
        guiGraphics.pose().popPose();
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
        ScreenUtil.applySDFont(sd -> {
            int panelWidth = sd ? 75 : 120;
            guiGraphics.drawString(this.font, PRIMARY_EFFECT_LABEL, (sd ? 4 : 9) + (panelWidth - font.width(PRIMARY_EFFECT_LABEL)) /2, sd ? 7 : 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.drawString(this.font, SECONDARY_EFFECT_LABEL, (sd ? 80 : 133) + (panelWidth - font.width(SECONDARY_EFFECT_LABEL)) /2, sd ? 7 : 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }
    private static final Item[] DISPLAY_ITEMS = new Item[]{Items.NETHERITE_INGOT,Items.EMERALD,Items.DIAMOND,Items.GOLD_INGOT,Items.IRON_INGOT};
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int panelWidth = sd ? 75 : 120;
        int panelHeight = sd ? 75 : 115;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + (sd ? 4 : 8),topPos + (sd ? 4 : 9),panelWidth, panelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + (sd ? 80 : 132),topPos + (sd ? 4 : 9),panelWidth, panelHeight);
        renderBeaconSprite(guiGraphics, LegacySprites.BEACON_1,leftPos + (sd ? 12 : 32), topPos + (sd ? 22 : 39));
        renderBeaconSprite(guiGraphics, LegacySprites.BEACON_2,leftPos + (sd ? 12 : 32), topPos + (sd ? 40 : 69));
        renderBeaconSprite(guiGraphics, LegacySprites.BEACON_3,leftPos + (sd ? 12 : 32), topPos + (sd ? 58 : 97));
        renderBeaconSprite(guiGraphics, LegacySprites.BEACON_4,leftPos + (sd ? 112 : 180), topPos + (sd ? 22 : 42));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + (sd ? 9 : 15), topPos + (sd ? 81 : 129), 100.0F);
        guiGraphics.pose().scale(sd ? 13 / 18f : 1.125f, sd ? 13 / 18f : 1.125f,sd ? 13 / 18f : 1.125f);
        for (Item displayItem : DISPLAY_ITEMS) {
            guiGraphics.renderItem(new ItemStack(displayItem), 0, 0);
            guiGraphics.pose().translate(18,0,0);
        }
        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderBeaconSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        if (LegacyOptions.getUIMode().isSD()) guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sprite, 0, 0, 20, 19);
        guiGraphics.pose().popPose();
    }
}
