package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

@Deprecated
public class ModelPartDefinitionLink implements IModelPart {
    private final Link link;

    public ModelPartDefinitionLink(IOHelper in, ModelDefinition def) throws IOException {
        this.link = new Link(in);
    }

    public ModelPartDefinitionLink(Link link) {
        this.link = link;
    }

    @Override
    public IResolvedModelPart resolve() {
        return IResolvedModelPart.EMPTY;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        if (link != null) link.write(dout);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.DEFINITION_LINK;
    }
}
