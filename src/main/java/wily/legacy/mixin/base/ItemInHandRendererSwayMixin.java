package wily.legacy.mixin.base;

import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererSwayMixin {
    @ModifyArg(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"))
    public float renderHandsWithItems(float f) {
        return f * 0.5f;
    }
}
