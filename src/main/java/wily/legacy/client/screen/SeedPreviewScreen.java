package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import wily.legacy.util.LegacySprites;

public class SeedPreviewScreen extends LegacyScreen {
    private float scale;
    private int left;
    private int top;

    public SeedPreviewScreen(Screen parent) {
        super(parent, Component.translatable("legacy.menu.seed_preview"));
    }

    @Override
    protected void init() {
        super.init();
        scale = Math.min(1, Math.min((width - 24) / 371f, (height - 48) / 265f));
        left = (width - scaled(371)) / 2;
        top = (height - scaled(265)) / 2 - scaled(17);
        addPanel("overviewPanel", LegacySprites.PANEL, 0, 0, 131, 141);
        addPanel("detailPanel", LegacySprites.PANEL, 136, 0, 234, 262);
        addPanel("helpPanel", LegacySprites.POINTER_PANEL, 0, 144, 133, 120);
    }

    private int scaled(int value) {
        return Math.round(value * scale);
    }

    private void addPanel(String name, Identifier sprite, int x, int y, int width, int height) {
        Panel panel = new Panel(this);
        panel.init(name);
        panel.appearance(sprite, scaled(width), scaled(height));
        panel.pos(left + scaled(x), top + scaled(y));
        addRenderableOnly(panel);
    }
}
