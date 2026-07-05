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
import wily.legacy.client.CommonColor;
import wily.legacy.util.ScreenUtil;

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
        layerSlider.fontOverrideSupplier = () -> null;
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
        layerSlider.setPosition(accessor.getInteger("layerSlider.x", panel.x + 21), accessor.getInteger("layerSlider.y", panel.y + 167));
        layerSlider.setWidth(accessor.getInteger("layerSlider.width", 271));
        addRenderableWidget(accessor.putWidget("layerSlider", layerSlider));
        addRenderableWidget(accessor.putWidget("okButton", Button.builder(Component.translatable("gui.ok"), b-> {
            applyLayer.accept(this);
            onClose();
        }).bounds(accessor.getInteger("okButton.x", panel.x + 57), accessor.getInteger("okButton.y", panel.y + 216), accessor.getInteger("okButton.width", 200), accessor.getInteger("okButton.height", 20)).build()));
    }

    @Override
    protected void slotClicked(Slot slot) {
        displayLayer = slot.getItem();
        selectedLayer = slot.getItem().getItem() instanceof BlockItem item ? item.getBlock() : slot.getItem().getItem() instanceof BucketItem bucket ? ItemContainerPlatform.getBucketFluid(bucket).defaultFluidState().createLegacyBlock().getBlock() : Blocks.AIR;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        int panelRecessY = accessor.getInteger("panelRecess.y", panel.y + 187);
        int panelRecessHeight = accessor.getInteger("panelRecess.height", 27);
        ScreenUtil.renderPanelRecess(accessor, guiGraphics, "panelRecess", panel.x + 20, panelRecessY, 275, panelRecessHeight);

        ScreenUtil.applySDFont(ignored -> {
            guiGraphics.drawString(this.font, this.title, panel.x + accessor.getInteger("title.x", (panel.width - font.width(title)) / 2), panel.y + accessor.getInteger("title.y", 8), CommonColor.GRAY_TEXT.get(), false);
            Component layerCount = Component.translatable("legacy.menu.create_flat_world.layer_count", layerSlider.getObjectValue());
            int layerCountY = panelRecessY + accessor.getInteger("layerCount.y", (panelRecessHeight - font.lineHeight) / 2 + 1);
            int layerNameY = panelRecessY + accessor.getInteger("layerName.y", (panelRecessHeight - font.lineHeight) / 2 + 1);
            guiGraphics.drawString(this.font, layerCount, panel.x + accessor.getInteger("layerCount.x", 49) - font.width(layerCount), layerCountY, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, selectedLayer.getName(), panel.x + accessor.getInteger("layerName.x", 70), layerNameY, 0xFFFFFF, true);
        });


        guiGraphics.pose().pushPose();
        float scale = accessor.getFloat("layerItem.scale", 1.25f);
        int layerItemY = accessor.getInteger("layerItem.y", panelRecessY + (panelRecessHeight - Math.round(16 * scale)) / 2);
        guiGraphics.pose().translate(panel.x + accessor.getInteger("layerItem.x", 50), layerItemY, 0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.renderItem(displayLayer, 0, 0);
        guiGraphics.pose().popPose();
    }

}
