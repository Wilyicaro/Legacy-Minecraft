package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class EffectRemoveArmorOffset implements IRenderEffect {
    private boolean remove;

    public EffectRemoveArmorOffset() {
    }

    @Override
    public void load(IOHelper in) throws IOException {
        remove = in.readBoolean();
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeBoolean(remove);
    }

    @Override
    public void apply(ModelDefinition def) {
        def.removeArmorOffset = remove;
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.REMOVE_ARMOR_OFFSET;
    }
}
