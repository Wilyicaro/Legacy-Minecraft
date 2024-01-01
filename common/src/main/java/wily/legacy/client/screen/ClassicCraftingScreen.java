package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.ClassicCraftingMenu;

import static wily.legacy.LegacyMinecraftClient.ARROW_SPRITE;

public class ClassicCraftingScreen extends AbstractContainerScreen<ClassicCraftingMenu> {
    protected final Panel panel = Panel.centered(this,215,202);
    public ClassicCraftingScreen(ClassicCraftingMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        panel.init();
        leftPos = panel.x;
        topPos = panel.y;
        imageWidth = panel.width;
        imageHeight = panel.height;
        inventoryLabelX = 14;
        inventoryLabelY = 90;
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 11;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        renderTooltip(guiGraphics,i,j);
    }
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 105,topPos + 43,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.blitSprite(ARROW_SPRITE,0,0,22,15);
        guiGraphics.pose().popPose();
    }

    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
    }
}
