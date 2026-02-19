package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.model.PlayerModelParts;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelElement;

import java.io.IOException;

@Deprecated
public class ModelPartPlayer implements IModelPart, IResolvedModelPart {
    private boolean[] keep = new boolean[8];

    public ModelPartPlayer(IOHelper in, ModelDefinition def) throws IOException {
        int keep = in.read();
        for (int i = 0; i < this.keep.length; i++) {
            this.keep[i] = (keep & (1 << i)) != 0;
        }
    }

    @Override
    public void preApply(ModelDefinition def) {
        for (int i = 0; i < PlayerModelParts.VALUES.length; i++) {
            RootModelElement elem = def.getModelElementFor(PlayerModelParts.VALUES[i]).getMainRoot();
            elem.setHidden(!keep[i]);
        }
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        int v = 0;
        for (int i = 0; i < this.keep.length; i++) {
            if (keep[i]) v |= (1 << i);
        }
        dout.write(v);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.PLAYER;
    }
}
