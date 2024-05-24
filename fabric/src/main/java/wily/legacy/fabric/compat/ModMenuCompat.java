package wily.legacy.fabric.compat;

import com.terraformersmc.modmenu.ModMenu;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuCompat {

    public static Screen getConfigScreen(String modid, Screen parent){
        return ModMenu.getConfigScreen(modid,parent);
    }
}
