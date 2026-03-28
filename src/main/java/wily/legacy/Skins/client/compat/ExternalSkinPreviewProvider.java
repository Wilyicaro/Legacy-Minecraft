package wily.legacy.Skins.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;

public interface ExternalSkinPreviewProvider extends ExternalSkinProvider {

    boolean canRenderPreview(String skinId);

    boolean renderPreview(PlayerSkinWidget owner,
                          GuiGraphics guiGraphics,
                          String skinId,
                          float rotationX,
                          float rotationY,
                          boolean crouching,
                          boolean punchLoop,
                          float partialTick,
                          int left,
                          int top,
                          int right,
                          int bottom);

    void resetPreviewCache();
}
