package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.PlayerModelParts;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelElement;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelType;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;

import java.io.IOException;

public class ModelPartRootInfo implements IModelPart, IResolvedModelPart {
    public static final int ROOT_HIDDEN = 1 << 0, ROOT_CREATE = 1 << 1, ROOT_MODEL = 1 << 2, ROOT_TRANSFORM = 1 << 3, ROOT_DISABLE_VANILLA = 1 << 4;
    private VanillaModelPart root;
    private Vec3f pos, rot;
    private int createFrom;
    private boolean hidden, disableVanilla;

    public ModelPartRootInfo(IOHelper is, ModelDefinition def) throws IOException {
        byte flags = is.readByte();
        root = (flags & ROOT_MODEL) != 0 ? is.readEnum(RootModelType.VALUES) : is.readEnum(PlayerModelParts.VALUES);
        if ((flags & ROOT_TRANSFORM) != 0) {
            pos = is.readVarVec3();
            rot = is.readAngle();
        } else {
            pos = new Vec3f();
            rot = new Vec3f();
        }
        if ((flags & ROOT_CREATE) != 0) createFrom = is.readVarInt();
        hidden = (flags & ROOT_HIDDEN) != 0;
        disableVanilla = (flags & ROOT_DISABLE_VANILLA) != 0;
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void write(IOHelper dout) throws IOException {
        boolean transform = !pos.epsilon(0.1f) || !rot.epsilon(0.1f);
        byte flags = 0;
        if (root instanceof RootModelType) flags |= ROOT_MODEL;
        if (transform) flags |= ROOT_TRANSFORM;
        if (hidden) flags |= ROOT_HIDDEN;
        if (createFrom != 0) flags |= ROOT_CREATE;
        if (disableVanilla) flags |= ROOT_DISABLE_VANILLA;
        dout.writeByte(flags);
        dout.writeEnum((Enum) root);
        if (transform) {
            dout.writeVarVec3(pos);
            Vec3f r = new Vec3f(rot);
            r.x = wrapDeg(r.x);
            r.y = wrapDeg(r.y);
            r.z = wrapDeg(r.z);
            dout.writeAngle(r);
        }
        if (createFrom != 0) dout.writeVarInt(createFrom);
    }

    @Override
    public void preApply(ModelDefinition def) {
        if (createFrom == 0) {
            RootModelElement elem = def.getModelElementFor(root).getMainRoot();
            elem.setHidden(hidden);
            elem.posN = pos;
            elem.rotN = new Rotation(rot, false);
            elem.disableVanilla = disableVanilla;
        }
    }

    @Override
    public void apply(ModelDefinition def) {
        if (createFrom != 0) {
            RootModelElement elem = def.getModelElementFor(PlayerModelParts.CUSTOM_PART).get();
            for (RenderedCube rc : elem.children)
                if (rc.getId() == createFrom) {
                    elem.children.remove(rc);
                    RootModelElement e = def.addRoot(createFrom, root);
                    e.posN = rc.pos;
                    e.rotN = rc.rotation;
                    e.setHidden(hidden);
                    e.disableVanilla = disableVanilla;
                    if (rc.children != null) {
                        e.children.addAll(rc.children);
                        rc.children.forEach(p -> p.setParent(e));
                    }
                    break;
                }
        }
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.ROOT_INFO;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Part Root Info: ").append(root);
        if (hidden) sb.append(" [H]");
        if (disableVanilla) sb.append(" [DV]");
        if (!pos.epsilon(0.1f)) sb.append("\n\tPos: ").append(pos);
        if (!rot.epsilon(0.1f)) sb.append("\n\tRot: ").append(rot);
        if (createFrom != 0) sb.append("\n\tNew Root");
        return sb.toString();
    }

    private static float wrapDeg(float deg) {
        deg = deg % 360.0F;
        if (deg >= 180.0F) deg -= 360.0F;
        if (deg < -180.0F) deg += 360.0F;
        return deg;
    }
}
