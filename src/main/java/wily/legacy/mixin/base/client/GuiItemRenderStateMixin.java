package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.LegacyGuiItemRenderState;
import wily.legacy.client.LegacyGuiItemRenderer;

@Mixin(GuiItemRenderState.class)
public class GuiItemRenderStateMixin implements LegacyGuiItemRenderState {
    @Unique
    private int size = LegacyGuiItemRenderer.GUI_ITEM_SUBMIT_SIZE;

    @Override
    public int size() {
        return size;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }
}
