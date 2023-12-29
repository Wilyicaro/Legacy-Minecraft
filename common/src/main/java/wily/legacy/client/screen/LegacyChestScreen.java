package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import wily.legacy.inventory.LegacyChestMenu;

public class LegacyChestScreen extends AbstractContainerScreen<LegacyChestMenu> {
    protected final Panel panel = Panel.centered(this,215,207 + menu.getVerticalDiff());

    public LegacyChestScreen(LegacyChestMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        addRenderableOnly(panel);
        panel.init();
        leftPos = panel.x;
        topPos = panel.y;
        imageWidth = panel.width;
        imageHeight = panel.height;
        titleLabelX = menu.getHorizontalDiff() > 0 ? (imageWidth - font.width(title)) / 2 : 14;
        titleLabelY = 11;
        inventoryLabelX = 14;
        inventoryLabelY = 94 + menu.getVerticalDiff();
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        renderTooltip(guiGraphics,i,j);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {

    }
}
