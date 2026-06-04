package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.controller.Controller;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

public class LegacyScreen extends Screen implements Controller.Event, ControlTooltip.Event {
    protected final UIAccessor accessor = UIAccessor.of(this);
    public Screen parent;

    protected LegacyScreen(Component component) {
        super(component);
    }

    protected LegacyScreen(Screen parent, Component component) {
        this(component);
        this.parent = parent;
    }

    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, GuiGraphicsExtractor, true);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        renderDefaultBackground(GuiGraphicsExtractor, i, j, f);
    }

    @Override
    public void onClose() {
        LegacySoundUtil.playBackSound();
        this.minecraft.setScreen(parent);
    }
}
