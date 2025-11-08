package wily.legacy.client;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.Controller;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacySoundUtil;

public interface NavigationElement {
    NavigationElement DEFAULT = new NavigationElement() {};

    static NavigationElement of(GuiEventListener listener) {
        return listener instanceof NavigationElement element ? element : DEFAULT;
    }

    default void playFocusSound(ComponentPath.Path path) {
        if (Legacy4JClient.controllerManager.isCursorDisabled || Controller.Event.of(path.component()).disableCursorOnWidgets())
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
    }

    default void applyFocus(ComponentPath.Path path, boolean apply) {
        boolean disableCursor = Controller.Event.of(path.component()).disableCursorOnWidgets();
        if (Legacy4JClient.controllerManager.isCursorDisabled || disableCursor) {
            if (!apply) {
                path.component().setFocused(null);
            } else {
                path.component().setFocused(path.childPath().component());
            }

            path.childPath().applyFocus(apply);

            if (disableCursor && apply) {
                Legacy4JClient.controllerManager.disableCursor();
            }
        } else if (apply) {
            path.component().setFocused(null);
            ScreenRectangle rect = path.childPath().component().getRectangle();
            Legacy4JClient.controllerManager.setPointerPos(rect.getCenterInAxis(ScreenAxis.HORIZONTAL), rect.getCenterInAxis(ScreenAxis.VERTICAL));
        }
    }

}
