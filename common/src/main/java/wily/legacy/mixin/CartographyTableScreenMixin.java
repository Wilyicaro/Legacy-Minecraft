package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
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
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.util.ScreenUtil;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
    private static final Component MAP_NAME = Component.translatable("legacy.container.mapName");
    private static final Component RENAME_MAP = Component.translatable("legacy.container.renameMap");
    private static final Component ZOOM = Component.translatable("legacy.container.zoomMap");
    private static final Component COPY = Component.translatable("legacy.container.copyMap");
    private static final Component LOCK = Component.translatable("legacy.container.lockMap");

    private EditBox name;
    private ContainerListener listener =  new ContainerListener() {
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

            if (!slot.getItem().hasCustomHoverName() && s.equals(slot.getItem().getHoverName().getString())) {
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
        guiGraphics.drawString(font,MAP_NAME,10,27,0x383838,false);
        Component cartographyAction = getCartographyAction();
        if (cartographyAction != null) guiGraphics.drawString(font,cartographyAction,(imageWidth - font.width(cartographyAction)) / 2,130,0x383838,false);

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    public void resize(Minecraft minecraft, int i, int j) {
        String string = this.name.getValue();
        this.init(minecraft, i, j);
        this.name.setValue(string);
    }
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        name.render(guiGraphics, i, j, f);
        guiGraphics.blitSprite(LegacySprites.COMBINER_PLUS,leftPos + 14, topPos + 88,13,13);
        ItemStack input2 = menu.getSlot(1).getItem();
        boolean bl = input2.is(Items.MAP);
        boolean bl2 = input2.is(Items.PAPER);
        boolean bl3 = input2.is(Items.GLASS_PANE);
        ItemStack input = menu.getSlot(0).getItem();
        guiGraphics.blitSprite(LegacySprites.ARROW,leftPos + 36, topPos + 87,22,15);
        if (input.is(Items.FILLED_MAP)) {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(MapItem.getMapId(input), this.minecraft.level);
            if (mapItemSavedData != null && (mapItemSavedData.locked && (bl2 || bl3) || bl2 && mapItemSavedData.scale >= 4))
                guiGraphics.blitSprite(LegacySprites.ERROR_CROSS, leftPos + 40, topPos + 87, 15, 15);
            guiGraphics.blitSprite(bl ? LegacySprites.CARTOGRAPHY_TABLE_COPY : bl2 ? LegacySprites.CARTOGRAPHY_TABLE_ZOOM : bl3 ? LegacySprites.CARTOGRAPHY_TABLE_LOCKED : LegacySprites.CARTOGRAPHY_TABLE_MAP,leftPos + 70, topPos + 61,66,66);
        }else{
            guiGraphics.blitSprite(LegacySprites.CARTOGRAPHY_TABLE,leftPos + 70, topPos + 61,66,66);
        }
        guiGraphics.blitSprite(LegacySprites.ARROW,leftPos + 139, topPos + 87,22,15);
    }
}
