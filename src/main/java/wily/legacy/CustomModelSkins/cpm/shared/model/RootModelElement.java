package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.BoundingBox;
import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;

public class RootModelElement extends RenderedCube {
    private final VanillaModelPart part;
    public Vec3f posN, defPos;
    public Rotation rotN, defRot;
    private final ModelDefinition def;
    private boolean rotAdd, posAdd;
    public boolean disableVanilla;
    private boolean hidden;

    public RootModelElement(VanillaModelPart part, ModelDefinition def) {
        this.part = part;
        this.def = def;
        posN = new Vec3f();
        rotN = new Rotation();
        pos = new Vec3f();
        rotation = new Rotation();
        defPos = new Vec3f();
        defRot = new Rotation();
    }

    public VanillaModelPart getPart() {
        return part;
    }

    @Override
    public void reset() {
        pos = new Vec3f();
        rotation = new Rotation();
        renderScale = new Vec3f(1, 1, 1);
        display = !hidden;
        posAdd = rotAdd = true;
    }

    @Override
    public void setColor(float x, float y, float z) {
    }

    @Override
    public void setVisible(boolean v) {
    }

    @Override
    public int getId() {
        return part.getId(this);
    }

    public BoundingBox getBounds() {
        PartValues v = part.getDefaultSize(def.getSkinType());
        float f = 0.001f, g = f * 2, scale = 1 / 16f;
        Vec3f o = v.getOffset(), s = v.getSize();
        return BoundingBox.create(o.x * scale - f, o.y * scale - f, o.z * scale - f, s.x * scale + g, s.y * scale + g, s.z * scale + g);
    }

    public void setPosAndRot(float px, float py, float pz, float rx, float ry, float rz) {
        if (!disableVanilla) {
            defPos.x = px + posN.x;
            defPos.y = py + posN.y;
            defPos.z = pz + posN.z;
            defRot.x = rx + rotN.x;
            defRot.y = ry + rotN.y;
            defRot.z = rz + rotN.z;
        } else {
            PartValues v = part.getDefaultSize(def.getSkinType());
            Vec3f p = v.getPos();
            defPos.x = p.x + posN.x;
            defPos.y = p.y + posN.y;
            defPos.z = p.z + posN.z;
            defRot.x = rotN.x;
            defRot.y = rotN.y;
            defRot.z = rotN.z;
        }
    }

    public void setPosAndRot(PartRoot cpy) {
        if (!disableVanilla) {
            RootModelElement e = cpy.getMainRoot();
            defPos.x = e.defPos.x - e.posN.x + posN.x;
            defPos.y = e.defPos.y - e.posN.y + posN.y;
            defPos.z = e.defPos.z - e.posN.z + posN.z;
            defRot.x = e.defRot.x - e.rotN.x + rotN.x;
            defRot.y = e.defRot.y - e.rotN.y + rotN.y;
            defRot.z = e.defRot.z - e.rotN.z + rotN.z;
        } else {
            PartValues v = part.getDefaultSize(def.getSkinType());
            Vec3f p = v.getPos();
            defPos.x = p.x + posN.x;
            defPos.y = p.y + posN.y;
            defPos.z = p.z + posN.z;
            defRot.x = rotN.x;
            defRot.y = rotN.y;
            defRot.z = rotN.z;
        }
    }

    @Override
    public void setPosition(boolean add, float x, float y, float z) {
        super.setPosition(add, x, y, z);
        posAdd &= add;
    }

    @Override
    public void setRotation(boolean add, float x, float y, float z) {
        super.setRotation(add, x, y, z);
        rotAdd &= add;
    }

    public Vec3f getPos() {
        return posAdd ? pos.add(defPos) : pos;
    }

    public Rotation getRot() {
        return rotAdd ? rotation.add(defRot) : rotation;
    }

    public boolean renderPart() {
        return true;
    }

    @Override
    public Vec3f getRenderScale() {
        return new Vec3f(1, 1, 1);
    }

    @Override
    public Vec3f getTransformPosition() {
        return getPos();
    }

    @Override
    public Rotation getTransformRotation() {
        return getRot();
    }

    @Override
    public void setHidden(boolean v) {
        hidden = v;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }
}
