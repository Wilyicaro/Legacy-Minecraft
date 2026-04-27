package wily.legacy.mixin.base.client.beacon;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

import java.util.List;
import java.util.function.Consumer;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {

private static final ItemStack[] DISPLAY_ITEMS = new ItemStack[]{Items.NETHERITE_INGOT.getDefaultInstance(), Items.EMERALD.getDefaultInstance(), Items.DIAMOND.getDefaultInstance(), Items.GOLD_INGOT.getDefaultInstance(), Items.IRON_INGOT.getDefaultInstance()};
    @Shadow
    @Final
    private static Component PRIMARY_EFFECT_LABEL;

    @Shadow
    @Final
    private static Component SECONDARY_EFFECT_LABEL;
    @Shadow
    @Final
    private List<BeaconScreen.BeaconButton> beaconButtons;

    public BeaconScreenMixin(BeaconMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    private BeaconScreen self() {
        return (BeaconScreen) (Object) this;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 160 : 260);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(sd ? 160 : 255);
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
                LegacySlotDisplay.override(s, (sd ? 87 : 141), (sd ? 82 : 129), defaultDisplay);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, (sd ? 22 : 36) + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 98 : 155) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, (sd ? 22 : 36) + s.getContainerSlot() * slotsSize, (sd ? 140 : 223), defaultDisplay);
            }
        }
        this.beaconButtons.clear();
        BeaconScreen.BeaconConfirmButton confirmButton = self().new BeaconConfirmButton(this.leftPos + (sd ? 112 : 202), this.topPos + (sd ? 80 : 127));
        confirmButton.setSize(sd ? 16 : 22, sd ? 16 : 22);
        addRenderableWidget(confirmButton);
        beaconButtons.add(confirmButton);

        int j;
        int l;
        Holder<MobEffect> mobEffect;
        BeaconScreen.BeaconPowerButton beaconPowerButton;
        for (int i = 0; i <= 2; ++i) {
            j = getFrom(BeaconBlockEntity.BEACON_EFFECTS, i).size();
            for (l = 0; l < j; ++l) {
                mobEffect = getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS, i), l);
                beaconPowerButton = self().new BeaconPowerButton(this.leftPos + (sd ? 29 : 59) + (j > 1 ? l * (sd ? 20 : 27) : (sd ? 10 : 13)), this.topPos + (sd ? 19 : 38) + i * (sd ? 18 : 30), mobEffect, true, i);
                beaconPowerButton.active = false;
                addRenderableWidget(beaconPowerButton).setSize(sd ? 16 : 22, sd ? 16 : 22);
                beaconButtons.add(beaconPowerButton);
            }
        }

        j = getFrom(BeaconBlockEntity.BEACON_EFFECTS, 3).size() + 1;
        int k = j > 1 ? 0 : (sd ? 10 : 13);

        for (l = 0; l < j - 1; ++l) {
            mobEffect = getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS, 3), l);
            beaconPowerButton = self().new BeaconPowerButton(this.leftPos + (sd ? 98 : 164) + l * (sd ? 20 : 27) + k, this.topPos + (sd ? 37 : 68), mobEffect, false, 3);
            beaconPowerButton.active = false;
            addRenderableWidget(beaconPowerButton).setSize(sd ? 16 : 22, sd ? 16 : 22);
            beaconButtons.add(beaconPowerButton);
        }

        BeaconScreen.BeaconPowerButton beaconPowerButton2 = self().new BeaconUpgradePowerButton(this.leftPos + (sd ? 98 : 165) + (j - 1) * 24 - k / 2, this.topPos + (sd ? 37 : 68), getFrom(getFrom(BeaconBlockEntity.BEACON_EFFECTS, 0), 0));
        beaconPowerButton2.visible = false;
        addRenderableWidget(beaconPowerButton2).setSize(sd ? 16 : 22, sd ? 16 : 22);
        beaconButtons.add(beaconPowerButton2);
    }

    @Unique
     //? if <1.20.5 {
    /*private <T> T getFrom(T[] array, int i){
        return array[i];
    }
    *///?} else {
    private <T> T getFrom(List<T> list, int i) {
        return list.get(i);
    }
    //?}

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    public void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyFontUtil.applySDFont(b -> {
            int panelWidth = b ? 75 : 120;
            GuiGraphicsExtractor.text(this.font, PRIMARY_EFFECT_LABEL, (b ? 4 : 8) + (panelWidth - font.width(PRIMARY_EFFECT_LABEL)) / 2, (b ? 7 : 13), CommonColor.GRAY_TEXT.get(), false);
            GuiGraphicsExtractor.text(this.font, SECONDARY_EFFECT_LABEL, (b ? 80 : 132) + (panelWidth - font.width(SECONDARY_EFFECT_LABEL)) / 2, (b ? 7 : 13), CommonColor.GRAY_TEXT.get(), false);
        });
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int panelWidth = sd ? 75 : 120;
        int panelHeight = sd ? 75 : 115;
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + (sd ? 4 : 8), topPos + (sd ? 4 : 9), panelWidth, panelHeight);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + (sd ? 80 : 132), topPos + (sd ? 4 : 9), panelWidth, panelHeight);
        renderBeaconSprite(GuiGraphicsExtractor, LegacySprites.BEACON_1, leftPos + (sd ? 12 : 32), topPos + (sd ? 22 : 39));
        renderBeaconSprite(GuiGraphicsExtractor, LegacySprites.BEACON_2, leftPos + (sd ? 12 : 32), topPos + (sd ? 40 : 69));
        renderBeaconSprite(GuiGraphicsExtractor, LegacySprites.BEACON_3, leftPos + (sd ? 12 : 32), topPos + (sd ? 58 : 97));
        renderBeaconSprite(GuiGraphicsExtractor, LegacySprites.BEACON_4, leftPos + (sd ? 112 : 180), topPos + (sd ? 22 : 42));
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 9 : 15), topPos + (sd ? 81 : 129));
        GuiGraphicsExtractor.pose().scale(sd ? 13 / 18f : 1.125f, sd ? 13 / 18f : 1.125f);
        for (ItemStack displayItem : DISPLAY_ITEMS) {
            GuiGraphicsExtractor.item(displayItem, 0, 0);
            GuiGraphicsExtractor.pose().translate(18, 0);
        }
        GuiGraphicsExtractor.pose().popMatrix();
    }

    @Unique
    private void renderBeaconSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        if (LegacyOptions.getUIMode().isSD())
            graphics.pose().scale(0.5f);
        FactoryGuiGraphics.of(graphics).blitSprite(sprite, 0, 0, 20, 19);
        graphics.pose().popMatrix();
    }

    @Unique
    private void renderBeaconIcon(GuiGraphicsExtractor arg, AbstractWidget widget, Consumer<GuiGraphicsExtractor> render) {
        if (LegacyOptions.getUIMode().isSD()) {
            arg.pose().pushMatrix();
            arg.pose().translate(widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2);
            arg.pose().scale(0.5f);
            arg.pose().translate(-widget.getX() - 11, -widget.getY() - 11);
            render.accept(arg);
            arg.pose().popMatrix();
        } else
            render.accept(arg);
    }
}
