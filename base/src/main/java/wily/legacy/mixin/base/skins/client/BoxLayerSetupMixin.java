package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.boxloader.BoxAddonLayer;

@Mixin(AvatarRenderer.class)
public abstract class BoxLayerSetupMixin {
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void consoleskins$addBoxAddonLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).consoleskins$getLayers().add(0, new BoxAddonLayer((RenderLayerParent<?, ?>) this));
    }
}
