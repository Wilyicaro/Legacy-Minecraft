package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public class EffectHideSkull implements IRenderEffect {
    private boolean hide;

    public EffectHideSkull() {
    }

    @Override
    public void load(IOHelper in) throws IOException {
        hide = in.readBoolean();
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeBoolean(hide);
    }

    @Override
    public void apply(ModelDefinition def) {
        def.hideHeadIfSkull = hide;
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.HIDE_SKULL;
    }
}
