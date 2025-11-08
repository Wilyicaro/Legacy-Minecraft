package wily.legacy.client.screen;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySaveCache;

public class ExitConfirmationScreen extends ConfirmationScreen {
    public ExitConfirmationScreen(Screen parent) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> getBaseHeight() + (LegacySaveCache.hasSaveSystem(Minecraft.getInstance()) ? LegacyOptions.getUIMode().isSD() ? 19 : 33 : 0), Component.translatable("menu.quit"), Minecraft.getInstance().hasSingleplayerServer() && LegacyOptions.autoSaveInterval.get() == 0 ? Component.translatable("legacy.menu.exit_message") : Minecraft.getInstance().screen instanceof TitleScreen ? Component.translatable("legacy.menu.gameExitMessage") : Component.translatable("legacy.menu.server_exit_message"), b -> {});
    }

    public static void exit(Minecraft minecraft, boolean save) {
        if (minecraft.getConnection() == null) {
            minecraft.stop();
            return;
        }

        if (save) LegacySaveCache.saveExit = LegacySaveCache.retakeWorldIcon = true;

        if (minecraft.level != null) {
            minecraft.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
        }

        minecraft.disconnect(new LegacyLoadingScreen(Component.translatable(save ? "menu.savingLevel" : "disconnect.quitting"), Component.empty()), false);
        ServerData serverData = minecraft.getCurrentServer();
        TitleScreen mainMenuScreen = new TitleScreen();
        if (serverData != null && serverData.isRealm()) {
            minecraft.setScreen(new RealmsMainScreen(mainMenuScreen));
        } else {
            minecraft.setScreen(mainMenuScreen);
        }
    }

    @Override
    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).build());
        if (LegacySaveCache.hasSaveSystem(minecraft)) {
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.exit_and_save"), b -> exit(minecraft, true)).build());
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.exit_without_save"), b -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.exit_without_save_title"), Component.translatable("legacy.menu.exit_without_save_message"), b1 -> exit(minecraft, false)))).build());
        } else
            renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), b -> exit(minecraft, minecraft.hasSingleplayerServer() && (minecraft.getSingleplayerServer().isHardcore() || !LegacySaveCache.isCurrentWorldSource(minecraft.getSingleplayerServer().storageSource)))).build());
    }
}
