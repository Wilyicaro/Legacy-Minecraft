package wily.legacy.client.screen;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.RenderableVListEntry;
import wily.legacy.util.client.LegacyFontUtil;

public class LegacyButton extends Button.Plain implements RenderableVListEntry {
    public LegacyButton(Component message, OnPress onPress, Tooltip tooltip) {
        this(message, onPress);
        setTooltip(tooltip);
    }

    public LegacyButton(Component message, OnPress onPress) {
        this(0, 0, 200, 20, message, onPress);
    }

    public LegacyButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    protected LegacyButton(int i, int j, int k, int l, Component component, OnPress onPress, CreateNarration createNarration) {
        super(i, j, k, l, component, onPress, createNarration);
    }

    @Override
    protected void renderDefaultLabel(ActiveTextCollector activeTextCollector) {
        LegacyFontUtil.applySDFont(b -> super.renderDefaultLabel(activeTextCollector));
    }

    public static int getDefaultHeight() {
        return LegacyOptions.getUIMode().isSD() ? 15 : 20;
    }

    @Override
    public void initRenderable(RenderableVList list) {
        setHeight(getDefaultHeight());
    }
}
