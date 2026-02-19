package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpl.math.Rotation;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelElement;

import java.io.IOException;

@Deprecated
public class ModelPartPlayerPos implements IModelPart, IResolvedModelPart {
    public int id;
    public Vec3f pos, rot;

    public ModelPartPlayerPos(IOHelper in, ModelDefinition def) throws IOException {
        id = in.readVarInt();
        pos = in.readVec6b();
        rot = in.readAngle();
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeVarInt(id);
        dout.writeVec6b(pos);
        dout.writeAngle(rot);
    }

    @Override
    public void apply(ModelDefinition def) {
        RenderedCube e = def.getElementById(id);
        if (e instanceof RootModelElement) {
            RootModelElement elem = (RootModelElement) e;
            elem.posN = pos;
            elem.rotN = new Rotation(rot, false);
        }
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.PLAYER_PARTPOS;
    }

    @Override
    public String toString() {
        return "Root Pos: " + id;
    }
}
