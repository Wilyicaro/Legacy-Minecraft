package wily.legacy.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyVariable(method = "drawStars", at = @At(value = "STORE"), ordinal = 4)
    private float drawStars(float original) {
        return original - 0.05f;
    }
}
