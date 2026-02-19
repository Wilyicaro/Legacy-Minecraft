package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;

public enum RootModelValues implements PartValues {
    CAPE(RootModelType.CAPE, 0, 0, 0, -5, 0, -1, 10, 16, 1, 0, 0, 0, false), A_HELMET(RootModelType.ARMOR_HELMET, 0, 0, 0, -4, -8, -4, 8, 8, 8, 0, 0, 1, false), A_BODY(RootModelType.ARMOR_BODY, 0, 0, 0, -4, 0, -2, 8, 12, 4, 16, 16, 1, false), A_LEFT_ARM(RootModelType.ARMOR_LEFT_ARM, 5, 2, 0, -1, -2, -2, 4, 12, 4, 40, 16, 1, true), A_RIGHT_ARM(RootModelType.ARMOR_RIGHT_ARM, -5, 2, 0, -3, -2, -2, 4, 12, 4, 40, 16, 1, false), A_LEGS_BODY(RootModelType.ARMOR_LEGGINGS_BODY, 0, 0, 0, -4, 0, -2, 8, 12, 4, 16, 16, .5f, false), A_LEFT_LEG(RootModelType.ARMOR_LEFT_LEG, 1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, .5f, true), A_RIGHT_LEG(RootModelType.ARMOR_RIGHT_LEG, -1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, .5f, false), A_LEFT_FOOT(RootModelType.ARMOR_LEFT_FOOT, 1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, 1, true), A_RIGHT_FOOT(RootModelType.ARMOR_RIGHT_FOOT, -1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, 1, false),
    ;
    public static final RootModelValues[] VALUES = values();
    public final RootModelType type;
    public final float px, py, pz, ox, oy, oz, sx, sy, sz, mcscale;
    public final int u, v;
    public boolean mirror;

    private RootModelValues(RootModelType type, float px, float py, float pz, float ox, float oy, float oz, float sx, float sy, float sz, int u, int v, float mcscale, boolean mirror) {
        this.type = type;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.u = u;
        this.v = v;
        this.mcscale = mcscale;
        this.mirror = mirror;
    }

    public Vec3f getPos() {
        return new Vec3f(px, py, pz);
    }

    public Vec3f getOffset() {
        return new Vec3f(ox, oy, oz);
    }

    public Vec3f getSize() {
        return new Vec3f(sx, sy, sz);
    }

    public Vec2i getUV() {
        return new Vec2i(u, v);
    }

    public boolean isMirror() {
        return mirror;
    }

    public float getMCScale() {
        return mcscale;
    }

    public static RootModelValues getFor(RootModelType part, SkinType skinType) {
        for (RootModelValues v : VALUES) {
            if (v.type == part) return v;
        }
        return null;
    }
}
