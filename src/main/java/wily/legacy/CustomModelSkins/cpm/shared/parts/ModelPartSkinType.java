package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;

import java.io.IOException;

public class ModelPartSkinType implements IModelPart, IResolvedModelPart {
    private SkinType type;

    public ModelPartSkinType(IOHelper din, ModelDefinition def) throws IOException {
        int t = din.read();
        this.type = t >= SkinType.VALUES.length ? SkinType.UNKNOWN : SkinType.VALUES[t];
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.write(type.ordinal());
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.SKIN_TYPE;
    }

    @Override
    public String toString() {
        return "Skin Type: " + type.getName();
    }
}
