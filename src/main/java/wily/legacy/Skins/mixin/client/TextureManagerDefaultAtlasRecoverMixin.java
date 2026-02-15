package wily.legacy.Skins.mixin.client;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import wily.legacy.Skins.skin.DefaultAtlasSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public class TextureManagerDefaultAtlasRecoverMixin {

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void consoleskins$preventReleaseDefaultAtlasDyn(ResourceLocation id, CallbackInfo ci) {
        if (DefaultAtlasSkins.isDefaultAtlasDyn(id)) {
            ci.cancel();
        }
    }

    @Inject(method = "getTexture", at = @At("RETURN"), cancellable = true)
    private void consoleskins$recoverDefaultAtlasDyn(ResourceLocation id, CallbackInfoReturnable<AbstractTexture> cir) {
        if (!DefaultAtlasSkins.isDefaultAtlasDyn(id)) return;

        if (cir.getReturnValue() instanceof DynamicTexture) return;

        Minecraft mc = Minecraft.getInstance();
        DynamicTexture dt = DefaultAtlasSkins.recreate(mc.getResourceManager(), id);
        if (dt == null) return;

        mc.getTextureManager().register(id, dt);
        cir.setReturnValue(dt);
    }
}
