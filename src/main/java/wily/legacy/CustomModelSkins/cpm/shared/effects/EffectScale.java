package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;

import java.io.IOException;

@Deprecated
public class EffectScale implements IRenderEffect {
    private int id;
    private Vec3f scale;
    private float mcScale;

    public EffectScale() {
    }

    @Override
    public void load(IOHelper in) throws IOException {
        id = in.readVarInt();
        mcScale = in.readFloat2();
        scale = in.readVec6b();
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeVarInt(id);
        out.writeFloat2(mcScale);
        out.writeVec6b(scale);
    }

    @Override
    public void apply(ModelDefinition def) {
        RenderedCube cube = def.getElementById(id);
        if (cube != null) {
            cube.getCube().meshScale = scale;
            cube.getCube().mcScale = mcScale;
        }
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.SCALE;
    }

    @Override
    public String toString() {
        return "Scale [" + id + "] " + scale + "+" + mcScale;
    }
}
