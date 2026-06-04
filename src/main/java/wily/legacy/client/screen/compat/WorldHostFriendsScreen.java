package wily.legacy.client.screen.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.PanelVListScreen;

public class WorldHostFriendsScreen extends PanelVListScreen {
    public static final Component FRIENDS = Component.translatable("legacy.menu.online");

    public WorldHostFriendsScreen(Screen parent) {
        super(parent, 250, 234, FRIENDS);
    }
}
