package wily.legacy.client.control.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.control.ControlType;

public abstract class LegacyIcon implements ComponentIcon {
    boolean lastPressed = false;
    long startPressTime = 0L;

    public abstract Component getComponent(boolean allowPressed);

    public abstract Component getOverlayComponent(boolean allowPressed);

    public Component getComponent() {
        return getComponent(false);
    }

    public abstract boolean pressed();

    public abstract boolean canLoop();

    public float getPressInterval() {
        return (Util.getMillis() - startPressTime) / 280f;
    }

    @Override
    public void click(MouseButtonEvent event) {
        startPressTime = Util.getMillis();
    }

    public Component getActualIcon(char[] chars, boolean allowPressed, ControlType type) {
        return chars == null ? null : ControlTooltip.getControlIcon(String.valueOf(chars[chars.length > 1 && allowPressed && startPressTime != 0 && (canLoop() || getPressInterval() <= 1) ? 1 + Math.round(((getPressInterval() / 2) <= 1.4f ? (getPressInterval() / 2f) % 1f : 0.4f) * (chars.length - 2)) : 0]), type).getComponent();
    }

    @Override
    public int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed, int color, boolean simulate) {
        Component c = getComponent(allowPressed);
        Component co = getOverlayComponent(allowPressed);
        Font font = Minecraft.getInstance().font;
        int cw = c == null ? 0 : font.width(c);
        int cow = co == null ? 0 : font.width(co);
        if (!simulate) {
            if (!pressed() && getPressInterval() % 1 < 0.1 && getPressInterval() >= 1) startPressTime = 0;
            if (allowPressed && pressed() && !lastPressed && startPressTime == 0) startPressTime = Util.getMillis();
            lastPressed = pressed();

            if (c != null) {
                graphics.text(font, c, x + (co == null || cw > cow ? 0 : (cow - cw) / 2), y, color, false);
            }
            if (co != null) {
                float rel = startPressTime == 0 ? 0 : canLoop() ? getPressInterval() % 1 : Math.min(getPressInterval(), 1);
                float d = 1 - Math.max(0, (rel >= 0.5f ? 1 - rel : rel) * 2 / 5);

                graphics.pose().pushMatrix();
                graphics.pose().translate(x + (c == null || cow > cw ? (cow - cow * d) / 2 : (cw - cow * d) / 2f), y + (9 - 9 * d) / 2);
                graphics.pose().scale(d, d);
                graphics.text(font, co, 0, 0, ColorUtil.withAlpha(color, ColorUtil.getAlpha(color) * (0.8f + (rel >= 0.5f ? 0.2f : 0))), false);
                graphics.pose().popMatrix();
            }
        }
        return Math.max(cw, cow);
    }
}
