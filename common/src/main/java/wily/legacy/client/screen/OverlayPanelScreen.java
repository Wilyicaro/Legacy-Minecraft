package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public abstract class OverlayPanelScreen extends PanelBackgroundScreen{
    protected boolean transparentBackground = true;
    public OverlayPanelScreen(Function<Screen, Panel> panelConstructor, Component component) {
        super(panelConstructor, component);
    }

    public OverlayPanelScreen(int imageWidth, int imageHeight, Component component) {
        super(imageWidth, imageHeight, component);
    }


    @Override
    public void renderDefaultBackground(PoseStack poseStack, int i, int j, float f) {
        if (transparentBackground) poseStack.fillGradient(0,0,poseStack.guiWidth(),poseStack.guiHeight(), -1073741824, -805306368);;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        if (parent != null){
            parent.render(poseStack, 0, 0, f);
            deferredTooltipRendering = null;
        }
        poseStack.pose().translate(0,0, 1200);
        super.render(poseStack, i, j, f);
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
