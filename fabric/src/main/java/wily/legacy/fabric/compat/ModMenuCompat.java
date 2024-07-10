package wily.legacy.fabric.compat;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.Legacy4JClient;

public class ModMenuCompat {

    public static Screen getConfigScreen(String modid, Screen parent){
        return ModMenu.getConfigScreen(modid,parent);
    }
    public static void init(){
        Legacy4JClient.SECURE_EXECUTOR.executeWhen(()->{
            ModMenuConfig.MODIFY_TITLE_SCREEN.setValue(false);
            return false;
        });
    }
}
