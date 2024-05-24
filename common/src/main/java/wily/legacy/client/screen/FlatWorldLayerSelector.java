package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.material.Fluids;
import wily.legacy.Legacy4JClient;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class FlatWorldLayerSelector extends PanelBackgroundScreen implements LegacyMenuAccess<AbstractContainerMenu> {
    public static final Container layerSelectionGrid = new SimpleContainer(50);
    public final List<ItemStack> layerItems = new ArrayList<>();
    protected final Stocker.Sizeable scrolledList = new Stocker.Sizeable(0);
    private final Consumer<FlatWorldLayerSelector> applyLayer;
    protected final int maxLayerHeight;
    protected ItemStack selectedLayer = Items.AIR.getDefaultInstance();

    protected final AbstractContainerMenu menu;
    protected Slot hoveredSlot = null;
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    public FlatWorldLayerSelector(Screen parent, Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        super(325,245, component);
        Legacy4JClient.controllerManager.enableCursor();
        this.parent = parent;
        this.applyLayer = applyLayer;
        this.maxLayerHeight = maxLayerHeight;
        menu = new AbstractContainerMenu(null,-1) {

            @Override
            public ItemStack quickMoveStack(Player player, int i) {
                return null;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            menu.slots.add(LegacySlotDisplay.override(new Slot(layerSelectionGrid,i,23 + i % 10 * 27, 24 + i / 10 * 27), new LegacySlotDisplay(){
                public int getWidth() {
                    return 27;
                }
                public int getHeight() {
                    return 27;
                }
            }));
        }
        BuiltInRegistries.FLUID.stream().filter(f-> f.getBucket() != null && (f == Fluids.EMPTY || f.isSource(f.defaultFluidState()))).forEach(f-> {
            Item i;
            if ((i =f.defaultFluidState().createLegacyBlock().getBlock().asItem()) instanceof BlockItem) layerItems.add(i.getDefaultInstance());
            else layerItems.add(f.getBucket().getDefaultInstance());
        });
        BuiltInRegistries.BLOCK.forEach(b->{
            if (b instanceof LiquidBlock) return;
            Item i = Item.BY_BLOCK.getOrDefault(b, null);
            if (i != null) layerItems.add(i.getDefaultInstance());
        });
        scrolledList.max = layerItems.size() <= layerSelectionGrid.getContainerSize() ? 0 : layerItems.size() / layerSelectionGrid.getContainerSize();
    }
    public FlatWorldLayerSelector(Screen parent, FlatLayerInfo editLayer,Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        this(parent,applyLayer,maxLayerHeight,component);
        selectedLayer = new ItemStack(editLayer.getBlockState().getBlock().asItem(),editLayer.getHeight());
    }


    public FlatLayerInfo getFlatLayerInfo() {
        return new FlatLayerInfo(selectedLayer.count, selectedLayer.getItem() instanceof BlockItem b ? b.getBlock() : selectedLayer.getItem() instanceof BucketItem bucket ? bucket.arch$getFluid().defaultFluidState().createLegacyBlock().getBlock(): Blocks.AIR);
    }

    public void updateCreativeGridScroll(double d, double e, int i){
        float x = panel.x + 299.5f;
        float y = panel.y + 23.5f;
        if (i == 0 && d >= x && d < x + 11 && e >= y && e < y + 133){
            int lastScroll = scrolledList.get();
            scrolledList.set((int) Math.round(scrolledList.max * (e - y) / 133));
            if (lastScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(scrolledList.get() - lastScroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                fillLayerGrid();
            }
        }
    }
    @Override
    protected void init() {
        panel.init();
        fillLayerGrid();
        List<Integer> layers = IntStream.rangeClosed(1,maxLayerHeight).boxed().toList();
        addRenderableWidget(new LegacySliderButton<>(panel.x + 21, panel.y + 167, 271, 16, (b)-> Component.translatable("legacy.menu.create_flat_world.layer_height"),()-> null,(Integer) selectedLayer.count, ()-> layers, b-> selectedLayer.setCount(b.getObjectValue())));
        addRenderableWidget(Button.builder(Component.translatable("gui.ok"), b-> {
            applyLayer.accept(this);
            onClose();
        }).bounds(panel.x + 57,panel.y + 216,200,20).build());
    }
    public void fillLayerGrid(){
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            int index = scrolledList.get() * 50 + i;
            layerSelectionGrid.setItem(i,layerItems.size() > index ?  layerItems.get(index) : ItemStack.EMPTY);
        }
    }
    public boolean mouseScrolled(double d, double e, double f, double g) {
        int scroll = (int) -Math.signum(g);
        if (scrolledList.max > 0){
            int lastScrolled = scrolledList.get();
            scrolledList.set(Math.max(0,Math.min(scrolledList.get() + scroll, scrolledList.max)));
            if (lastScrolled != scrolledList.get()) {
                scrollRenderer.updateScroll(scroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                fillLayerGrid();
            }
        }
        return super.mouseScrolled(d,e,f,g);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        updateCreativeGridScroll(d,e,i);
        if (hoveredSlot != null) {
            int layerCount = selectedLayer.count;
            selectedLayer = hoveredSlot.getItem().copy();
            selectedLayer.setCount(layerCount);
        }
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        updateCreativeGridScroll(d,e,i);
        return super.mouseDragged(d, e, i, f, g);
    }

    public void setHoveredSlot(Slot hoveredSlot) {
        this.hoveredSlot = hoveredSlot;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        setHoveredSlot(null);
        menu.slots.forEach(s-> {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(panel.x, panel.y, s);
            if (!s.getItem().isEmpty())
                holder.itemIcon = s.getItem();
            holder.render(guiGraphics,i,j,f);
            if (holder.isHovered) {
                if (s.isHighlightable()) holder.renderHighlight(guiGraphics);
                setHoveredSlot(s);
            }
        });
        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty())
            guiGraphics.renderTooltip(font, hoveredSlot.getItem(), i, j);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        panel.render(guiGraphics, i, j, f);
        ScreenUtil.renderPanelRecess(guiGraphics,panel.x + 20, panel. y + 187, 275, 27,2f);

        guiGraphics.drawString(this.font, this.title,panel.x + (panel.width - font.width(title)) / 2,panel. y + 8, 0x383838, false);
        Component layerCount = Component.translatable("legacy.menu.create_flat_world.layer_count", selectedLayer.count);
        guiGraphics.drawString(this.font, layerCount,panel.x + 49 - font.width(layerCount),panel. y + 197, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, selectedLayer.getItem().getDescription(),panel.x + 70,panel. y + 197, 0xFFFFFF, true);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panel.x + 50, panel.y + 190,0);
        guiGraphics.pose().scale(1.25f,1.25f,1.25f);
        guiGraphics.renderItem(selectedLayer,0, 0);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panel.x + 299.5, panel.y + 23, 0f);
        if (scrolledList.max > 0) {
            if (scrolledList.get() != scrolledList.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 139);
            if (scrolledList.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else guiGraphics.setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,0, 0,13,135,2f);
        guiGraphics.pose().translate(-2f, -1f + (scrolledList.max > 0 ? scrolledList.get() * 121.5f / scrolledList.max : 0), 0f);
        ScreenUtil.renderPanel(guiGraphics,0,0, 16,16,3f);
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public ScreenRectangle getMenuRectangle() {
        return new ScreenRectangle(panel.x,panel.y,panel.width,panel.height);
    }

    @Override
    public Slot getHoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public ControlTooltip.Renderer getControlTooltipRenderer() {
        return controlTooltipRenderer;
    }
}
