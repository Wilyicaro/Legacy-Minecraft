package wily.legacy.client.screen;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.ScreenUtil;

public class ExitConfirmationScreen extends ConfirmationScreen{
    public ExitConfirmationScreen(Screen parent) {
        super(parent, 230, 156, Component.translatable("menu.quit"), Minecraft.getInstance().hasSingleplayerServer() && !ScreenUtil.getLegacyOptions().autoSave().get() ? Component.translatable("legacy.menu.exit_message") : Minecraft.getInstance().screen instanceof MainMenuScreen ? Component.translatable("legacy.menu.gameExitMessage") : Component.translatable("legacy.menu.server_exit_message"), b-> {});
    }

    @Override
    protected void initButtons() {
        if (minecraft.hasSingleplayerServer() && !ScreenUtil.getLegacyOptions().autoSave().get())
            addRenderableWidget(Button.builder(Component.translatable("legacy.menu.exit_and_save"),b-> exit(minecraft,true)).bounds(panel.x + 15, panel.y + panel.height -52,200,20).build());
        else panel.height-=22;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).bounds(panel.x + 15, panel.y + panel.height - (minecraft.hasSingleplayerServer() && !ScreenUtil.getLegacyOptions().autoSave().get() ? 74 : 52),200,20).build());
        addRenderableWidget(Button.builder(Component.translatable(minecraft.hasSingleplayerServer() && !ScreenUtil.getLegacyOptions().autoSave().get() ? "legacy.menu.exit_without_save" : "menu.quit"),b-> exit(minecraft,false)).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
    }
    public static void exit(Minecraft minecraft, boolean save) {
        if (minecraft.screen instanceof ConfirmationScreen s && s.parent instanceof MainMenuScreen){
            minecraft.stop();
            return;
        }
        if (save) {
            Legacy4JClient.manualSave = true;
            Legacy4JClient.retakeWorldIcon = true;
        }

        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }

        minecraft.disconnect(new LegacyLoadingScreen( Component.translatable(save ? "menu.savingLevel": "disconnect.quitting"),Component.empty()));
        ServerData serverData = minecraft.getCurrentServer();
        MainMenuScreen mainMenuScreen = new MainMenuScreen();
        if (serverData != null && serverData.isRealm()) {
            minecraft.setScreen(new RealmsMainScreen(mainMenuScreen));
        } else {
            minecraft.setScreen(mainMenuScreen);
        }
    }
}
