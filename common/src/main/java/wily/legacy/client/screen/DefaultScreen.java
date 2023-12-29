package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.util.ScreenUtil;

public class DefaultScreen extends Screen {
    public Screen parent;
    protected DefaultScreen(Component component) {
        super(component);
    }
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f){
        ScreenUtil.renderDefaultBackground(guiGraphics);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderDefaultBackground(guiGraphics,i,j,f);
    }
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
