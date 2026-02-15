package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.Cube;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.util.TextureStitcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ModelPartDefinition implements IModelPart, IResolvedModelPart, PartCollection {
    private List<Cube> cubes;
    private List<RenderedCube> rc;
    private List<IModelPart> otherParts;
    private List<IResolvedModelPart> resolvedOtherParts;

    public ModelPartDefinition(IOHelper is, ModelDefinition def) throws IOException {
        try {
            int count = is.readVarInt();
            cubes = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Cube c = Cube.loadDefinitionCube(is);
                c.id = i + 10;
                cubes.add(c);
            }
        } catch (IOException e) {
            is.reset();
            int count = is.read();
            cubes = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Cube c = Cube.loadDefinitionCube(is);
                c.id = i + 10;
                cubes.add(c);
            }
        }
        rc = Cube.resolveCubes(cubes);
        otherParts = new ArrayList<>();
        while (true) {
            IModelPart part = is.readObjectBlock(ModelPartType.VALUES, (t, d) -> t.getFactory().create(d, def));
            if (part == null) continue;
            if (part instanceof ModelPartEnd) break;
            switch (part.getType()) {
                case DEFINITION:
                case DEFINITION_LINK:
                case SKIN_LINK:
                    throw new IOException("Invalid tag in definition");
                case END:
                    break;
                default:
                    otherParts.add(part);
                    break;
            }
        }
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        resolvedOtherParts = new ArrayList<>();
        for (IModelPart t : otherParts) resolvedOtherParts.add(t.resolve());
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeVarInt(cubes.size());
        List<Cube> lst = new ArrayList<>(cubes);
        lst.sort((a, b) -> Integer.compare(a.id, b.id));
        for (Cube cube : lst) Cube.saveDefinitionCube(dout, cube);
        for (IModelPart part : otherParts) dout.writeObjectBlock(part);
        dout.writeObjectBlock(ModelPartEnd.END);
    }

    @Override
    public void preApply(ModelDefinition def) {
        def.addCubes(rc);
        resolvedOtherParts.forEach(p -> p.preApply(def));
    }

    @Override
    public void apply(ModelDefinition def) {
        resolvedOtherParts.forEach(p -> p.apply(def));
    }

    @Override
    public void stitch(TextureStitcher stitcher) {
        resolvedOtherParts.forEach(p -> p.stitch(stitcher));
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.DEFINITION;
    }

    @Override
    public String toString() {
        StringBuilder bb = new StringBuilder("PartDefinition\n\tCubes: ").append(cubes.size()).append("\n\tParts:");
        for (IModelPart p : otherParts) bb.append("\n\t\t").append(p.toString().replace("\n", "\n\t\t"));
        return bb.toString();
    }

    public void writePackage(IOHelper dout) throws IOException {
        write(dout);
    }

    public IModelPart toLink(Link link) {
        return new ModelPartDefinitionLink(link);
    }

    @Override
    public void writeBlocks(IOHelper dout) throws IOException {
        dout.writeObjectBlock(this);
    }
}
