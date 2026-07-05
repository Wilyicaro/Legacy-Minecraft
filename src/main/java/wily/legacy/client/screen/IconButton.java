package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class IconButton extends ListButton {
    public IconButton(RenderableVList list, int x, int y, int width, int height, Component component) {
        super(list, x, y, width, height, component);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);
        if (!list.accessor.getBoolean(listName() + ".buttonIcon.isVisible", true)) return;
        renderIcon(guiGraphics, mouseX, mouseY, delta);
    }

    public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int iconWidth = iconWidth();
        int iconHeight = iconHeight();
        int iconX = iconX(iconWidth);
        int iconY = iconY(iconHeight);
        renderIcon(guiGraphics, mouseX, mouseY, iconX, iconY, iconWidth, iconHeight);
        if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
            renderIconHighlight(guiGraphics, mouseX, mouseY, iconX, iconY, iconWidth, iconHeight);
        }
    }

    protected int iconWidth() {
        return list.accessor.getInteger(listName() + ".buttonIcon.width", iconSize());
    }

    protected int iconHeight() {
        return list.accessor.getInteger(listName() + ".buttonIcon.height", iconSize());
    }

    protected int iconSize() {
        return list.accessor.getInteger(listName() + ".buttonIcon.size", 20);
    }

    protected int iconX(int iconWidth) {
        return iconPos(iconWidth);
    }

    protected int iconY(int iconHeight) {
        return iconPos(iconHeight);
    }

    protected int iconPos(int iconHeight) {
        return (height - iconHeight) / 2;
    }

    public void renderIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
    }

    public void renderIconHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        guiGraphics.fill(getX() + x, getY() + y, getX() + x + width, getY() + y + height, -1601138544);
    }
}
