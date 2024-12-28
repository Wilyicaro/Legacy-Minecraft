package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {

    @Shadow private EditBox name;

    @Shadow protected abstract void onNameChanged(String string);

    @Shadow @Final private static Component TOO_EXPENSIVE_TEXT;

    @Shadow @Final private Player player;

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
        titleLabelX = 73;
        titleLabelY = 11;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 15,56, SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 84,56, SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 167,56, SLOTS_DISPLAY);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 10 + (s.getContainerSlot() - 9) % 9 * 21,116 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 10 + s.getContainerSlot() * 21,185);
            }
        }
    }
    public AnvilScreenMixin(AnvilMenu itemCombinerMenu, Inventory inventory, Component component, ResourceLocation resourceLocation) {
        super(itemCombinerMenu, inventory, component, resourceLocation);
    }

    //? if >=1.20.5 {
    @Inject(method = "setInitialFocus",at = @At("HEAD"), cancellable = true)
    protected void setInitialFocus(CallbackInfo ci) {
        ci.cancel();
        super.setInitialFocus();
    }
    //?}

    @Override
    public void repositionElements() {
        String string = this.name.getValue();
        super.repositionElements();
        this.name.setValue(string);
    }

    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        super.renderLabels(guiGraphics, i, j);
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
                guiGraphics.drawString(this.font, component, m, 90, l);
            }
        }
    }
    @Inject(method = "subInit",at = @At("HEAD"), cancellable = true)
    public void subInit(CallbackInfo ci) {
        ci.cancel();
        this.name = new EditBox(this.font, leftPos + 72, topPos + 26, 120, 18, Component.translatable("container.repair"));
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setMaxLength(50);
        this.name.setResponder(this::onNameChanged);
        this.name.setValue("");
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(this.menu.getSlot(0).hasItem());
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
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 13.5, topPos + 9.5,0f);
        guiGraphics.pose().scale(2.5f,2.5f,2.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ANVIL_HAMMER,0,0,15,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 53, topPos + 60,0f);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.COMBINER_PLUS,0,0,13,13);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 122, topPos + 59,0f);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,0,0,22,15);
        if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
    }
}
