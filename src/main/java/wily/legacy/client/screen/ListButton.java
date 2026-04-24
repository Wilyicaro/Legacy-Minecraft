package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON_HIGHLIGHTED, LegacySprites.BUTTON_HIGHLIGHTED) : LegacyRenderUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON, LegacySprites.BUTTON), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        this.renderString(guiGraphics, minecraft.font, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
        if (this.isHovered()) {
            guiGraphics.requestCursor(this.isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + list.accessor.getInteger(list.name + ".buttonMessage.xOffset", 35), this.getY(), getX() + this.getWidth() - 2, this.getY() + this.getHeight(), j, true));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}