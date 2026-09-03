package wily.legacy.mixin.base.client;

//? if >=26.2 {
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.LegacyNameTag;

// 26.2 moved SubmitNodeStorage$NameTagSubmit into its owning renderer as NameTagFeatureRenderer$Submit.
@Mixin(/*? if >=26.2 {*/NameTagFeatureRenderer.Submit.class/*?} else {*//*SubmitNodeStorage.NameTagSubmit.class*//*?}*/)
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
