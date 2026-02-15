package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;

import java.io.IOException;

@Deprecated
public class EffectHide implements IRenderEffect {
    private int id;

    public EffectHide() {
    }

    @Override
    public void load(IOHelper in) throws IOException {
        id = in.readVarInt();
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeVarInt(id);
    }

    @Override
    public void apply(ModelDefinition def) {
        RenderedCube cube = def.getElementById(id);
        if (cube != null) {
            cube.setHidden(true);
        }
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.HIDE;
    }

    @Override
    public String toString() {
        return "Hide [" + id + "]";
    }
}
