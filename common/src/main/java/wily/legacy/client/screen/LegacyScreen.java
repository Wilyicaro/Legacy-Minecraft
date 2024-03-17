package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

public class LegacyScreen extends Screen {
    public Screen parent;
    public final ControlTooltip.Renderer controlTooltipRenderer = ControlTooltip.defaultScreen(this);

    protected LegacyScreen(Component component) {
        super(component);
    }
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f){
        ScreenUtil.renderDefaultBackground(guiGraphics);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderDefaultBackground(guiGraphics,i,j,f);
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }

    @Override
    public void onClose() {
        ScreenUtil.playSimpleUISound(LegacySoundEvents.BACK.get(),1.0f);
        this.minecraft.setScreen(parent);
    }
}
