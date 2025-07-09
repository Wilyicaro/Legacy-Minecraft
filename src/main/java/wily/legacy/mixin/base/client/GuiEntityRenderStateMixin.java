package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.MutablePIPRenderState;

@Mixin({GuiEntityRenderState.class, GuiBookModelRenderState.class, GuiBannerResultRenderState.class})
public abstract class GuiEntityRenderStateMixin implements PictureInPictureRenderState, MutablePIPRenderState {
    @Unique
    Matrix3x2f pose = IDENTITY_POSE;

    @Unique
    Float scale = null;

    @Override
    public Matrix3x2f pose() {
        return pose;
    }

    @Override
    public void setPose(Matrix3x2f pose) {
        this.pose = new Matrix3x2f(pose);
    }

    @ModifyReturnValue(method = "scale", at = @At("RETURN"))
    public float scale(float original) {
        if (scale == null) {
            return scale = original;
        }
        return scale;
    }

    @Override
    public void setScale(float scale) {
        this.scale = scale;
    }
}
