package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.effects.IRenderEffect;
import wily.legacy.CustomModelSkins.cpm.shared.effects.RenderEffects;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class ModelPartRenderEffect implements IModelPart, IResolvedModelPart {
    private IRenderEffect effect;

    public ModelPartRenderEffect(IOHelper in, ModelDefinition def) throws IOException {
        RenderEffects ef = in.readEnum(RenderEffects.VALUES);
        if (ef != null) {
            effect = ef.create();
            effect.load(in);
        }
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeEnum(effect.getEffect());
        effect.write(dout);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.RENDER_EFFECT;
    }

    @Override
    public void apply(ModelDefinition def) {
        if (effect != null) {
            def.markEffectUsed(effect.getEffect());
            effect.apply(def);
        }
    }

    @Override
    public String toString() {
        return "RenderEffect: " + effect;
    }
}
