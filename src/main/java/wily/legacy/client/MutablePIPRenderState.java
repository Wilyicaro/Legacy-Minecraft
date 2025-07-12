package wily.legacy.client;

import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;

public interface MutablePIPRenderState {
    void setPose(Matrix3x2f pose);

    void setScale(float scale);

    static MutablePIPRenderState of(PictureInPictureRenderState renderState) {
        return (MutablePIPRenderState) renderState;
    }
}
