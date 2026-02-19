package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.Cube;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModelPartCubes implements IModelPart, IResolvedModelPart {
    private List<Cube> cubes;
    private List<RenderedCube> rc;

    public ModelPartCubes(IOHelper is, ModelDefinition def) throws IOException {
        int count = is.readVarInt();
        cubes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Cube c = Cube.loadDefinitionCubeV2(is);
            c.id = i + 10;
            cubes.add(c);
        }
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        rc = Cube.resolveCubesV2(cubes);
        return this;
    }

    @Override
    public void preApply(ModelDefinition def) {
        def.addCubes(rc);
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeVarInt(cubes.size());
        List<Cube> lst = new ArrayList<>(cubes);
        lst.sort((a, b) -> Integer.compare(a.id, b.id));
        for (Cube cube : lst) {
            Cube.saveDefinitionCubeV2(dout, cube);
        }
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.CUBES;
    }
}
