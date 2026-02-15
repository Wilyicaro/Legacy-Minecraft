package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;


public class SkipPart implements IModelPart, IResolvedModelPart {

    public SkipPart(IOHelper in, ModelDefinition def) throws IOException {

    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {

    }

    @Override
    public ModelPartType getType() {

        return ModelPartType.END;
    }
}
