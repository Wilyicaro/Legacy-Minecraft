//? if fabric {
package wily.legacy.client.screen.compat;


import com.terraformersmc.modmenu.config.ModMenuConfig;
import wily.factoryapi.FactoryAPIClient;

public class ModMenuCompat {
    public static void init() {
        FactoryAPIClient.SECURE_EXECUTOR.executeWhen(() -> {
            ModMenuConfig.MODIFY_TITLE_SCREEN.setValue(false);
            return false;
        });
    }
}
//?}
