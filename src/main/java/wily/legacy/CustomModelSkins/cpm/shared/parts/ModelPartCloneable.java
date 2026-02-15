package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.definition.SafetyException;
import wily.legacy.CustomModelSkins.cpm.shared.definition.SafetyException.BlockReason;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper.ImageBlock;

import java.io.IOException;

public class ModelPartCloneable implements IModelPart, IResolvedModelPart {
    public String name;
    public String desc;
    public Image icon;

    public ModelPartCloneable(IOHelper is, ModelDefinition def) throws IOException {
        int fields = is.size() == 0 ? 0 : is.readByte();
        if ((fields & 1) != 0) name = is.readUTF();
        if ((fields & 2) != 0) desc = is.readUTF();
        if ((fields & 4) != 0) {
            ImageBlock img = is.readImage();
            if (img.getWidth() > 400) throw new SafetyException(BlockReason.TEXTURE_OVERFLOW);
            if (img.getHeight() > 400) throw new SafetyException(BlockReason.TEXTURE_OVERFLOW);
            img.doReadImage();
            icon = img.getImage();
        }
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        int fields = 0;
        if (name != null && !name.isEmpty()) fields |= 1;
        if (desc != null && !desc.isEmpty()) fields |= 2;
        if (icon != null) fields |= 4;
        dout.writeByte(fields);
        if ((fields & 1) != 0) dout.writeUTF(name);
        if ((fields & 2) != 0) dout.writeUTF(desc);
        if ((fields & 4) != 0) dout.writeImage(icon);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.CLONEABLE;
    }

    @Override
    public void apply(ModelDefinition def) {
        def.setCloneable(this);
    }
}
