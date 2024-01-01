package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.LegacyMinecraftClient.ARROW_SPRITE;
import static wily.legacy.LegacyMinecraftClient.ERROR_CROSS_SPRITE;

@Mixin(GrindstoneScreen.class)
public class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
    public GrindstoneScreenMixin(GrindstoneMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void init() {
        imageWidth = 207;
        imageHeight = 215;
        inventoryLabelX = 10;
        inventoryLabelY = 105;
        titleLabelX = 10;
        titleLabelY = 11;
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos, imageWidth,imageHeight,2f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 85,topPos + 50,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.blitSprite(ARROW_SPRITE,0,0,22,15);
        if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(2).hasItem())
            guiGraphics.blitSprite(ERROR_CROSS_SPRITE, 2, 0, 15, 15);
        guiGraphics.pose().popPose();
    }
}
