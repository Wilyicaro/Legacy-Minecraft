package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Function;

public abstract class OverlayPanelScreen extends PanelBackgroundScreen {
    protected boolean darkBackground = true;

    public OverlayPanelScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component) {
        super(parent, panelConstructor, component);
    }

    public OverlayPanelScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(parent, imageWidth, imageHeight, component);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        if (parent != null) {
            guiGraphics.nextStratum();
            parent.renderBackground(guiGraphics, 0, 0, f);
            guiGraphics.nextStratum();
            parent.render(guiGraphics, 0, 0, f);
            guiGraphics.deferredTooltip = null;
        }

        if (darkBackground) {
            renderTransparentBackground(guiGraphics);
            LegacyRenderUtil.renderUsername(guiGraphics);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (parent != null) parent.init(minecraft, width, height);
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
