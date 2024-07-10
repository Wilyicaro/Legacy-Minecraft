package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class WidgetPanel extends Panel implements GuiEventListener, NarratableEntry {
    boolean focused;

    public WidgetPanel(Function<Panel, Integer> leftPosGetter, Function<Panel, Integer> topPosGetter, int width, int height) {
        super(leftPosGetter, topPosGetter, width, height);
    }
    public WidgetPanel(Screen s, int imageWidth, int imageHeight) {
        super( p-> (s.width - p.width) / 2, p-> (s.height - p.height) / 2, imageWidth, imageHeight);
    }


    public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }
    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return super.getRectangle();
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
