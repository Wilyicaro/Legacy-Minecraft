package wily.legacy.client;

import net.minecraft.client.gui.render.pip.GuiEntityRenderer;

public interface LegacyGuiEntityRenderer {

    static LegacyGuiEntityRenderer of(GuiEntityRenderer renderState) {
        return (LegacyGuiEntityRenderer) (Object) renderState;
    }

    void available();

    boolean isAvailable();

    void use();
}
