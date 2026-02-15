package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class ModelPartAnimationNewStatic implements IModelPart {
    public ModelPartAnimationNewStatic(IOHelper in) throws IOException {
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
        return ModelPartType.ANIMATION_NEW;
    }
}
