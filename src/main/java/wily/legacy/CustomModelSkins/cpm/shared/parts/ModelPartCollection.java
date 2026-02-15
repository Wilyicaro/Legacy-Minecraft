package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;
import java.util.ArrayList;

public class ModelPartCollection extends ArrayList<IModelPart> implements PartCollection {
    private static final long serialVersionUID = 7943493602412147742L;

    public static class PackageLink extends ModelPartLink {
        public PackageLink(IOHelper in, ModelDefinition def) throws IOException {
            super(in, def);
        }

        private PackageLink(Link link) {
            super(link);
        }

        @Override
        public ModelPartType getType() {
            return ModelPartType.PACKAGE_LINK;
        }
    }

    @Override
    public void writeBlocks(IOHelper dout) throws IOException {
        for (IModelPart part : this) dout.writeObjectBlock(part);
    }

    @Override
    public String toString() {
        StringBuilder bb = new StringBuilder("PartCollection:");
        for (IModelPart p : this) bb.append("\n\t\t").append(p.toString().replace("\n", "\n\t\t"));
        return bb.toString();
    }

    public void writePackage(IOHelper dout) throws IOException {
        writeBlocks(dout);
        dout.writeObjectBlock(ModelPartEnd.END);
    }

    public IModelPart toLink(Link link) {
        return new PackageLink(link);
    }
}
