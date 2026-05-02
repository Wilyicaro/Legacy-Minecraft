package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.material.Fluids;
import wily.factoryapi.ItemContainerPlatform;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

import java.util.function.Consumer;

public class FlatWorldLayerSelector extends ItemViewerScreen {
    protected final Panel panelRecess;
    protected final LegacySliderButton<Integer> layerSlider;
    private final Consumer<FlatWorldLayerSelector> applyLayer;
    protected Block selectedLayer = Blocks.AIR;
    protected ItemStack displayLayer = ItemStack.EMPTY;

    public FlatWorldLayerSelector(Screen parent, Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        super(parent, s -> Panel.centered(s, 325, 245), component);
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, 275, 27), p -> p.pos(panel.x + 20, panel.y + 187));
        this.applyLayer = applyLayer;
        layerSlider = LegacySliderButton.createFromIntRange(panel.x + 21, panel.y + 167, 271, 16, (b) -> Component.translatable("legacy.menu.create_flat_world.layer_height"), (b) -> null, 1, 1, maxLayerHeight, b -> {}, null);
        layerSlider.fontOverrideSupplier = () -> null;
    }

    public FlatWorldLayerSelector(Screen parent, FlatLayerInfo editLayer, Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        this(parent, applyLayer, maxLayerHeight, component);
        selectedLayer = editLayer.getBlockState().getBlock();
        displayLayer = editLayer.getBlockState().getBlock().asItem().getDefaultInstance();
        layerSlider.setObjectValue(editLayer.getHeight());
    }

    @Override
    protected void addLayerItems() {
        BuiltInRegistries.FLUID.stream().filter(f -> f.getBucket() != null && (f == Fluids.EMPTY || f.isSource(f.defaultFluidState()))).forEach(f -> {
            Item i;
            if ((i = f.defaultFluidState().createLegacyBlock().getBlock().asItem()) instanceof BlockItem)
                layerItems.add(i.getDefaultInstance());
            else layerItems.add(f.getBucket().getDefaultInstance());
        });
        BuiltInRegistries.BLOCK.forEach(b -> {
            if (b instanceof LiquidBlock) return;
            Item i = Item.BY_BLOCK.getOrDefault(b, null);
            if (i != null) layerItems.add(i.getDefaultInstance());
        });
    }

    public FlatLayerInfo getFlatLayerInfo() {
        return new FlatLayerInfo(layerSlider.getObjectValue(), selectedLayer);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        panelRecess.init("panelRecess");
    }

    @Override
    protected void init() {
        super.init();
        layerSlider.setPosition(panel.x + 21, panel.y + 167);
        layerSlider.setSize(271, 16);
        addRenderableWidget(accessor.putWidget("layerSlider", layerSlider));
        addRenderableWidget(accessor.putWidget("okButton", new LegacyButton(panel.x + 57, panel.y + 216, 200, 20, Component.translatable("gui.ok"), b -> {
            applyLayer.accept(this);
            onClose();
        })));
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.renderDefaultBackground(GuiGraphicsExtractor, i, j, f);
        panelRecess.extractRenderState(GuiGraphicsExtractor, i, j, f);
        LegacyFontUtil.applySDFont(sd -> {
            GuiGraphicsExtractor.text(this.font, this.title, panel.x + accessor.getInteger("title.x", (panel.width - font.width(title)) / 2), panel.y + accessor.getInteger("title.y", 8), CommonColor.GRAY_TEXT.get(), false);
            Component layerCount = Component.translatable("legacy.menu.create_flat_world.layer_count", layerSlider.getObjectValue());
            GuiGraphicsExtractor.text(this.font, layerCount, panel.x + accessor.getInteger("layerCount.x", 49) - font.width(layerCount), panelRecess.y + accessor.getInteger("layerCount.y", (panelRecess.getHeight() - font.lineHeight) / 2 + 1), 0xFFFFFFFF, true);
            GuiGraphicsExtractor.text(this.font, selectedLayer.getName(), panel.x + accessor.getInteger("layerName.x", 70), panelRecess.y + accessor.getInteger("layerName.y", (panelRecess.getHeight() - font.lineHeight) / 2 + 1), 0xFFFFFFFF, true);
        });

        float itemScale = accessor.getFloat("layerItem.scale", 1.25f);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(panel.x + accessor.getInteger("layerItem.x", 50), accessor.getInteger("layerItem.y", panelRecess.y + (panelRecess.height - Math.round(16 * itemScale)) / 2));
        GuiGraphicsExtractor.pose().scale(itemScale, itemScale);
        GuiGraphicsExtractor.item(displayLayer, 0, 0);
        GuiGraphicsExtractor.pose().popMatrix();
    }

    @Override
    protected void slotClicked(Slot slot) {
        displayLayer = slot.getItem();
        selectedLayer = slot.getItem().getItem() instanceof BlockItem item ? item.getBlock() : slot.getItem().getItem() instanceof BucketItem bucket ? ItemContainerPlatform.getBucketFluid(bucket).defaultFluidState().createLegacyBlock().getBlock() : Blocks.AIR;
    }
}
