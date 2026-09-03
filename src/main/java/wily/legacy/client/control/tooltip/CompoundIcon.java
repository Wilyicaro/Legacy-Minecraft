package wily.legacy.client.control.tooltip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import wily.legacy.Legacy4JClient;

import java.util.function.Function;

/**
 * Appends different icons, making them render sequentially. It'll click the first found icon, or all the icons if additive.
 */
public interface CompoundIcon extends Icon {
    Function<Icon[], Icon> COMPOUND_ICON_CACHE = Util.memoize(icons -> (CompoundIcon) () -> icons);

    static Icon of(Icon... icons) {
        return COMPOUND_ICON_CACHE.apply(icons);
    }

    Icon[] getIcons();

    @Override
    default void clickIfInside(double tooltipX, MouseButtonEvent event) {
        Icon[] icons = getIcons();
        for (int i = 0; i < icons.length; i++) {
            Icon icon = icons[i];
            double diffX = event.x() - tooltipX;
            if (isAdditive() || (diffX >= 0 && diffX < icon.getWidth() || i == icons.length - 1)) {
                icon.clickIfInside(tooltipX, event);
                if (!isAdditive()) break;
            }
            tooltipX += icon.getWidth();
        }
        if (Legacy4JClient.controllerManager.simulateShift) Legacy4JClient.controllerManager.simulateShift = false;
    }

    default boolean isAdditive() {
        return false;
    }

    @Override
    default void release(MouseButtonEvent event) {
        for (Icon icon : getIcons()) icon.release(event);
    }

    @Override
    default int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed, int color, boolean simulate) {
        int totalWidth = 0;
        for (Icon icon : getIcons())
            totalWidth += icon.render(graphics, x + totalWidth, y, allowPressed, color, simulate);
        return totalWidth;
    }
}
