package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.inventory.LegacySlotWrapper;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CreativeModeScreen extends EffectRenderingInventoryScreen<CreativeModeScreen.CreativeModeMenu> {
    protected final TabList tabList = new TabList();
    protected final Panel panel;
    public static final Container creativeModeGrid = new SimpleContainer(50);
    private CreativeInventoryListener listener;
    protected boolean hasClickedOutside;
    public final List<Stocker.Sizeable> tabsScrolledList = new ArrayList<>();
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    protected static final List<LegacyCreativeTabListing> java4LegacyListing = CreativeModeTabs.tabs().stream().filter(c->c.getType() == CreativeModeTab.Type.CATEGORY).map(c->new LegacyCreativeTabListing(c.getDisplayName(), BuiltInRegistries.ITEM.getKey(c.getIconItem().getItem()), c.getIconItem().getTag(),new ArrayList<>(c.getDisplayItems()))).toList();

    protected final List<LegacyCreativeTabListing> displayListing = Stream.concat(LegacyCreativeTabListing.list.stream(), java4LegacyListing.stream()).toList();
    protected Stocker.Sizeable page = new Stocker.Sizeable(0);
    public CreativeModeScreen(Player player) {
        super(new CreativeModeMenu(player), player.getInventory(), Component.empty());
        player.containerMenu = this.menu;
        panel = new Panel(p-> (width - p.width) / 2, p-> Math.max(33,(height - 179)/ 2) ,321, 212);
        displayListing.forEach(t->{
            Stocker.Sizeable scroll = new Stocker.Sizeable(0);
            scroll.max = t.displayItems().size() <= creativeModeGrid.getContainerSize() ? 0 : t.displayItems().size() / creativeModeGrid.getContainerSize();
            tabsScrolledList.add(scroll);
        });
        page.max = (int) Math.ceil(displayListing.size() / 8f);
        initPagedTabs();
    }
    public void initPagedTabs(){
        tabList.tabButtons.clear();
        tabList.selectedTab = 0;
        int t = 0;
        for (int i = page.get() * 8; i < displayListing.size(); i++) {
            t++;
            LegacyCreativeTabListing tab = displayListing.get(i);
            tabList.addTabButton(39,t == 1 ? 0 : t >= 8 ? 2 : 1,tab.icon(), tab.itemIconTag(),tab.name(),b-> fillCreativeGrid());
            if (t == 8) break;
        }
    }
    public void removed() {
        super.removed();
        if (this.minecraft.player != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
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
            t.translocation = (b)-> {
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
        List<ItemStack> list = displayListing.get(page.get() * 8 + tabList.selectedTab).displayItems();
        for (int i = 0; i < creativeModeGrid.getContainerSize(); i++) {
            int index = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab).get() * 50 + i;
            creativeModeGrid.setItem(i,list.size() > index ?  list.get(index) : ItemStack.EMPTY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 296.5f, topPos + 27.5f, 0f);
        Stocker.Sizeable scroll = tabsScrolledList.get(page.get() * 8 + tabList.selectedTab);
        if (scroll.max > 0) {
            if (scroll.get() != scroll.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,0, 139);
            if (scroll.get() > 0)
                scrollRenderer.renderScroll(guiGraphics,ScreenDirection.UP, 0, -11);
        }else guiGraphics.setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        ScreenUtil.renderSquareRecessedPanel(guiGraphics, 0, 0,13,135,2f);
        guiGraphics.pose().translate(-2f, -1f + (scroll.max > 0 ? scroll.get() * 121.5f / scroll.max : 0), 0f);
        ScreenUtil.renderPanel(guiGraphics,0,0, 16,16,3f);
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component tabTitle = tabList.tabButtons.get(tabList.selectedTab).getMessage();
        guiGraphics.drawString(this.font, tabTitle, (imageWidth - font.width(tabTitle)) / 2, 12, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {

    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
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
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
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
        tabList.controlTab(i,j,k);
        if ((i == 262 || i == 263) && page.max > 0){
            int lastPage = page.get();
            page.set(Math.min(page.max - 1, page.get() + (i == 262 ? 1 : -1)));
            if (lastPage != page.get()){
                initPagedTabs();
                rebuildWidgets();
            }
        }
        if (i == InputConstants.KEY_X) {
            for (int n = 36; n < 45; ++n)
                this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, n);
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
                    int l = bl ? itemStack2.getMaxStackSize() : itemStack2.getCount();
                    menu.setCarried(itemStack2.copyWithCount(l));
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
    public static class CreativeModeMenu extends AbstractContainerMenu {
        private final AbstractContainerMenu inventoryMenu;

        public CreativeModeMenu(Player player) {
            super(null, 0);
            this.inventoryMenu = player.inventoryMenu;
            for (int h = 0; h < 5; h++) {
                for (int x = 0; x < 10; x++) {
                    addSlot(new LegacySlotWrapper(creativeModeGrid,h * 10 + x, 21 + x * 27, 29 + h * 27){
                        public int getWidth() {
                            return 27;
                        }
                        @Override
                        public int getHeight() {
                            return 27;
                        }
                    });
                }
            }
            for (int x = 0; x < 9; x++) {
                addSlot(new LegacySlotWrapper(player.getInventory(), x,35 + x * 27,176){
                    public int getWidth() {
                        return 27;
                    }
                    @Override
                    public int getHeight() {
                        return 27;
                    }
                });
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
