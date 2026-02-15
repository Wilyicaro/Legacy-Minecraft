package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.PerFaceUV;

import java.io.IOException;

public class EffectPerFaceUV implements IRenderEffect {
    private int id;
    private PerFaceUV uv;

    public EffectPerFaceUV() {
    }

    @Override
    public void load(IOHelper in) throws IOException {
        id = in.readVarInt();
        uv = new PerFaceUV();
        uv.readFaces(in);
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeVarInt(id);
        uv.writeFaces(out);
    }

    @Override
    public void apply(ModelDefinition def) {
        RenderedCube cube = def.getElementById(id);
        if (cube != null) {
            cube.faceUVs = uv;
        }
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.PER_FACE_UV;
    }

    @Override
    public String toString() {
        return "Per Face UV [" + id + "] " + uv;
    }
}
