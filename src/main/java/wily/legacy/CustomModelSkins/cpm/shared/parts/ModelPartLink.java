package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class ModelPartLink implements IModelPart {
    protected final Link link;

    public ModelPartLink(IOHelper in, ModelDefinition def) throws IOException {
        this.link = new Link(in);
    }

    protected ModelPartLink(Link link) {
        this.link = link;
    }

    @Override
    public IResolvedModelPart resolve() {
        return IResolvedModelPart.EMPTY;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        link.write(dout);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.PACKAGE_LINK;
    }

    @Override
    public String toString() {
        return "Link(disabled): " + link;
    }
}
