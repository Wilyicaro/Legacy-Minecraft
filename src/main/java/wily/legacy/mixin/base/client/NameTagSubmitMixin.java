package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.LegacyNameTag;

@Mixin(SubmitNodeStorage.NameTagSubmit.class)
public class NameTagSubmitMixin implements LegacyNameTag {
    float[] nameTagColor = null;

    @Override
    public float[] getNameTagColor() {
        return nameTagColor;
    }

    @Override
    public void setNameTagColor(float[] color) {
        nameTagColor = color;
    }
}
