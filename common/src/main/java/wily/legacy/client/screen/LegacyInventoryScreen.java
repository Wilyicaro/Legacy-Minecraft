package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacyInventoryMenu;
import wily.legacy.util.ScreenUtil;

public class LegacyInventoryScreen extends EffectRenderingInventoryScreen<LegacyInventoryMenu> {
    protected final Panel panel = Panel.centered(this,215,217);
    public final ResourceLocation SMALL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/small_arrow");
    public LegacyInventoryScreen(LegacyInventoryMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        addRenderableOnly(panel);
        addRenderableOnly((graphics,i,j,f)-> {
            ScreenUtil.renderEntityPanel(graphics,leftPos + 40 + (menu.hasCrafting ? 0 : 50),topPos + 13,63,84,2);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,leftPos + 40 + (menu.hasCrafting ? 0 : 50),topPos + 13,leftPos + 103 + (menu.hasCrafting ? 0 : 50),topPos + 97,35,0.0625f,i,j, minecraft.player);
            if (menu.hasCrafting) {
                graphics.drawString(this.font, this.title, leftPos + 111, topPos + 16, 0x404040, false);
                graphics.blitSprite(SMALL_ARROW_SPRITE,leftPos + 158,topPos + 43,16,13);
            }
        });
        panel.init();
        inventoryLabelX = 14;
        inventoryLabelY = 103;
        leftPos = panel.x;
        topPos = panel.y;
        imageWidth = panel.width;
        imageHeight = panel.height;
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
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
