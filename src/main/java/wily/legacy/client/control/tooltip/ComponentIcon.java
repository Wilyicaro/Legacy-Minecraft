package wily.legacy.client.control.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public interface ComponentIcon extends Icon {
    static ComponentIcon of(Component component) {
        return new ComponentIcon() {
            @Override
            public Component getComponent() {
                return component;
            }

            @Override
            public int render(GuiGraphicsExtractor graphics, int x, int y, boolean allowPressed, int color, boolean simulate) {
                Font font = Minecraft.getInstance().font;
                if (!simulate) graphics.text(font, getComponent(), x, y, color, false);
                return font.width(getComponent());
            }
        };
    }

    Component getComponent();
}
