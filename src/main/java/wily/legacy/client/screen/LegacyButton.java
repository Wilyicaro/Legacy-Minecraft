package wily.legacy.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.RenderableVListEntry;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacyFontUtil;

public class LegacyButton extends Button implements RenderableVListEntry {
    public LegacyButton(Component message, Button.OnPress onPress, Tooltip tooltip) {
        this(message, onPress);
        setTooltip(tooltip);
    }

    public LegacyButton(Component message, Button.OnPress onPress) {
        this(0, 0, 200, 20, message, onPress);
    }

    public LegacyButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        this(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    protected LegacyButton(int i, int j, int k, int l, Component component, Button.OnPress onPress, Button.CreateNarration createNarration) {
        super(i, j, k, l, component, onPress, createNarration);
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderScrollingString(guiGraphics, font, i, j));
    }

    public static int getDefaultHeight() {
        return LegacyOptions.getUIMode().isSD() ? 15 : 20;
    }

    @Override
    public void initRenderable(RenderableVList list) {
        height = getDefaultHeight();
    }
}
