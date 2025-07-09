package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyGuiItemRenderState;
import wily.legacy.client.LegacyGuiItemRenderer;

@Mixin(GuiItemRenderState.class)
public abstract class GuiItemRenderStateMixin implements LegacyGuiItemRenderState {

    @Unique
    private int size = 16;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(String string, Matrix3x2f matrix3x2f, TrackingItemStackRenderState trackingItemStackRenderState, int i, int j, ScreenRectangle screenRectangle, CallbackInfo ci) {
        size = LegacyGuiItemRenderer.getScale(matrix3x2f);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }
}
