package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.MathHelper;
import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.animation.IModelComponent;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.Mesh;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.PerFaceUV;

import java.util.ArrayList;
import java.util.List;

public class RenderedCube implements IModelComponent {
    private Cube cube;
    private RenderedCube parent;
    public List<RenderedCube> children;
    public Vec3f offset, pos, renderScale;
    public Rotation rotation;
    public Mesh renderObject;
    public boolean updateObject, display = true, recolor = false, singleTex = false, extrude = false;
    public PerFaceUV faceUVs;
    public int color;

    protected RenderedCube() {
    }

    public RenderedCube(Cube cube) {
        this.cube = cube;
        reset();
    }

    @Override
    public void reset() {
        if (cube.offset != null) offset = new Vec3f(cube.offset);
        if (cube.rotation != null) rotation = new Rotation(cube.rotation, false);
        if (cube.pos != null) pos = new Vec3f(cube.pos);
        if (cube.scale != null) renderScale = new Vec3f(cube.scale);
        color = recolor || cube.texSize == 0 ? cube.rgb : 0xffffff;
        display = !cube.hidden;
    }

    public void setParent(RenderedCube parent) {
        this.parent = parent;
    }

    public void addChild(RenderedCube cube) {
        if (children == null) children = new ArrayList<>();
        children.add(cube);
    }

    public Cube getCube() {
        return cube;
    }

    public boolean doDisplay() {
        return display;
    }

    public RenderedCube getParent() {
        return parent;
    }

    public int getId() {
        return cube.id;
    }

    @Override
    public void setPosition(boolean add, float x, float y, float z) {
        if (add) {
            pos.x += x;
            pos.y += y;
            pos.z += z;
        } else {
            pos.x = x;
            pos.y = y;
            pos.z = z;
        }
    }

    @Override
    public void setRotation(boolean add, float x, float y, float z) {
        if (add) {
            rotation.x += x;
            rotation.y += y;
            rotation.z += z;
        } else {
            rotation.x = x;
            rotation.y = y;
            rotation.z = z;
        }
    }

    @Override
    public void setVisible(boolean v) {
        display = v;
    }

    public void setColor(float r, float g, float b) {
        if (recolor || cube.texSize == 0)
            color = (MathHelper.clamp((int) r, 0, 255) << 16) | (MathHelper.clamp((int) g, 0, 255) << 8) | MathHelper.clamp((int) b, 0, 255);
    }

    public void setColor(int color) {
        if (recolor || cube.texSize == 0) this.color = color;
    }

    @Override
    public Vec3f getPosition() {
        return pos;
    }

    @Override
    public Rotation getRotation() {
        return rotation;
    }

    @Override
    public boolean isVisible() {
        return display;
    }

    @Override
    public int getRGB() {
        return cube == null ? -1 : (recolor || cube.texSize == 0 ? cube.rgb : -1);
    }

    public static enum ElementSelectMode {
        NULL(false), SELECTED(true), SEL_CHILDREN(true), SEL_ONLY(true);
        private final boolean renderOutline;

        private ElementSelectMode(boolean renderOutline) {
            this.renderOutline = renderOutline;
        }

        public boolean isRenderOutline() {
            return renderOutline;
        }
    }

    @Override
    public Vec3f getRenderScale() {
        return renderScale;
    }

    @Override
    public void setRenderScale(boolean add, float x, float y, float z) {
        if (add) {
            if (x != 0) renderScale.x *= x;
            if (y != 0) renderScale.y *= y;
            if (z != 0) renderScale.z *= z;
        } else {
            if (x != 0) renderScale.x = x;
            if (y != 0) renderScale.y = y;
            if (z != 0) renderScale.z = z;
        }
    }

    public Vec3f getTransformPosition() {
        return getPosition();
    }

    public Rotation getTransformRotation() {
        return getRotation();
    }

    public void setHidden(boolean v) {
        cube.hidden = v;
    }

    public boolean isHidden() {
        return cube.hidden;
    }
}
