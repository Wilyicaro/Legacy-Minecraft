package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import wily.legacy.util.client.LegacyRenderUtil;

public abstract class ListButton extends AbstractButton {
    public ListButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    //? if <1.21.11 {
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderButton(guiGraphics, mouseX, mouseY, partialTicks);
    }

    protected void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
    }
    //?} else {

    /*@Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        renderButton(guiGraphics, i, j, f);
    }

    protected void renderButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderDefaultSprite(guiGraphics);
        renderScrollingString(guiGraphics, Minecraft.getInstance().font, 2, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
    }

    protected abstract void renderScrollingString(GuiGraphics guiGraphics, Font font, int offset, int color);
    *///?}
}