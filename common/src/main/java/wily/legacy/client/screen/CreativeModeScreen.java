package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacySlotDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static wily.legacy.client.screen.ControlTooltip.*;

public class CreativeModeScreen extends EffectRenderingInventoryScreen<CreativeModeScreen.CreativeModeMenu> implements Controller.Event,ControlTooltip.Event{
    protected Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(new PagedList<>(page,8));
    protected final Panel panel;
    public static final Container creativeModeGrid = new SimpleContainer(50);
    private CreativeInventoryListener listener;
    protected boolean hasClickedOutside;
    public final List<Stocker.Sizeable> tabsScrolledList = new ArrayList<>();
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final List<List<ItemStack>> displayListing = new ArrayList<>();

    public CreativeModeScreen(Player player) {
        super(new CreativeModeMenu(player), player.getInventory(), Component.empty());
        LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(Minecraft.getInstance());
        for (LegacyCreativeTabListing tab : LegacyCreativeTabListing.list) {
            displayListing.add(tab.displayItems().stream().map(Supplier::get).filter(i-> !i.isEmpty() && i.isItemEnabled(Minecraft.getInstance().getConnection().enabledFeatures())).toList());
            tabList.addTabButton(39, 0, tab.icon(), tab.name(), b -> fillCreativeGrid());
        }
        BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter(CreativeModeScreen::canDisplayVanillaCreativeTab).forEach(c-> {
            displayListing.add(List.copyOf(c.getDisplayItems()));
            tabList.addTabButton(39, 0,LegacyTabButton.iconOf(c.getIconItem()), c.getDisplayName(), b -> fillCreativeGrid());
        });

        player.containerMenu = this.menu;
        panel = new Panel(p-> (width - p.width) / 2, p-> Math.max(33,(height - 179)/ 2) ,321, 212);
        displayListing.forEach(t->{
            Stocker.Sizeable scroll = new Stocker.Sizeable(0);
            scroll.max = t.size() <= creativeModeGrid.getContainerSize() ? 0 : t.size() / creativeModeGrid.getContainerSize();
            tabsScrolledList.add(scroll);
        });

    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        Event.super.addControlTooltips(renderer);
        renderer.
                set(3,ControlTooltip.create(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT), PLUS_ICON,getKeyIcon(InputConstants.KEY_LSHIFT)}) : ControllerBinding.UP_BUTTON.bindingState.getIcon(),()-> hoveredSlot != null && hoveredSlot.hasItem() ? getAction(hoveredSlot.container == creativeModeGrid ?  "legacy.action.take_all" : "legacy.action.clear") : null)).
                set(2,ControlTooltip.create(()-> ControlType.getActiveType().isKbm() ?  getKeyIcon(canClearQuickSelect() ? InputConstants.KEY_X: InputConstants.MOUSE_BUTTON_RIGHT) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()-> getAction(canClearQuickSelect() ? "legacy.action.clear_quick_select" : "legacy.action.take_half"))).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->getAction("legacy.action.page"));
    }


    public boolean canClearQuickSelect(){
        return hoveredSlot == null || hoveredSlot.container == minecraft.player.getInventory() && !hoveredSlot.hasItem();
    }
    public static EffectRenderingInventoryScreen<?> getActualCreativeScreenInstance(Minecraft minecraft){
        return ScreenUtil.getLegacyOptions().legacyCreativeTab().get() ? new CreativeModeScreen(minecraft.player) : new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get());
    }
    public void removed() {
        super.removed();
        if (this.minecraft.player != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
    }
    public static boolean canDisplayVanillaCreativeTab(CreativeModeTab c){
        ResourceLocation location = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(c);
        return c.shouldDisplay() && c.getType() == CreativeModeTab.Type.CATEGORY && location != null && (ScreenUtil.getLegacyOptions().vanillaTabs().get() || !location.getNamespace().equals("minecraft") || location.equals(CreativeModeTabs.OP_BLOCKS.location()));
    }

    @Override
    protected void init() {
        if (!this.minecraft.gameMode.hasInfiniteItems()) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
            return;
        }
        addRenderableWidget(tabList);
        addRenderableOnly(panel);
        panel.init();
        imageWidth = panel.width;
        imageHeight = panel.height;
        leftPos = panel.x;
        topPos = panel.y;
        this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        this.listener = new CreativeInventoryListener(this.minecraft);
        this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
        tabList.init(panel.x,panel.y - 33, panel.width,(t,i)->{
            int index = tabList.tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= 7 ? 2 : 1;
            t.offset = (b)-> {
                if (!t.selected) return new Offset(0,1.5,0);
                return Offset.ZERO;
            };
            t.setWidth(41);
            t.setX(t.getX() - tabList.tabButtons.indexOf(t));
        });
        fillCreativeGrid();
    }
    public void fillCreativeGrid(){
        if (displayListing.isEmpty()) return;
        List<ItemStack> list = displayListing.get(page.get() * 8 + tabList.selectedTab);
        for (int i = 0; i < creativeModeGrid.getContainerSize(); i++) {
            int index = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab).get() * 50 + i;
            creativeModeGrid.setItem(i,list.size() > index ?  list.get(index) : ItemStack.EMPTY);
        }
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        super.render(poseStack, i, j, f);
        poseStack.pushPose();
        poseStack.translate(leftPos + 296.5f, topPos + 27.5f, 0f);
        Stocker.Sizeable scroll = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab);
        if (scroll.max > 0) {
            if (scroll.get() != scroll.max)
                scrollRenderer.renderScroll(poseStack, ScreenDirection.DOWN,0, 139);
            if (scroll.get() > 0)
                scrollRenderer.renderScroll(poseStack,ScreenDirection.UP, 0, -11);
        }else poseStack.setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, 0, 0,13,135);
        poseStack.translate(-2f, -1f + (scroll.max > 0 ? scroll.get() * 121.5f / scroll.max : 0), 0f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        poseStack.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        poseStack.popPose();
        this.renderTooltip(poseStack, i, j);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int i, int j) {
        Component tabTitle = tabList.tabButtons.get(tabList.selectedTab).getMessage();
        poseStack.drawString(this.font, tabTitle, (imageWidth - font.width(tabTitle)) / 2, 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float f, int i, int j) {

    }

    @Override
    public boolean mouseScrolled(double d, double e, double g) {
        int i = (int) -Math.signum(g);
        Stocker.Sizeable scroll = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab);
        if (scroll.max > 0 || scroll.get() > 0){
            int lastScrolled = scroll.get();
            scroll.set(Math.max(0,Math.min(scroll.get() + i,scroll.max)));
            if (lastScrolled != scroll.get()) {
                scrollRenderer.updateScroll(i > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                fillCreativeGrid();
            }
        }
        return super.mouseScrolled(d, e, g);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (canClearQuickSelect() && i == 1) return false;
        updateCreativeGridScroll(d,e,i);
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        updateCreativeGridScroll(d,e,i);
        return super.mouseDragged(d, e, i, f, g);
    }

    public void updateCreativeGridScroll(double d, double e, int i){
        float x = leftPos + 297.5f;
        float y = topPos + 28.5f;
        if (i == 0 && d >= x && d < x + 11 && e >= y && e < y + 133){
            Stocker.Sizeable scrolledList = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab);
            int lastScroll = scrolledList.get();
            scrolledList.set((int) Math.round(scrolledList.max * (e - y) / 133));
            if (lastScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(scrolledList.get() - lastScroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                fillCreativeGrid();
            }
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i);
        if (hasShiftDown() && tabList.controlPage(page,i == 263 , i == 262)) repositionElements();
        if (i == InputConstants.KEY_X && canClearQuickSelect()) {
            for (int n = 36; n < 45; ++n)
                this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, n);
            return true;
        }
        return super.keyPressed(i, j, k);
    }
    protected void slotClicked(@Nullable Slot slot, int i, int j, ClickType clickType) {
        boolean bl = clickType == ClickType.QUICK_MOVE;
        clickType = i == -999 && clickType == ClickType.PICKUP ? ClickType.THROW : clickType;
        if (slot != null || clickType == ClickType.QUICK_CRAFT) {
            if (slot != null && !slot.mayPickup(this.minecraft.player)) {
                return;
            }
            if (clickType != ClickType.QUICK_CRAFT && slot.container == creativeModeGrid) {
                ItemStack itemStack = menu.getCarried();
                ItemStack itemStack2 = slot.getItem();
                if (clickType == ClickType.SWAP) {
                    if (!itemStack2.isEmpty()) {
                        this.minecraft.player.getInventory().setItem(j, itemStack2.copyWithCount(itemStack2.getMaxStackSize()));
                        this.minecraft.player.inventoryMenu.broadcastChanges();
                    }
                    return;
                }
                if (clickType == ClickType.CLONE) {
                    if (menu.getCarried().isEmpty() && slot.hasItem()) {
                        ItemStack itemStack3 = slot.getItem();
                        menu.setCarried(itemStack3.copyWithCount(itemStack3.getMaxStackSize()));
                    }
                    return;
                }
                if (clickType == ClickType.THROW) {
                    if (!itemStack2.isEmpty()) {
                        ItemStack itemStack3 = itemStack2.copyWithCount(j == 0 ? 1 : itemStack2.getMaxStackSize());
                        this.minecraft.player.drop(itemStack3, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack3);
                    }
                    return;
                }
                if (!itemStack.isEmpty() && !itemStack2.isEmpty() && ItemStack.isSameItemSameTags(itemStack, itemStack2)) {
                    if (j == 0) {
                        if (bl) {
                            itemStack.setCount(itemStack.getMaxStackSize());
                        } else if (itemStack.getCount() < itemStack.getMaxStackSize()) {
                            itemStack.grow(1);
                        }
                    } else {
                        itemStack.shrink(1);
                    }
                } else if (itemStack2.isEmpty() || !itemStack.isEmpty()) {
                    if (j == 0) {
                        menu.setCarried(ItemStack.EMPTY);
                    } else if (!menu.getCarried().isEmpty()) {
                        menu.getCarried().shrink(1);
                    }
                } else {
                    ItemStack stack =  itemStack2.copyWithCount(bl ? itemStack2.getMaxStackSize() : itemStack2.getCount());
                    if (!menu.moveItemStackTo(stack,50,59,false))
                        if (!stack.isEmpty()){
                            menu.setCarried(stack);
                            ((LegacyMenuAccess<?>)this).movePointerToSlot(menu.slots.get(58));
                        }
                    menu.inventoryMenu.broadcastChanges();
                }
            } else {
                ItemStack itemStack = slot == null ? ItemStack.EMPTY : menu.getSlot(slot.index).getItem();
                menu.clicked(slot == null ? i : slot.index, j, clickType, this.minecraft.player);
                if (AbstractContainerMenu.getQuickcraftHeader(j) == 2) {
                    for (int m = 0; m < 9; ++m) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(menu.getSlot(50 + m).getItem(), 36 + m);
                    }
                } else if (slot != null) {
                    ItemStack itemStack2 = menu.getSlot(slot.index).getItem();
                    this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack2, slot.index - menu.slots.size() + 9 + 36);
                    int l = 50 + j;
                    if (clickType == ClickType.SWAP) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack, l - menu.slots.size() + 9 + 36);
                    } else if (clickType == ClickType.THROW && !itemStack.isEmpty()) {
                        ItemStack itemStack4 = itemStack.copyWithCount(j == 0 ? 1 : itemStack.getMaxStackSize());
                        this.minecraft.player.drop(itemStack4, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack4);
                    }
                    this.minecraft.player.inventoryMenu.broadcastChanges();
                }
            }
        } else if (!menu.getCarried().isEmpty() && this.hasClickedOutside) {
            if (j == 0) {
                this.minecraft.player.drop(menu.getCarried(), true);
                this.minecraft.gameMode.handleCreativeModeItemDrop(menu.getCarried());
                menu.setCarried(ItemStack.EMPTY);
            }
            if (j == 1) {
                ItemStack itemStack = menu.getCarried().split(1);
                this.minecraft.player.drop(itemStack, true);
                this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack);
            }
        }
    }
    protected boolean hasClickedOutside(double d, double e, int i, int j, int k) {
        boolean bl = d < (double)i || e < (double)j || d >= (double)(i + this.imageWidth) || e >= (double)(j + this.imageHeight);
        this.hasClickedOutside = bl && !tabList.isMouseOver(d,e);
        return this.hasClickedOutside;
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && s.pressed && s.canClick() && tabList.controlPage(page,s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y))){
            repositionElements();
        }
    }

    public static class CreativeModeMenu extends AbstractContainerMenu {
        private final AbstractContainerMenu inventoryMenu;

        public CreativeModeMenu(Player player) {
            super(null, 0);
            this.inventoryMenu = player.inventoryMenu;
            for (int h = 0; h < 5; h++) {
                for (int x = 0; x < 10; x++) {
                    addSlot(LegacySlotDisplay.override(new Slot(creativeModeGrid,h * 10 + x, 21 + x * 27, 29 + h * 27), new LegacySlotDisplay(){
                        public int getWidth() {
                            return 27;
                        }
                        @Override
                        public int getHeight() {
                            return 27;
                        }
                    }));
                }
            }
            for (int x = 0; x < 9; x++) {
                addSlot(LegacySlotDisplay.override(new Slot(player.getInventory(), x,35 + x * 27,176), new LegacySlotDisplay(){
                    public int getWidth() {
                        return 27;
                    }
                    @Override
                    public int getHeight() {
                        return 27;
                    }
                }));
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int i) {
            Slot slot;
            if (i >= this.slots.size() - 9 && i < this.slots.size() && (slot = this.slots.get(i)) != null && slot.hasItem()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
        @Override
        public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
            return slot.container != creativeModeGrid;
        }

        @Override
        public boolean canDragTo(Slot slot) {
            return slot.container != creativeModeGrid;
        }

        @Override
        public ItemStack getCarried() {
            return this.inventoryMenu.getCarried();
        }

        @Override
        public void setCarried(ItemStack itemStack) {
            this.inventoryMenu.setCarried(itemStack);
        }
    }
}
