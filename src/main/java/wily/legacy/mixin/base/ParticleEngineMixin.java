package wily.legacy.mixin.base;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4JClient;

import java.util.List;

@Mixin(targets = {"net.minecraft.client.particle.ParticleEngine$MutableSpriteSet"})
public class ParticleEngineMixin {
    @Shadow private List<TextureAtlasSprite> sprites;

    @Inject(method="get(II)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", at = @At("HEAD"), cancellable = true)
    public void get(int i, int j, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        cir.setReturnValue(this.sprites.get(Mth.clamp(Math.round((i + FactoryAPIClient.getGamePartialTick(true)) * (this.sprites.size() - 1) / (float)j),0,sprites.size() - 1)));
    }
}
