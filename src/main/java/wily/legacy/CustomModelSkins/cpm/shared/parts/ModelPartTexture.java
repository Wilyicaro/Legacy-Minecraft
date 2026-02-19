package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.TextureSheetType;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureProvider;

import java.io.IOException;

public class ModelPartTexture implements IModelPart, IResolvedModelPart {
    private TextureProvider image;
    private TextureSheetType texType;

    public ModelPartTexture(IOHelper in, ModelDefinition def) throws IOException {
        texType = in.readEnum(TextureSheetType.VALUES);
        image = new TextureProvider(in, def);
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void preApply(ModelDefinition def) {
        def.setTexture(texType, image);
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeEnum(texType);
        image.write(dout);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.TEXTURE;
    }

    @Override
    public String toString() {
        return "Texture: " + texType;
    }
}
