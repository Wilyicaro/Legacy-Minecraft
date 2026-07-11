//? if <1.21.2 {
package wily.legacy.mixin.base.skins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.skins.client.util.BirthdayCapeUtil;

@Mixin(CapeLayer.class)
public abstract class BirthdayCapeLayerMixin {
    @ModifyExpressionValue(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;isModelPartShown(Lnet/minecraft/world/entity/player/PlayerModelPart;)Z"
            )
    )
    private boolean consoleskins$showBirthdayCape(boolean showCape) {
        return showCape || BirthdayCapeUtil.isActiveNow();
    }
}
//?}
