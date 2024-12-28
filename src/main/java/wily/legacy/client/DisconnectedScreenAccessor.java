package wily.legacy.client;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public interface DisconnectedScreenAccessor {
    static DisconnectedScreenAccessor of(DisconnectedScreen screen){
        return (DisconnectedScreenAccessor) screen;
    }
    Component getReason();
    Screen getParent();
}
