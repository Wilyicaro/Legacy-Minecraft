package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.TextureSheetType;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureProvider;

import java.io.IOException;

@Deprecated
public class ModelPartSkin implements IModelPart, IResolvedModelPart {
    private TextureProvider image;

    public ModelPartSkin(IOHelper in, ModelDefinition def) throws IOException {
        image = new TextureProvider(in, def);
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void preApply(ModelDefinition def) {
        def.setTexture(TextureSheetType.SKIN, image);
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        image.write(dout);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.SKIN;
    }
}
