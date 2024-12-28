//? if fabric {
package wily.legacy.client.screen.compat;


import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import wily.factoryapi.FactoryAPIClient;

public class ModMenuCompat implements ModMenuApi {

    public static Screen getConfigScreen(String modid, Screen parent){
        return ModMenu.getConfigScreen(modid,parent);
    }

    public static void init(){
        FactoryAPIClient.SECURE_EXECUTOR.executeWhen(()->{
            ModMenuConfig.MODIFY_TITLE_SCREEN.setValue(false);
            return false;
        });
    }
}
//?}
