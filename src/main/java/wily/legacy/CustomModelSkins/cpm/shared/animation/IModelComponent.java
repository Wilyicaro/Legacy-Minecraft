package wily.legacy.CustomModelSkins.cpm.shared.animation;

import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;

public interface IModelComponent {
    void reset();

    void setPosition(boolean add, float x, float y, float z);

    void setRotation(boolean add, float x, float y, float z);

    void setVisible(boolean v);

    Vec3f getPosition();

    Rotation getRotation();

    boolean isVisible();

    int getRGB();

    Vec3f getRenderScale();

    void setRenderScale(boolean add, float x, float y, float z);
}
