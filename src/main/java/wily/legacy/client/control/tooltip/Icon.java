package wily.legacy.client.control.tooltip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface Icon {
    int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed, int color, boolean simulate);

    default void clickIfInside(double tooltipX, MouseButtonEvent event) {
        click(event);
    }

    default void click(MouseButtonEvent event) {

    }

    default void release(MouseButtonEvent event) {

    }

    default int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed, int color) {
        return render(graphics, x, y, allowPressed, color, false);
    }

    default int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed) {
        return render(graphics, x, y, allowPressed, 0xFFFFFFFF);
    }

    default int getWidth() {
        return render(null, 0, 0, false, 0xFFFFFFFF, true);
    }

}
