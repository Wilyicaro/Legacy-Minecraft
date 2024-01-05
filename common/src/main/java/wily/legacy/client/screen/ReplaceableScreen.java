package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;

public interface ReplaceableScreen {
    boolean canReplace();
    void setCanReplace(boolean canReplace);
    Screen getReplacement();
}
