package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class ModelPartAnimatedTexture implements IModelPart {
    public ModelPartAnimatedTexture(IOHelper in, ModelDefinition def) throws IOException {
    }

    @Override
    public IResolvedModelPart resolve() {
        return IResolvedModelPart.EMPTY;
    }

    @Override
    public void write(IOHelper out) throws IOException {
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.ANIMATED_TEX;
    }
}
