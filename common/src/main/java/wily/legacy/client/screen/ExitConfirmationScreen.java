package wily.legacy.client.screen;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class ExitConfirmationScreen extends ConfirmationScreen{
    public ExitConfirmationScreen(Screen parent) {
        super(parent, 230, 156, Component.translatable("menu.quit"), Component.translatable("legacy.menu.exit_message"), b-> {});
    }

    @Override
    protected void initButtons() {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).bounds(panel.x + 15, panel.y + panel.height - 74,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("legacy.menu.exit_and_save"),b-> exitToTitleScreen(minecraft,true)).bounds(panel.x + 15, panel.y + panel.height - 52,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("legacy.menu.exit_without_save"),b-> exitToTitleScreen(minecraft,false)).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
    }
    public static void exitToTitleScreen(Minecraft minecraft, boolean save) {
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.disconnect(new LegacyLoadingScreen(Component.translatable("menu.savingLevel"),Component.empty()));
        if (save && minecraft.hasSingleplayerServer())
            minecraft.getSingleplayerServer().saveEverything(false,false,false);
        ServerData serverData = minecraft.getCurrentServer();
        MainMenuScreen mainMenuScreen = new MainMenuScreen(false);
        if (serverData != null && serverData.isRealm()) {
            minecraft.setScreen(new RealmsMainScreen(mainMenuScreen));
        } else {
            minecraft.setScreen(mainMenuScreen);
        }
    }
}
