package wily.legacy.client;

import net.minecraft.client.gui.render.state.GuiItemRenderState;

public interface LegacyGuiItemRenderState {

    static LegacyGuiItemRenderState of(GuiItemRenderState renderState) {
        return (LegacyGuiItemRenderState) (Object) renderState;
    }

    int size();

    void setSize(int size);

    float opacity();

    void setOpacity(float opacity);
}
