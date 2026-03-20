package wily.legacy.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LegacyFlatPresetsScreen extends PanelVListScreen {
    protected final Panel panelRecess;

    public LegacyFlatPresetsScreen(Screen parent, HolderLookup.RegistryLookup<FlatLevelGeneratorPreset> presetGetter, FeatureFlagSet enabledFeatures, Consumer<Holder<FlatLevelGeneratorPreset>> applyPreset) {
        super(parent, s -> Panel.centered(s, LegacySprites.PANEL, 285, 260), Component.translatable("createWorld.customize.presets"));
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 12, panel.height - 29), p -> p.pos(panel.x + 6, panel.y + 20));
        presetGetter.listElements().forEach(holder -> {
            Set<Block> set = (holder.value()).settings().getLayersInfo().stream().map((flatLayerInfo) -> flatLayerInfo.getBlockState().getBlock()).filter((block) -> !block.isEnabled(enabledFeatures)).collect(Collectors.toSet());
            if (!set.isEmpty()) {
                Legacy4J.LOGGER.info("Discarding flat world preset {} since it contains experimental blocks {}", holder.unwrapKey().map((resourceKey) -> resourceKey.location().toString()).orElse("<unknown>"), set);
            } else {
                FlatLevelGeneratorPreset preset = holder.value();
                renderableVList.addRenderable(new LegacyFlatWorldScreen.ItemIconButton(0, 0, 263, 30, Component.translatable(holder.key().location().toLanguageKey("flat_world_preset"))) {
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        super.renderWidget(guiGraphics, i, j, f);
                        renderItem(guiGraphics, preset.displayItem().value().getDefaultInstance(), "presetIcon", 5);
                    }

                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        renderScrollingString(guiGraphics, font, "presetMessage", 33, i, j);
                    }

                    @Override
                    public void onPress(InputWithModifiers input) {
                        minecraft.setScreen(new ConfirmationScreen(LegacyFlatPresetsScreen.this, Component.translatable("legacy.menu.create_flat_world.load_preset"), Component.translatable("legacy.menu.create_flat_world.load_preset_message"), b -> {
                            applyPreset.accept(holder);
                            minecraft.setScreen(parent);
                        }));
                    }
                });
            }
        });
        renderableVList.layoutSpacing(l -> 0);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        panelRecess.init("panelRecess");
        addRenderableOnly(((guiGraphics, i, j, f) -> LegacyFontUtil.applySDFont(sd -> guiGraphics.drawString(font, getTitle(), panel.x + (panel.width - font.width(getTitle())) / 2, panel.y + (sd ? 5 : 9), CommonColor.GRAY_TEXT.get(), false))));
        addRenderableOnly(panelRecess);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 11, panel.y + 26, panel.getWidth() - 22, panel.height - 38);
    }
}
