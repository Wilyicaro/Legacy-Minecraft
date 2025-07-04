package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.controller.Controller;
import wily.legacy.util.client.LegacyRenderUtil;

public class LegacyScreen extends Screen implements Controller.Event, ControlTooltip.Event {
    public Screen parent;
    protected final UIAccessor accessor = UIAccessor.of(this);

    protected LegacyScreen(Component component) {
        super(component);
    }

    protected LegacyScreen(Screen parent, Component component) {
        this(component);
        this.parent = parent;
    }

    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f){
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, true);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderDefaultBackground(guiGraphics,i,j,f);
    }

    @Override
    public void onClose() {
        LegacyRenderUtil.playBackSound();
        this.minecraft.setScreen(parent);
    }
}
