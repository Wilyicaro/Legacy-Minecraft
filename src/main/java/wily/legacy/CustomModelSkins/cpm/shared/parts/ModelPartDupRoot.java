package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.PlayerModelParts;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelElement;

import java.io.IOException;

@Deprecated
public class ModelPartDupRoot implements IModelPart, IResolvedModelPart {
    private int id;
    private PlayerModelParts type;

    public ModelPartDupRoot(IOHelper din, ModelDefinition def) throws IOException {
        id = din.readVarInt();
        type = din.readEnum(PlayerModelParts.VALUES);
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeVarInt(id);
        dout.writeEnum(type);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.DUP_ROOT;
    }

    @Override
    public void apply(ModelDefinition def) {
        RootModelElement elem = def.getModelElementFor(PlayerModelParts.CUSTOM_PART).get();
        for (RenderedCube rc : elem.children) {
            if (rc.getId() == id) {
                elem.children.remove(rc);
                RootModelElement e = def.addRoot(id, type);
                e.posN = rc.pos;
                e.rotN = rc.rotation;
                e.setHidden(rc.isHidden());
                if (rc.children != null) {
                    e.children.addAll(rc.children);
                    rc.children.forEach(p -> p.setParent(e));
                }
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "DupRoot: " + type + " " + id;
    }
}
