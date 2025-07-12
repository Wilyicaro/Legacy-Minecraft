package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//? if <1.21.2 {
/*import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
*///?}
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?} else {
/*import net.minecraft.client.searchtree.SearchRegistry;
*///?}
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryItemUtil;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.function.Supplier;

import static wily.legacy.client.screen.ControlTooltip.*;

public class CreativeModeScreen extends /*? if <=1.21.2 {*//*EffectRenderingInventoryScreen*//*?} else {*/AbstractContainerScreen/*?}*/<CreativeModeScreen.CreativeModeMenu> implements Controller.Event,ControlTooltip.Event{
    protected Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(UIAccessor.of(this), new PagedList<>(page,8));
    protected final Panel panel;
    public static final Container creativeModeGrid = new SimpleContainer(50);
    private CreativeInventoryListener listener;
    protected boolean hasClickedOutside;
    public final List<Stocker.Sizeable> tabsScrolledList = new ArrayList<>();
    protected final LegacyScroller scroller = LegacyScroller.create(135, ()-> tabsScrolledList.get(page.get() * 8 + tabList.selectedIndex));
    protected final List<List<ItemStack>> displayListing = new ArrayList<>();
    protected final Stocker.Sizeable arrangement = new Stocker.Sizeable(0,2);
    protected final EditBox searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200,20, LegacyComponents.SEARCH_ITEMS);
    boolean canRemoveSearch = false;

    public CreativeModeScreen(Player player) {
        super(new CreativeModeMenu(player), player.getInventory(), Component.empty());
        searchBox.setResponder(s-> {
            fillCreativeGrid();
            tabsScrolledList.get(page.get() * 8 + tabList.selectedIndex).set(0);
        });
        searchBox.setMaxLength(50);
        LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(Minecraft.getInstance());
        for (LegacyCreativeTabListing tab : LegacyCreativeTabListing.map.values()) {
            displayListing.add(tab.displayItems().stream().map(Supplier::get).filter(i-> !i.isEmpty() && i.isItemEnabled(Minecraft.getInstance().getConnection().enabledFeatures())).toList());
            tabList.addTabButton(39, LegacyTabButton.Type.LEFT, tab.icon(), tab.name(), b -> pressCommonTab());
        }
        BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter(CreativeModeScreen::canDisplayVanillaCreativeTab).forEach(c-> {
            List<ItemStack> displayItems;
            if (c.getType() == CreativeModeTab.Type.HOTBAR){
                displayItems = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    Hotbar hotbar = Minecraft.getInstance().getHotbarManager().get(i);
                    if (hotbar.isEmpty()) {
                        for (int j = 0; j < 10; j++) {
                            if (j == i) {
                                ItemStack itemStack = new ItemStack(Items.PAPER);
                                Component component = Minecraft.getInstance().options.keyHotbarSlots[i].getTranslatedKeyMessage();
                                Component component2 = Minecraft.getInstance().options.keySaveHotbarActivator.getTranslatedKeyMessage();
                                //? if <1.20.5 {
                                /*itemStack.getOrCreateTagElement("CustomCreativeLock");
                                itemStack.setHoverName(Component.translatable("inventory.hotbarInfo", component2, component));
                                *///?} else {
                                itemStack.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                                itemStack.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", component2, component));
                                //?}
                                displayItems.add(itemStack);
                            } else {
                                displayItems.add(ItemStack.EMPTY);
                            }
                        }
                    } else {
                        displayItems.addAll(hotbar/*? if >=1.20.5 {*/.load(Minecraft.getInstance().level.registryAccess())/*?}*/);
                        displayItems.add(ItemStack.EMPTY);
                    }
                }
            }else displayItems = List.copyOf(c.getDisplayItems());
            displayListing.add(displayItems);
            tabList.addTabButton(39, LegacyTabButton.Type.LEFT,LegacyTabButton.iconOf(c.getIconItem()), c.getDisplayName(), b -> pressCommonTab());
        });
        if (LegacyOptions.searchCreativeTab.get()) {
            displayListing.add(List.copyOf(CreativeModeTabs.searchTab().getDisplayItems()));
            tabList.addTabButton(39, LegacyTabButton.Type.LEFT, LegacyTabButton.iconOf(LegacySprites.SEARCH), LegacyComponents.SEARCH_ITEMS, b -> {
                canRemoveSearch = arrangement.get() != 2 && !canRemoveSearch;
                arrangement.set(2);
                repositionElements();
            });
        }
        player.containerMenu = this.menu;
        panel = Panel.createPanel(this, p-> p.appearance(LegacySprites.CREATIVE_PANEL, 321, 212), p-> p.pos(p.centeredLeftPos(this), Math.max(33,(height - 179)/ 2)));
        displayListing.forEach(t-> tabsScrolledList.add(new Stocker.Sizeable(0)));
    }

    public void pressCommonTab(){
        if (canRemoveSearch) {
            canRemoveSearch = false;
            arrangement.set(0);
            repositionElements();
        }else fillCreativeGrid();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        Event.super.addControlTooltips(renderer);
        renderer.
                replace(2,i-> i,a-> canClearQuickSelect() && !ControlType.getActiveType().isKbm() ? LegacyComponents.CLEAR_QUICK_SELECT : a).
                replace(3,i-> i, a-> hoveredSlot != null && hoveredSlot.hasItem() && hoveredSlot.container != creativeModeGrid ? LegacyComponents.CLEAR : a).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? ControlTooltip.ComponentIcon.compoundOf(ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.RIGHT_STICK.getIcon() : null,()-> LegacyComponents.PAGE).
                add(()-> canClearQuickSelect() && ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : null, ()-> LegacyComponents.CLEAR_QUICK_SELECT);
    }


    public boolean canClearQuickSelect(){
        return ControlType.getActiveType().isKbm() || hoveredSlot == null || hoveredSlot.container == minecraft.player.getInventory() && hoveredSlot.getItem().getCount() <= 1;
    }
    public static AbstractContainerScreen<?> getActualCreativeScreenInstance(Minecraft minecraft){
        return LegacyOptions.legacyCreativeTab.get() ? new CreativeModeScreen(minecraft.player) : new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get());
    }
    public void removed() {
        super.removed();
        if (this.minecraft.player != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
    }
    public static boolean canDisplayVanillaCreativeTab(CreativeModeTab c){
        ResourceLocation location = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(c);
        return c.shouldDisplay() && (c.getType() == CreativeModeTab.Type.CATEGORY || c.getType() == CreativeModeTab.Type.HOTBAR) && location != null && (LegacyOptions.vanillaTabs.get() || !location.getNamespace().equals("minecraft") || location.equals(CreativeModeTabs.OP_BLOCKS.location()));
    }

    @Override
    protected void init() {
        super.init();
        if (!Legacy4JClient.playerHasInfiniteMaterials()) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
            return;
        }
        addRenderableWidget(tabList);
        addRenderableOnly(panel);
        addRenderableOnly(tabList::renderSelected);
        panel.init();
        imageWidth = panel.width;
        imageHeight = panel.height;
        leftPos = panel.x;
        topPos = panel.y;
        addRenderableOnly(scroller);
        scroller.setPosition(panel.x + 296, panel.y + 27);
        scroller.offset(new Vec3(0.5f, 0.5f, 0));
        if (arrangement.get() == 2){
            searchBox.setPosition(panel.getX() + (panel.getWidth() - searchBox.getWidth()) / 2 - 6, panel.getY() + 7);
            addRenderableWidget(searchBox);
        }
        this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        this.listener = new CreativeInventoryListener(this.minecraft);
        this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
        tabList.init(panel.x,panel.y - 33, panel.width,(t,i)->{
            int index = tabList.tabButtons.indexOf(t);
            t.type = LegacyTabButton.Type.bySize(index, 7);
            t.offset = (b)-> {
                if (!t.selected) return new Vec3(0,1.5,0);
                return Vec3.ZERO;
            };
            t.setWidth(41);
            t.setX(t.getX() - tabList.tabButtons.indexOf(t));
        });
        fillCreativeGrid();
    }

    public void fillCreativeGrid(){
        if (displayListing.isEmpty()) return;
        List<ItemStack> list = displayListing.get(page.get() * 8 + tabList.selectedIndex);
        if (arrangement.get() != 0 && (arrangement.get() == 1 || !searchBox.getValue().isEmpty() && minecraft.getConnection() != null)) list = arrangement.get() == 1 ? list.stream().sorted(Comparator.comparing(i->i.getDisplayName().getString())).toList() : getItemsSearchResult(minecraft,searchBox.getValue());
        for (int i = 0; i < creativeModeGrid.getContainerSize(); i++) {
            int index = tabsScrolledList.get(page.get() * 8 + tabList.selectedIndex).get() * 50 + i;
            creativeModeGrid.setItem(i,list.size() > index ?  list.get(index) : ItemStack.EMPTY);
        }
        tabsScrolledList.get(page.get() * 8 + tabList.selectedIndex).max = Math.max(0, (list.size() - 1) / creativeModeGrid.getContainerSize());
    }

    public static List<ItemStack> getItemsSearchResult(Minecraft minecraft, String value){
        return (value.startsWith("#") ? /*? if <1.20.5 {*//*minecraft.getSearchTree(SearchRegistry.CREATIVE_TAGS)*//*?} else {*/minecraft.getConnection().searchTrees().creativeTagSearch()/*?}*/.search(value.substring(1).toLowerCase(Locale.ROOT)) : /*? if <1.20.5 {*//*minecraft.getSearchTree(SearchRegistry.CREATIVE_NAMES)*//*?} else {*/minecraft.getConnection().searchTrees().creativeNameSearch()/*?}*/.search(value.toLowerCase(Locale.ROOT)));
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
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        this.renderTooltip(guiGraphics, i, j);
        //? if >=1.21.2 {
        LegacyRenderUtil.renderContainerEffects(guiGraphics,leftPos,topPos,imageWidth,imageHeight,i,j);
        //?}
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        if (arrangement.get() == 2) return;
        Component tabTitle = tabList.tabButtons.get(tabList.selectedIndex).getMessage();
        guiGraphics.drawString(this.font, tabTitle, (imageWidth - font.width(tabTitle)) / 2, 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {

    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        scroller.mouseReleased(d, e, i);
        return super.mouseReleased(d, e, i);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        else if (scroller.mouseScrolled(g)){
            fillCreativeGrid();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (searchBox.isFocused() && !searchBox.isMouseOver(d,e)) setFocused(null);
        if (scroller.mouseClicked(d, e, i)) fillCreativeGrid();
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (scroller.mouseDragged(e)) fillCreativeGrid();
        return super.mouseDragged(d, e, i, f, g);
    }

    @Override
    public int getBindingMouseClick(BindingState state) {
        return state.is(ControllerBinding.LEFT_BUTTON) && canClearQuickSelect() ? -1 : Controller.Event.super.getBindingMouseClick(state);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (tabList.controlTab(i)) return true;
        if (i != InputConstants.KEY_ESCAPE && searchBox.isFocused()) return searchBox.keyPressed(i,j,k);

        if (hasShiftDown() && tabList.controlPage(page,i == 263 , i == 262)) repositionElements();
        if (i == InputConstants.KEY_X && canClearQuickSelect()) {
            for (int n = 36; n < 45; ++n) {
                //? if >=1.21.2
                this.minecraft.player.inventoryMenu.getSlot(n).set(ItemStack.EMPTY);
                this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, n);
            }
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
                    if (!itemStack2.isEmpty()/*? if >=1.21.2 {*/ && minecraft.player.canDropItems()/*?}*/) {
                        ItemStack itemStack3 = itemStack2.copyWithCount(j == 0 ? 1 : itemStack2.getMaxStackSize());
                        this.minecraft.player.drop(itemStack3, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack3);
                    }
                    return;
                }
                if (!itemStack.isEmpty() && !itemStack2.isEmpty() && FactoryItemUtil.equalItems(itemStack, itemStack2)) {
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
                    }
                    //? if <1.21.2 {
                    /*else if (clickType == ClickType.THROW && !itemStack.isEmpty()) {
                        ItemStack itemStack4 = itemStack.copyWithCount(j == 0 ? 1 : itemStack.getMaxStackSize());
                        this.minecraft.player.drop(itemStack4, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack4);
                    }
                    *///?}
                    this.minecraft.player.inventoryMenu.broadcastChanges();
                }
            }
        } else if (!menu.getCarried().isEmpty() && this.hasClickedOutside) {
            //? if >=1.21.2 {
            if (!this.minecraft.player.canDropItems()) {
                return;
            }
            //?}
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
        return this.hasClickedOutside = super.hasClickedOutside(d, e, i, j, k) && !tabList.isMouseOver(d,e);
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
                    addSlot(LegacySlotDisplay.override(new Slot(creativeModeGrid,h * 10 + x, 21 + x * 27, 29 + h * 27){
                        @Override
                        public boolean mayPickup(Player player) {
                            return super.mayPickup(player) && !getItem().isEmpty() ? getItem().isItemEnabled(player.level().enabledFeatures()) && /*? if <1.20.5 {*//*getItem().getTagElement("CustomCreativeLock") == null*//*?} else {*/!getItem().has(DataComponents.CREATIVE_SLOT_LOCK)/*?}*/ : getItem().isEmpty();
                        }
                    }, new LegacySlotDisplay(){
                        @Override
                        public int getWidth() {
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
