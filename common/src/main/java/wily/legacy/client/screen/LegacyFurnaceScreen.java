package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.ClassicCraftingMenu;
import wily.legacy.inventory.LegacyFurnaceMenu;
import wily.legacy.inventory.LegacyInventoryMenu;

import static wily.legacy.LegacyMinecraftClient.ARROW_SPRITE;

public class LegacyFurnaceScreen extends AbstractContainerScreen<LegacyFurnaceMenu> {
    protected final Panel panel = Panel.centered(this,214,215);
    public final ResourceLocation FULL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/full_arrow");
    public final ResourceLocation LIT = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit");
    public final ResourceLocation LIT_PROGRESS = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit_progress");
    public LegacyFurnaceScreen(LegacyFurnaceMenu menu, Inventory inventory, Component component) {
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
        inventoryLabelY = 98;
        titleLabelX = 14;
        titleLabelY = 11;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        Component ingredient = Component.translatable("legacy.container.ingredient");
        guiGraphics.drawString(this.font, ingredient, 70 - font.width(ingredient), 32, 0x404040, false);
        Component fuel = Component.translatable("legacy.container.fuel");
        guiGraphics.drawString(this.font, fuel, 70 - font.width(fuel), 79, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        renderTooltip(guiGraphics,i,j);
    }
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 77,topPos + 48,0);
        guiGraphics.pose().scale(19/13f,19/13f,1.0f);
        guiGraphics.blitSprite(LIT,0,0, 13, 13);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 75.5,topPos + 46.5,0);
        guiGraphics.pose().scale(19/39f,19/39f,1.0f);
        if (menu.isLit()) {
            int n = Mth.ceil(menu.getLitProgress() * 39.0f) + 1;
            guiGraphics.blitSprite(LIT_PROGRESS, 42, 42, 0, 42 - n, 0, 42 - n, 42, n);
        }
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 48,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.0f);
        guiGraphics.blitSprite(ARROW_SPRITE,0,0,22,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 114,topPos + 46.5,0);
        guiGraphics.pose().scale(0.5f,0.5f,1.0f);
        guiGraphics.blitSprite(FULL_ARROW_SPRITE,66,48,0,0,0,0,2, (int) Math.ceil(menu.getBurnProgress() * 66), 48);
        guiGraphics.pose().popPose();
    }

    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
    }
}
