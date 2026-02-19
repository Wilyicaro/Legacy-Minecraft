package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpl.math.MathHelper;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.ScaleData;

import java.io.IOException;

@Deprecated
public class ModelPartScale implements IModelPart, IResolvedModelPart {
    private float scale;

    public ModelPartScale(IOHelper in, ModelDefinition def) throws IOException {
        this(in);
    }

    public ModelPartScale(IOHelper in) throws IOException {
        scale = in.readByte() / 10f;
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeByte(MathHelper.clamp((int) (scale * 10), Byte.MIN_VALUE, Byte.MAX_VALUE));
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.SCALE;
    }

    @Override
    public void apply(ModelDefinition def) {
        def.setScale(new ScaleData(scale));
    }
}
