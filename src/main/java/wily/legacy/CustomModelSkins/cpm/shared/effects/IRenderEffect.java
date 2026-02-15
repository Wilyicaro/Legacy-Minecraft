package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public interface IRenderEffect {
    void load(IOHelper in) throws IOException;

    void write(IOHelper out) throws IOException;

    void apply(ModelDefinition def);

    RenderEffects getEffect();
}
