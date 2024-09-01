package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LegacyCommonInputs {

    public static boolean selected(Screen screen) {
        if (screen == null) {
            return false;
        }
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(windowHandle, InputConstants.KEY_SPACE) || 
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_ENTER);
    }
}
