package wily.legacy.mixin.base.client.cartography;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
 //?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.util.ScreenUtil;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 23;
        }
    };
    private static final LegacySlotDisplay SD_SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 15;
        }
    };

    private EditBox name;
    private boolean updatingName;
    private ItemStack lastInput = ItemStack.EMPTY;
    private final ContainerListener listener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (i == 0) {
                ItemStack input = itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copyWithCount(1);
                if (!ItemStack.matches(input, lastInput) && !name.isFocused()) {
                    setNameValue(itemStack.isEmpty() ? "" : RenameItemMenu.getItemName(itemStack));
                }
                lastInput = input;
                name.setEditable(!itemStack.isEmpty());
            }
        }
        @Override
        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };

    public CartographyTableScreenMixin(CartographyTableMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    @Override
    public void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 207;
        imageHeight = sd ? 165 : 254;
        inventoryLabelX = sd ? 7 : 10;
        inventoryLabelY = sd ? 96 : 144;
        ScreenUtil.applySDFont(ignored -> titleLabelX = (imageWidth - font.width(getTitle())) / 2);
        titleLabelY = sd ? 5 : 10;
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
                LegacySlotDisplay.override(s, inventoryLabelX, sd ? 41 : 62, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s,inventoryLabelX, sd ? 70 : 105, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 110 : 166, sd ? 54 : 82,new LegacySlotDisplay(){

                    public int getWidth() {
                        return sd ? 16 : 27;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 104 : 156) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 148 : 225, defaultDisplay);
            }
        }
        this.name = new EditBox(this.font, leftPos + inventoryLabelX, topPos + (sd ? 25 : 38), sd ? 70 : 120, sd ? 13 : 18, Component.empty());
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setMaxLength(50);
        this.name.setResponder(s-> {
            if (updatingName) {
                return;
            }
            Slot slot = menu.getSlot(0);
            if (!slot.hasItem())
                return;

            if (!FactoryItemUtil.hasCustomName(slot.getItem()) && s.equals(RenameItemMenu.getItemName(slot.getItem()))) {
                s = "";
            }
            ((RenameItemMenu)menu).setResultItemName(s);
            minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
        });
        setNameValue("");
        this.addWidget(this.name);
        this.name.setEditable(this.menu.getSlot(0).hasItem());
        menu.addSlotListener(listener);
    }
    public Component getCartographyAction(){
        ItemStack input = menu.getSlot(0).getItem();
        ItemStack input2 = menu.getSlot(1).getItem();
        if (canUsePaperConversion(input, input2)) {
            return LegacyComponents.BASIC_MAP;
        }
        if (input.is(Items.FILLED_MAP)){
            if (input2.is(Items.PAPER)) return LegacyComponents.ZOOM_MAP;
            else if (input2.is(Items.MAP)) return LegacyComponents.COPY_MAP;
            else if (input2.is(Items.GLASS_PANE)) return LegacyComponents.LOCK_MAP;
            return LegacyComponents.RENAME_MAP;
        }
        return null;
    }

    private boolean canUsePaperConversion(ItemStack input, ItemStack input2) {
        return Legacy4JClient.hasModOnServer() && input.is(Items.PAPER) && input2.isEmpty();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i != 256 && (this.name.keyPressed(i, j, k) || this.name.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        boolean result = super.mouseClicked(d, e, i);
        if (name.isFocused() && !name.isMouseOver(d, e)) {
            setFocused(null);
            name.setFocused(false);
        }
        return result;
    }

    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        ScreenUtil.applySDFont(sd -> {
            super.renderLabels(guiGraphics, i, j);
            guiGraphics.drawString(font, LegacyComponents.MAP_NAME, inventoryLabelX, sd ? 18 : 27, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
            Component cartographyAction = getCartographyAction();
            if (cartographyAction != null) guiGraphics.drawString(font,cartographyAction,(imageWidth - font.width(cartographyAction)) / 2,sd ? 86 : 130,CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        });

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

    @Override
    public void repositionElements() {
        String string = this.name.getValue();
        super.repositionElements();
        setNameValue(string);
    }

    private void setNameValue(String value) {
        updatingName = true;
        name.setValue(value);
        updatingName = false;
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        name.render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.COMBINER_PLUS,leftPos + (sd ? 7 : 14), topPos + (sd ? 56 : 88),13,13);
        ItemStack input2 = menu.getSlot(1).getItem();
        boolean bl = input2.is(Items.MAP);
        boolean bl2 = input2.is(Items.PAPER);
        boolean bl3 = input2.is(Items.GLASS_PANE);
        ItemStack input = menu.getSlot(0).getItem();
        int arrowWidth = sd ? 16 : 22;
        int arrowHeight = sd ? 14 : 15;
        int arrowY = topPos + (sd ? 55 : 87);
        ResourceLocation arrowSprite = sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(arrowSprite,leftPos + (sd ? 24 : 36), arrowY,arrowWidth,arrowHeight);
        ResourceLocation cartographySprite;
        if (input.is(Items.FILLED_MAP)) {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(/*? if <1.20.5 {*//*MapItem.getMapId(input)*//*?} else {*/input.get(DataComponents.MAP_ID)/*?}*/, this.minecraft.level);
            if (mapItemSavedData != null && (mapItemSavedData.locked && (bl2 || bl3) || bl2 && mapItemSavedData.scale >= 4))
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, leftPos + (sd ? 26 : 40), arrowY, 15, 15);
            cartographySprite = bl ? LegacySprites.CARTOGRAPHY_TABLE_COPY : bl2 ? LegacySprites.CARTOGRAPHY_TABLE_ZOOM : bl3 ? LegacySprites.CARTOGRAPHY_TABLE_LOCKED : LegacySprites.CARTOGRAPHY_TABLE_MAP;
        } else if (canUsePaperConversion(input, input2))
            cartographySprite = LegacySprites.CARTOGRAPHY_TABLE_MAP;
        else
            cartographySprite = LegacySprites.CARTOGRAPHY_TABLE;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(cartographySprite,leftPos + (sd ? 43 : 70), topPos + (sd ? 40 : 61),sd ? 44 : 66,sd ? 44 : 66);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(arrowSprite,leftPos + (sd ? 91 : 139), arrowY,arrowWidth,arrowHeight);
    }
}
