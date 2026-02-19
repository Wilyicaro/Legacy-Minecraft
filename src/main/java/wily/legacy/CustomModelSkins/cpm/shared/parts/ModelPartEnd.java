package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class ModelPartEnd implements IModelPart {
    public static final ModelPartEnd END = new ModelPartEnd();

    public ModelPartEnd(IOHelper in, ModelDefinition def) throws IOException {
    }

    private ModelPartEnd() {
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return null;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.END;
    }
}
