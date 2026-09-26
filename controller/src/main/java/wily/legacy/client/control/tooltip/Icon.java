package wily.legacy.client.control.tooltip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A control icon that can rendered and calculated its width, and should be clickable too.
 */
public interface Icon {
    /**
     * @param simulate if it should be really rendered, or be just used for calculating the width.
     * @return icon width
     */
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

    /**
     * Defaulted to the simulated rendering, as it'll calculate the width anyway during the process
     * @return icon width
     */
    default int getWidth() {
        return render(null, 0, 0, false, 0xFFFFFFFF, true);
    }

}
