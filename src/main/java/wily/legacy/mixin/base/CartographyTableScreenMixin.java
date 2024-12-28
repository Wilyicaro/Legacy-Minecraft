package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.RenameItemMenu;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
    private static final Component MAP_NAME = Component.translatable("legacy.container.mapName");
    private static final Component RENAME_MAP = Component.translatable("legacy.container.renameMap");
    private static final Component ZOOM = Component.translatable("legacy.container.zoomMap");
    private static final Component COPY = Component.translatable("legacy.container.copyMap");
    private static final Component LOCK = Component.translatable("legacy.container.lockMap");

    private EditBox name;
    private final ContainerListener listener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (i == 0) {
                name.setValue(itemStack.isEmpty() ? "" : itemStack.getHoverName().getString());
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
        menu.addSlotListener(listener);
        this.name = new EditBox(this.font, leftPos + 10, topPos + 38, 120, 18, Component.empty());
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setMaxLength(50);
        this.name.setResponder(s-> {
            Slot slot = menu.getSlot(0);
            if (!slot.hasItem())
                return;

            if (!RenameItemMenu.hasCustomName(slot.getItem()) && s.equals(slot.getItem().getHoverName().getString())) {
                s = "";
            }
            ((RenameItemMenu)menu).setResultItemName(s);
            minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
        });
        this.name.setValue("");
        this.addWidget(this.name);
        this.name.setEditable(this.menu.getSlot(0).hasItem());
    }
    public Component getCartographyAction(){
        ItemStack input = menu.getSlot(0).getItem();
        ItemStack input2 = menu.getSlot(1).getItem();
        if (input.is(Items.FILLED_MAP)){
            if (input2.is(Items.PAPER)) return ZOOM;
            else if (input2.is(Items.MAP)) return COPY;
            else if (input2.is(Items.GLASS_PANE)) return LOCK;
            return RENAME_MAP;
        }
        return null;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i != 256 && (this.name.keyPressed(i, j, k) || this.name.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(i, j, k);
    }


    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        guiGraphics.drawString(font,MAP_NAME,10,27, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
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
        this.name.setValue(string);
    }
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        name.render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.COMBINER_PLUS,leftPos + 14, topPos + 88,13,13);
        ItemStack input2 = menu.getSlot(1).getItem();
        boolean bl = input2.is(Items.MAP);
        boolean bl2 = input2.is(Items.PAPER);
        boolean bl3 = input2.is(Items.GLASS_PANE);
        ItemStack input = menu.getSlot(0).getItem();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,leftPos + 36, topPos + 87,22,15);
        if (input.is(Items.FILLED_MAP)) {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(/*? if <1.20.5 {*//*MapItem.getMapId(input)*//*?} else {*/input.get(DataComponents.MAP_ID)/*?}*/, this.minecraft.level);
            if (mapItemSavedData != null && (mapItemSavedData.locked && (bl2 || bl3) || bl2 && mapItemSavedData.scale >= 4))
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, leftPos + 40, topPos + 87, 15, 15);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(bl ? LegacySprites.CARTOGRAPHY_TABLE_COPY : bl2 ? LegacySprites.CARTOGRAPHY_TABLE_ZOOM : bl3 ? LegacySprites.CARTOGRAPHY_TABLE_LOCKED : LegacySprites.CARTOGRAPHY_TABLE_MAP,leftPos + 70, topPos + 61,66,66);
        }else{
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.CARTOGRAPHY_TABLE,leftPos + 70, topPos + 61,66,66);
        }
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,leftPos + 139, topPos + 87,22,15);
    }
}
