package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacySlotWrapper;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CreativeModeScreen extends EffectRenderingInventoryScreen<CreativeModeScreen.CreativeModeMenu> {
    public final String[] legacyCreativeTabNames = new String[]{"structures","decoration","redstone_and_transport","materials","food","tools","brewing","misc"};
    protected final TabList tabList = new TabList();
    protected final Panel panel;
    public static final Container creativeModeGrid = new SimpleContainer(50);
    private CreativeInventoryListener listener;
    protected boolean hasClickedOutside;
    public static final List<List<ResourceKey<CreativeModeTab>>> java4legacyTabs = List.of(List.of(CreativeModeTabs.BUILDING_BLOCKS),List.of(CreativeModeTabs.NATURAL_BLOCKS,CreativeModeTabs.COLORED_BLOCKS),List.of(CreativeModeTabs.REDSTONE_BLOCKS),List.of(CreativeModeTabs.INGREDIENTS),List.of(),List.of(CreativeModeTabs.COMBAT,CreativeModeTabs.TOOLS_AND_UTILITIES));
    public final List<List<ItemStack>> creativeModeTabItems = new ArrayList<>();
    public final List<Stocker.Sizeable> creativeModeTabScrolls = new ArrayList<>();
    public CreativeModeScreen(Player player) {
        super(new CreativeModeMenu(player), player.getInventory(), Component.empty());
        player.containerMenu = this.menu;
        panel = new Panel(p-> (width - p.width) / 2, p-> Math.max(33,(height - 179)/ 2) ,321, 212);
        for (int i = 0; i < legacyCreativeTabNames.length; i++) {
            String tab = legacyCreativeTabNames[i];
            tabList.addTabButton(39,i == 0 ? 0 : i >= legacyCreativeTabNames.length - 1 ? 2 : 1,new ResourceLocation(LegacyMinecraft.MOD_ID,"icon/" + tab),Component.translatable("legacy.container.creative_tab." + tab),b-> fillCreativeGrid());
        }
        for (int i = 0; i < tabList.tabButtons.size(); i++) {
            creativeModeTabItems.add(new ArrayList<>());
            List<ItemStack> tabItemList = creativeModeTabItems.get(i);
            if (java4legacyTabs.size() > i)
                for (ResourceKey<CreativeModeTab> creativeModeTabResourceKey : java4legacyTabs.get(i))
                    tabItemList.addAll(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(creativeModeTabResourceKey).getDisplayItems());
            if (i == 4) tabItemList.addAll(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FOOD_AND_DRINKS).getDisplayItems().stream().filter(item-> !(item.getItem() instanceof PotionItem)).toList());
            else if (i == 6) tabItemList.addAll(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FOOD_AND_DRINKS).getDisplayItems().stream().filter(item-> item.getItem() instanceof PotionItem).toList());
            else if (i == 7) {
                List<ResourceKey<CreativeModeTab>> displayedTabs = java4legacyTabs.stream().flatMap(Collection::stream).toList();
                CreativeModeTabs.tabs().stream().filter(c-> c.getType() == CreativeModeTab.Type.CATEGORY && !displayedTabs.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(c).get()) && CreativeModeTabs.FOOD_AND_DRINKS != BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(c).get()).forEach(c->tabItemList.addAll(c.getDisplayItems()));
            }
            Stocker.Sizeable scroll = new Stocker.Sizeable(0);
            scroll.max = tabItemList.size() <= creativeModeGrid.getContainerSize() ? 0 : tabItemList.size() / creativeModeGrid.getContainerSize();
            creativeModeTabScrolls.add(scroll);
        }
    }
    public void removed() {
        super.removed();
        if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
    }

    @Override
    protected void init() {
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
        List<ItemStack> list = creativeModeTabItems.get(tabList.selectedTab);
        for (int i = 0; i < creativeModeGrid.getContainerSize(); i++) {
            int index = creativeModeTabScrolls.get(tabList.selectedTab).get() * 50 + i;
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
        Stocker.Sizeable scroll = creativeModeTabScrolls.get(tabList.selectedTab);
        if (scroll.max > 0) {
            if (scroll.get() != scroll.max)
                guiGraphics.blitSprite(RenderableVList.SCROLL_DOWN, 0, 139, 13, 7);
            if (scroll.get() > 0)
                guiGraphics.blitSprite(RenderableVList.SCROLL_UP, 0, -11, 13, 7);
        }else guiGraphics.setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(LegacyIconHolder.SIZEABLE_ICON_HOLDER, 0, 0,13,135);
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
        Stocker.Sizeable scroll = creativeModeTabScrolls.get(tabList.selectedTab);
        if (scroll.max > 0 || scroll.get() > 0){
            int lastScrolled = scroll.get();
            scroll.set(Math.max(0,Math.min(scroll.get() + i,scroll.max)));
            if (lastScrolled != scroll.get()) {
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
            Stocker.Sizeable scroll = creativeModeTabScrolls.get(tabList.selectedTab);
            int lastScroll = scroll.get();
            scroll.set((int) Math.round(scroll.max * (e - y) / 133));
            if (lastScroll != scroll.get()) fillCreativeGrid();
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i,j,k);
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
            } else if (this.menu != null) {
                ItemStack itemStack = slot == null ? ItemStack.EMPTY : menu.getSlot(slot.index).getItem();
                menu.clicked(slot == null ? i : slot.index, j, clickType, this.minecraft.player);
                if (AbstractContainerMenu.getQuickcraftHeader(j) == 2) {
                    for (int m = 0; m < 9; ++m) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(menu.getSlot(50 + m).getItem(), 36 + m);
                    }
                } else if (slot != null) {
                    ItemStack itemStack2 = menu.getSlot(slot.index).getItem();
                    this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack2, slot.index - menu.slots.size() + 9 + 36);
                    int l = 45 + j;
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
