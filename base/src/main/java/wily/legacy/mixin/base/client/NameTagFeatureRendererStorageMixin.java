package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.LegacyNameTag;

@Mixin(NameTagFeatureRenderer.Storage.class)
public class NameTagFeatureRendererStorageMixin {
    @ModifyArg(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    public Object add(Object e) {
        if (e instanceof SubmitNodeStorage.NameTagSubmit submit) {
            LegacyNameTag.of(submit).copyFrom(LegacyNameTag.NEXT_SUBMIT);
        }
        return e;
    }
}
