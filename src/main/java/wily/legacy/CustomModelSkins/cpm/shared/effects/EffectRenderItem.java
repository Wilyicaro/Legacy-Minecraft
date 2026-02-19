package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public final class EffectRenderItem implements IRenderEffect {
    @Override
    public void load(IOHelper in) throws IOException {
        in.readVarInt();
        in.readEnum(wily.legacy.CustomModelSkins.cpl.util.ItemSlot.VALUES);
        in.readByte();
    }

    @Override
    public void write(IOHelper out) throws IOException {
        out.writeVarInt(0);
        out.writeEnum(wily.legacy.CustomModelSkins.cpl.util.ItemSlot.HEAD);
        out.writeByte(0);
    }

    @Override
    public void apply(ModelDefinition def) {
    }

    @Override
    public RenderEffects getEffect() {
        return RenderEffects.ITEM;
    }
}
