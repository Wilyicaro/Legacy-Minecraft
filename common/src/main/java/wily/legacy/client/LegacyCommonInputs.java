package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LegacyCommonInputs {

    public static boolean selected(int keyCode) {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(windowHandle, keyCode);
    }
}

/*
package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LegacyCommonInputs {

    public static boolean selected(int keyCode) {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(windowHandle, keyCode);
    }

    public static boolean isSpaceOrEnterPressed() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(windowHandle, InputConstants.KEY_SPACE) || 
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_ENTER);
    }

    public static boolean isMouseButtonPressed(int button) {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isMouseButtonDown(windowHandle, button);
    }

    public static boolean isModifierKeyPressed() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(windowHandle, InputConstants.KEY_LSHIFT) ||
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_RSHIFT) ||
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_LCONTROL) ||
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_RCONTROL) ||
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_LALT) ||
               InputConstants.isKeyDown(windowHandle, InputConstants.KEY_RALT);
    }
}
*/