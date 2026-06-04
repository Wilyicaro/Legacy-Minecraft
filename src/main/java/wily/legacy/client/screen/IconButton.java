package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class IconButton extends ListButton {
    public IconButton(RenderableVList list, int x, int y, int width, int height, Component component) {
        super(list, x, y, width, height, component);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractContents(GuiGraphicsExtractor, i, j, f);
        if (list.accessor.getBoolean(list.name + ".buttonIcon.isVisible", true))
            renderIcon(GuiGraphicsExtractor, i, j, f);
    }

    public void renderIcon(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float f) {
        int iconWidth = list.accessor.getInteger(list.name + ".buttonIcon.width", 20);
        int iconHeight = list.accessor.getInteger(list.name + ".buttonIcon.height", 20);
        int iconPos = (height - iconHeight) / 2;
        renderIcon(GuiGraphicsExtractor, mouseX, mouseY, iconPos, iconPos, iconWidth, iconHeight);
        if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
            renderIconHighlight(GuiGraphicsExtractor, mouseX, mouseY, iconPos, iconPos, iconWidth, iconHeight);
        }
    }

    public void renderIcon(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, int x, int y, int width, int height) {
    }

    public void renderIconHighlight(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, int x, int y, int width, int height) {
        GuiGraphicsExtractor.fill(getX() + x, getY() + y, getX() + x + width, getY() + y + height, -1601138544);
    }
}
