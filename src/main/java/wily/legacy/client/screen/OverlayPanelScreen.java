package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Function;

public abstract class OverlayPanelScreen extends PanelBackgroundScreen {
    protected boolean darkBackground = true;

    public OverlayPanelScreen(Screen parent, Panel.Constructor<OverlayPanelScreen> panelConstructor, Component component) {
        super(parent, panelConstructor.cast(), component);
    }

    public OverlayPanelScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(parent, imageWidth, imageHeight, component);
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        if (parent != null) {
            GuiGraphicsExtractor.nextStratum();
            parent.extractBackground(GuiGraphicsExtractor, 0, 0, f);
            GuiGraphicsExtractor.nextStratum();
            parent.extractRenderState(GuiGraphicsExtractor, 0, 0, f);
            GuiGraphicsExtractor.deferredTooltip = null;
        }

        if (darkBackground) {
            extractTransparentBackground(GuiGraphicsExtractor);
            LegacyRenderUtil.renderUsername(GuiGraphicsExtractor);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (parent != null) parent.init(width, height);
    }

    @Override
    public void tick() {
        super.tick();
        if (parent != null) parent.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }
}
