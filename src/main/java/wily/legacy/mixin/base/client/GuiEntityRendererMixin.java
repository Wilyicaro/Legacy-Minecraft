package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyGuiEntityRenderer;

@Mixin(PictureInPictureRenderer.class)
public class GuiEntityRendererMixin implements LegacyGuiEntityRenderer {
    @Unique
    PictureInPictureRenderState lastRenderState = null;
    @Unique
    private boolean available;

    @ModifyVariable(method = "prepare", at = @At("STORE"), ordinal = 0)
    protected boolean renderToTexture(boolean invalid, @Local(argsOnly = true) PictureInPictureRenderState renderState) {
        boolean newInvalid = invalid || lastRenderState == null || !lastRenderState.equals(renderState);
        lastRenderState = renderState;
        return newInvalid;
    }

    @Override
    public void available() {
        this.available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void use() {
        this.available = false;
    }
}
