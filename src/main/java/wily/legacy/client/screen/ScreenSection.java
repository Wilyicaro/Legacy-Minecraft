package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public interface ScreenSection<S extends Screen> {
    Component title();

    S build(Screen parent);

    default Button.Builder createButtonBuilder(Screen parent) {
        return RenderableVListScreen.openScreenButton(title(), () -> build(parent));
    }
}
