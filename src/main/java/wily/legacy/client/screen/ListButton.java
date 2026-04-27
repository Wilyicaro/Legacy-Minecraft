package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

public abstract class ListButton extends AbstractButton {
    protected RenderableVList list;

    public ListButton(RenderableVList list, int x, int y, int width, int height, Component component) {
        super(x, y, width, height, component);
        this.list = list;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON_HIGHLIGHTED, LegacySprites.BUTTON_HIGHLIGHTED) : LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON, LegacySprites.BUTTON), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        this.renderScrollingString(GuiGraphicsExtractor, minecraft.font, 2, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
    }

    protected void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j) {
        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, font, this.getMessage(), this.getX() + list.accessor.getInteger(list.name + ".buttonMessage.xOffset", 35), this.getY(), getX() + this.getWidth() - 2, this.getY() + this.getHeight(), j, true));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}