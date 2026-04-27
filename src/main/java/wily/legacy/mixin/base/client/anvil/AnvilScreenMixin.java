package wily.legacy.mixin.base.client.anvil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacySoundUtil;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {

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
    @Shadow
    @Final
    private static Component TOO_EXPENSIVE_TEXT;
    @Shadow
    private EditBox name;
    @Shadow
    @Final
    private Player player;

    public AnvilScreenMixin(AnvilMenu itemCombinerMenu, Inventory inventory, Component component, Identifier resourceLocation) {
        super(itemCombinerMenu, inventory, component, resourceLocation);
    }

    @Shadow
    protected abstract void onNameChanged(String string);

    @Override
    public void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 130 : 207);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(sd ? 140 : 215);
        inventoryLabelX = sd ? 7 : 10;
        inventoryLabelY = sd ? 67 : 105;
        titleLabelX = sd ? 54 : 73;
        titleLabelY = sd ? 6 : 11;
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
                LegacySlotDisplay.override(s, sd ? 10 : 15, sd ? 36 : 56, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 53 : 84, sd ? 36 : 56, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 104 : 167, sd ? 36 : 56, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 77 : 116) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, (sd ? 122 : 185), defaultDisplay);
            }
        }
    }

    @Inject(method = "setInitialFocus", at = @At("HEAD"), cancellable = true)
    protected void setInitialFocus(CallbackInfo ci) {
        ci.cancel();
        super.setInitialFocus();
    }

    @Override
    public void repositionElements() {
        String string = this.name.getValue();
        super.repositionElements();
        this.name.setValue(string);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    public void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyFontUtil.applySDFont(b -> {
            super.extractLabels(GuiGraphicsExtractor, i, j);
            int k = this.menu.getCost();
            if (k > 0) {
                Component component;
                int l = CommonColor.EXPERIENCE_TEXT.get();
                if (k >= 40 && !this.minecraft.player.getAbilities().instabuild) {
                    component = TOO_EXPENSIVE_TEXT;
                    l = CommonColor.ANVIL_ERROR_TEXT.get();
                } else if (!this.menu.getSlot(2).hasItem()) {
                    component = null;
                } else {
                    component = Component.translatable("container.repair.cost", k);
                    if (!this.menu.getSlot(2).mayPickup(this.player)) {
                        l = CommonColor.ANVIL_ERROR_TEXT.get();
                    }
                }
                if (component != null) {
                    int m = this.imageWidth - 8 - this.font.width(component) - 2;
                    GuiGraphicsExtractor.text(this.font, component, m, b ? 58 : 90, l);
                }
            }
        });
    }

    @Inject(method = "subInit", at = @At("HEAD"), cancellable = true)
    public void subInit(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        this.name = new EditBox(this.font, leftPos + (sd ? 47 : 72), topPos + (sd ? 16 : 26), sd ? 70 : 120, sd ? 13 : 18, Component.translatable("container.repair"));
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setMaxLength(50);
        this.name.setResponder(this::onNameChanged);
        this.name.setValue("");
        this.addRenderableWidget(this.name);
        this.name.setEditable(this.menu.getSlot(0).hasItem());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 6.5f : 13.5f), topPos + (sd ? 3.5f : 9.5f));
        GuiGraphicsExtractor.pose().scale(sd ? 2.0f : 2.5f, sd ? 2.0f : 2.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.ANVIL_HAMMER, 0, 0, 15, 15);
        GuiGraphicsExtractor.pose().popMatrix();
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 34.5f : 53), topPos + (sd ? 39 : 60));
        if (!sd) GuiGraphicsExtractor.pose().scale(1.5f, 1.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.COMBINER_PLUS, 0, 0, 13, 13);
        GuiGraphicsExtractor.pose().popMatrix();
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 81 : 122), topPos + (sd ? 38 : 59));
        if (!sd) GuiGraphicsExtractor.pose().scale(1.5f, 1.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, sd ? 16 : 22, sd ? 14 : 15);
        if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem())
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        GuiGraphicsExtractor.pose().popMatrix();
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;closeContainer()V"))
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        LegacySoundUtil.playBackSound();
    }

    @Redirect(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component slotChanged(ItemStack itemStack) {
        return Component.literal(RenameItemMenu.getItemName(itemStack));
    }
}
