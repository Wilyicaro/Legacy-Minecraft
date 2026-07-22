package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShulkerBulletRenderer.class)
public class ShulkerBulletRendererMixin {
    @ModifyArg(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), index = 6)
    private int legacy$outerGlowColor(int color) {
        return 0x80FFFFFF;
    }
}
