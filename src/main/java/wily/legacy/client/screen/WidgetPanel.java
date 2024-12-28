package wily.legacy.client.screen;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.UIDefinition;

import java.util.function.Consumer;
import java.util.function.Function;

public class WidgetPanel extends Panel implements GuiEventListener, NarratableEntry, ControlTooltip.ActionHolder {
    boolean focused;

    public WidgetPanel(Screen screen) {
        super(screen);
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

    @Override
    public @Nullable Component getAction(Context context) {
        return null;
    }
}
