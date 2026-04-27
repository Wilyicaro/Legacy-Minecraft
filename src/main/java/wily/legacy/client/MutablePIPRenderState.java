package wily.legacy.client;

import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;

public interface MutablePIPRenderState {
    static MutablePIPRenderState of(PictureInPictureRenderState renderState) {
        return (MutablePIPRenderState) renderState;
    }

    void setPose(Matrix3x2f pose);

    void setScale(float scale);
}
