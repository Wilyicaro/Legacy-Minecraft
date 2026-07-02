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
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.RenameItemMenu;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
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
        imageWidth = 207;
        imageHeight = 254;
        inventoryLabelX = 10;
        inventoryLabelY = 144;
        titleLabelX = (imageWidth - font.width(getTitle())) / 2;
        titleLabelY = 10;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 10, 62,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 23;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s,10, 105,new LegacySlotDisplay(){

                    public int getWidth() {
                        return 23;
                    }
                });
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 166, 82,new LegacySlotDisplay(){

                    public int getWidth() {
                        return 27;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 10 + (s.getContainerSlot() - 9) % 9 * 21,156 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 10 + s.getContainerSlot() * 21,225);
            }
        }
        this.name = new EditBox(this.font, leftPos + 10, topPos + 38, 120, 18, Component.empty());
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
        super.renderLabels(guiGraphics, i, j);
        guiGraphics.drawString(font, LegacyComponents.MAP_NAME,10,27, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        Component cartographyAction = getCartographyAction();
        if (cartographyAction != null) guiGraphics.drawString(font,cartographyAction,(imageWidth - font.width(cartographyAction)) / 2,130,CommonColor.INVENTORY_GRAY_TEXT.get(),false);

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
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        name.render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.COMBINER_PLUS,leftPos + 14, topPos + 88,13,13);
        ItemStack input2 = menu.getSlot(1).getItem();
        boolean bl = input2.is(Items.MAP);
        boolean bl2 = input2.is(Items.PAPER);
        boolean bl3 = input2.is(Items.GLASS_PANE);
        ItemStack input = menu.getSlot(0).getItem();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,leftPos + 36, topPos + 87,22,15);
        ResourceLocation cartographySprite;
        if (input.is(Items.FILLED_MAP)) {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(/*? if <1.20.5 {*//*MapItem.getMapId(input)*//*?} else {*/input.get(DataComponents.MAP_ID)/*?}*/, this.minecraft.level);
            if (mapItemSavedData != null && (mapItemSavedData.locked && (bl2 || bl3) || bl2 && mapItemSavedData.scale >= 4))
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, leftPos + 40, topPos + 87, 15, 15);
            cartographySprite = bl ? LegacySprites.CARTOGRAPHY_TABLE_COPY : bl2 ? LegacySprites.CARTOGRAPHY_TABLE_ZOOM : bl3 ? LegacySprites.CARTOGRAPHY_TABLE_LOCKED : LegacySprites.CARTOGRAPHY_TABLE_MAP;
        } else if (canUsePaperConversion(input, input2))
            cartographySprite = LegacySprites.CARTOGRAPHY_TABLE_MAP;
        else
            cartographySprite = LegacySprites.CARTOGRAPHY_TABLE;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(cartographySprite,leftPos + 70, topPos + 61,66,66);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,leftPos + 139, topPos + 87,22,15);
    }
}
