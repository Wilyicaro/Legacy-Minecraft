package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.util.ScreenUtil;

import java.util.function.Function;

public abstract class OverlayPanelScreen extends PanelBackgroundScreen{
    protected boolean transparentBackground = true;

    public OverlayPanelScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component) {
        super(parent, panelConstructor, component);
    }

    public OverlayPanelScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(parent, imageWidth, imageHeight, component);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        if (transparentBackground) {
            /*? if <=1.20.1 {*//*ScreenUtil.*//*?}*/renderTransparentBackground(guiGraphics);
            ScreenUtil.renderUsername(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (parent != null){
            parent.render(guiGraphics, 0, 0, f);
            deferredTooltipRendering = null;
        }
        guiGraphics.pose().translate(0,0, 1200);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    protected void init() {
        super.init();
        if (parent != null) parent.init(minecraft,width,height);
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
