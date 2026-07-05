package wily.legacy.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LegacyFlatPresetsScreen extends PanelVListScreen{

    public LegacyFlatPresetsScreen(Screen parent, HolderLookup.RegistryLookup<FlatLevelGeneratorPreset> presetGetter, FeatureFlagSet enabledFeatures, Consumer<Holder<FlatLevelGeneratorPreset>> applyPreset) {
        super(parent, s-> Panel.centered(s, LegacySprites.PANEL,285, 260), Component.translatable("createWorld.customize.presets"));
        presetGetter.listElements().forEach(holder->{
            Set<Block> set = (holder.value()).settings().getLayersInfo().stream().map((flatLayerInfo) -> flatLayerInfo.getBlockState().getBlock()).filter((block) -> !block.isEnabled(enabledFeatures)).collect(Collectors.toSet());
            if (!set.isEmpty()) {
                Legacy4J.LOGGER.info("Discarding flat world preset {} since it contains experimental blocks {}", holder.unwrapKey().map((resourceKey) -> resourceKey.location().toString()).orElse("<unknown>"), set);
            } else {
                FlatLevelGeneratorPreset preset = holder.value();
                renderableVList.addRenderable(new AbstractButton(0,0,263,30,Component.translatable(holder.key().location().toLanguageKey("flat_world_preset"))) {
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        super.renderWidget(guiGraphics, i, j, f);
                        renderPresetIcon(this, guiGraphics, preset.displayItem().value().getDefaultInstance());
                    }
                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        int k = this.getX() + getListInteger("presetMessage.xOffset", 33);
                        int l = this.getX() + this.getWidth();
                        ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j,true));
                    }
                    @Override
                    public void onPress() {
                        minecraft.setScreen(new ConfirmationScreen(LegacyFlatPresetsScreen.this, Component.translatable("legacy.menu.create_flat_world.load_preset"),Component.translatable("legacy.menu.create_flat_world.load_preset_message"),b->{
                            applyPreset.accept(holder);
                            minecraft.setScreen(parent);
                        }));
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }
                });
            }
        });
        renderableVList.layoutSpacing(l-> 0);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.applySDFont(sd -> guiGraphics.drawString(font,getTitle(),panel.x + (panel.width - font.width(getTitle()))/2, panel.y + (sd ? 5 : 9), CommonColor.INVENTORY_GRAY_TEXT.get(), false))));
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.renderPanelRecess(accessor, guiGraphics, "panelRecess", panel.x + 6, panel.y + 20, panel.width - 12, panel.height - 29)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", panel.x + 11),
                accessor.getInteger("renderableVList.y", panel.y + 26),
                accessor.getInteger("renderableVList.width", panel.width - 22),
                accessor.getInteger("renderableVList.height", panel.height - 38));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget) {
            //? if <=1.20.1 {
            /*((WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", 30));
            *///?} else {
            widget.setHeight(accessor.getInteger("buttonsHeight", 30));
            //?}
        }
    }

    private int getListInteger(String key, int fallback) {
        String name = getRenderableVList().name == null ? "renderableVList" : getRenderableVList().name;
        return accessor.getInteger(name + "." + key, fallback);
    }

    private float getListFloat(String key, float fallback) {
        String name = getRenderableVList().name == null ? "renderableVList" : getRenderableVList().name;
        return accessor.getFloat(name + "." + key, fallback);
    }

    private void renderPresetIcon(AbstractWidget widget, GuiGraphics guiGraphics, ItemStack stack) {
        guiGraphics.pose().pushPose();
        float scale = getListFloat("presetIcon.scale", 1.25f);
        int iconHeight = Math.round(16 * scale);
        int x = widget.getX() + getListInteger("presetIcon.x", 5);
        int y = widget.getY() + getListInteger("buttonItem.y", (widget.getHeight() - iconHeight) / 2);
        guiGraphics.pose().translate(x, y,0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.renderItem(stack,0, 0);
        guiGraphics.pose().popPose();
    }
}
