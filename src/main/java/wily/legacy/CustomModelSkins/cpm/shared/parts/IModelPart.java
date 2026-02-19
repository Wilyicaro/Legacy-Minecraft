package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper.ObjectBlock;

import java.io.IOException;

public interface IModelPart extends ObjectBlock<ModelPartType> {
    IResolvedModelPart resolve() throws IOException;

    @Override
    void write(IOHelper dout) throws IOException;

    @Override
    ModelPartType getType();

    @FunctionalInterface
    public static interface Factory {
        IModelPart create(IOHelper in, ModelDefinition def) throws IOException;
    }
}
