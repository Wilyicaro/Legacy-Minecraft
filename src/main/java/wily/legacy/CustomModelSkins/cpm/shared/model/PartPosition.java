package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;

public class PartPosition {
    protected Vec3f rPos = new Vec3f(), rScale = new Vec3f();
    protected Rotation rRotation = new Rotation();

    public Vec3f getRPos() {
        return rPos;
    }

    public Rotation getRRotation() {
        return rRotation;
    }

    public Vec3f getRScale() {
        return rScale;
    }
}
