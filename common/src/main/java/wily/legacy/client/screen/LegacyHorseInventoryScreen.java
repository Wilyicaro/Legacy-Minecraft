package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacyHorseMenu;
import wily.legacy.inventory.LegacyInventoryMenu;
import wily.legacy.util.ScreenUtil;

public class LegacyHorseInventoryScreen extends EffectRenderingInventoryScreen<LegacyHorseMenu> {
    protected final Panel panel = Panel.centered(this,215,203);

    public LegacyHorseInventoryScreen(LegacyHorseMenu menu, Inventory inventory) {
        super(menu, inventory, menu.horse.getDisplayName());
    }

    @Override
    protected void init() {
        addRenderableOnly(panel);
        addRenderableOnly((graphics,i,j,f)-> {
            ScreenUtil.renderSquareEntityPanel(graphics,leftPos + 34,topPos + 20,63,63,2);
            ScreenUtil.renderSquareRecessedPanel(graphics,leftPos + 97,topPos + 20,105,63,2);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,leftPos + 35,topPos + 21,leftPos + 95,topPos + 81,25,0.0625f,i,j, menu.horse);

        });
        panel.init();
        imageWidth = panel.width;
        imageHeight = panel.height;
        leftPos = panel.x;
        topPos = panel.y;
        inventoryLabelX = 14;
        inventoryLabelY = 91;
        titleLabelX = 14;
        titleLabelY = 8;
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
