package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacySprites;

import java.util.function.Consumer;

public class FlatWorldLayerSelector extends ItemViewerScreen {
    private final Consumer<FlatWorldLayerSelector> applyLayer;
    protected Block selectedLayer = Blocks.AIR;
    protected final LegacySliderButton<Integer> layerSlider;

    protected ItemStack displayLayer = ItemStack.EMPTY;

    public FlatWorldLayerSelector(Screen parent, Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        super(parent,s-> Panel.centered(s,325,245), component);
        this.applyLayer = applyLayer;
        layerSlider = LegacySliderButton.createFromIntRange(panel.x + 21, panel.y + 167, 271, 16, (b)-> Component.translatable("legacy.menu.create_flat_world.layer_height"),(b)-> null, 1, 1, maxLayerHeight, b-> {});
    }

    @Override
    protected void addLayerItems() {
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
    }

    public FlatWorldLayerSelector(Screen parent, FlatLayerInfo editLayer, Consumer<FlatWorldLayerSelector> applyLayer, int maxLayerHeight, Component component) {
        this(parent,applyLayer,maxLayerHeight,component);
        selectedLayer = editLayer.getBlockState().getBlock();
        displayLayer = editLayer.getBlockState().getBlock().asItem().getDefaultInstance();
        layerSlider.setObjectValue(editLayer.getHeight());
    }

    public FlatLayerInfo getFlatLayerInfo() {
        return new FlatLayerInfo(layerSlider.getObjectValue(), selectedLayer);
    }

    @Override
    protected void init() {
        super.init();
        layerSlider.setPosition(panel.x + 21, panel.y + 167);
        addRenderableWidget(layerSlider);
        addRenderableWidget(Button.builder(Component.translatable("gui.ok"), b-> {
            applyLayer.accept(this);
            onClose();
        }).bounds(panel.x + 57,panel.y + 216,200,20).build());
    }

    @Override
    protected void slotClicked(Slot slot) {
        displayLayer = slot.getItem();
        selectedLayer = slot.getItem().getItem() instanceof BlockItem item ? item.getBlock() : slot.getItem().getItem() instanceof BucketItem bucket ? ItemContainerPlatform.getBucketFluid(bucket).defaultFluidState().createLegacyBlock().getBlock() : Blocks.AIR;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,panel.x + 20, panel. y + 187, 275, 27);

        guiGraphics.drawString(this.font, this.title,panel.x + (panel.width - font.width(title)) / 2,panel. y + 8, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        Component layerCount = Component.translatable("legacy.menu.create_flat_world.layer_count", layerSlider.getObjectValue());
        guiGraphics.drawString(this.font, layerCount,panel.x + 49 - font.width(layerCount),panel. y + 197, 0xFFFFFFFF, true);
        guiGraphics.drawString(this.font, selectedLayer.getName(),panel.x + 70,panel. y + 197, 0xFFFFFFFF, true);


        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(panel.x + 50, panel.y + 190);
        guiGraphics.pose().scale(1.25f, 1.25f);
        guiGraphics.renderItem(displayLayer, 0, 0);
        guiGraphics.pose().popMatrix();
    }

}
