package wily.legacy.client.screen;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.util.ScreenUtil;

public class ExitConfirmationScreen extends ConfirmationScreen{
    public ExitConfirmationScreen(Screen parent) {
        super(parent, 230,  hasSaveSystem(Minecraft.getInstance()) ? 156 : 133, Component.translatable("menu.quit"), Minecraft.getInstance().hasSingleplayerServer() && LegacyOption.autoSaveInterval.get() == 0 ? Component.translatable("legacy.menu.exit_message") : Minecraft.getInstance().screen instanceof TitleScreen ? Component.translatable("legacy.menu.gameExitMessage") : Component.translatable("legacy.menu.server_exit_message"), b-> {});
    }

    @Override
    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
        if (hasSaveSystem(minecraft)) {
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.exit_and_save"), b -> exit(minecraft, true)).build());
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.exit_without_save"), b-> minecraft.setScreen(new ConfirmationScreen(this,Component.translatable("legacy.menu.exit_without_save_title"),Component.translatable("legacy.menu.exit_without_save_message"), b1-> exit(minecraft, false)))).build());
        }else renderableVList.addRenderable(Button.builder(Component.translatable( "menu.quit"), b-> exit(minecraft, minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isHardcore())).build());
    }

    public static boolean hasSaveSystem(Minecraft minecraft){
        return minecraft.hasSingleplayerServer() && !minecraft.isDemo() && !minecraft.getSingleplayerServer().isHardcore();
    }

    public static void exit(Minecraft minecraft, boolean save) {
        if (minecraft.getConnection() == null){
            minecraft.stop();
            return;
        }

        if (save) Legacy4JClient.saveExit = Legacy4JClient.retakeWorldIcon = true;
        //? if <=1.20.2
        /*boolean wasInRealms = minecraft.isConnectedToRealms();*/
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.getSoundManager().stop();

        minecraft./*? if >1.20.2 {*/disconnect/*?} else {*//*clearLevel*//*?}*/(new LegacyLoadingScreen(Component.translatable(save ? "menu.savingLevel": "disconnect.quitting"),Component.empty()));
        Assort.applyDefaultResourceAssort();
        ServerData serverData = minecraft.getCurrentServer();
        TitleScreen mainMenuScreen = new TitleScreen();
        if (serverData != null && /*? if >1.20.2 {*/serverData.isRealm()/*?} else {*//*wasInRealms*//*?}*/) {
            minecraft.setScreen(new RealmsMainScreen(mainMenuScreen));
        } else {
            minecraft.setScreen(mainMenuScreen);
        };
    }
}
