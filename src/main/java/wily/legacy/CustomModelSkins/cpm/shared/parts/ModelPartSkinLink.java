package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

@Deprecated
public class ModelPartSkinLink implements IModelPart {
    public ModelPartSkinLink(IOHelper in, ModelDefinition def) throws IOException {
    }

    @Override
    public IResolvedModelPart resolve() {
        return IResolvedModelPart.EMPTY;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.SKIN_LINK;
    }
}
