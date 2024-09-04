package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = {"net.minecraft.client.particle.ParticleEngine$MutableSpriteSet"})
public class ParticleEngineMixin {
    @Shadow private List<TextureAtlasSprite> sprites;

    @Inject(method="get(II)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", at = @At("HEAD"), cancellable = true)
    public void get(int i, int j, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        cir.setReturnValue(this.sprites.get(Math.clamp(Math.round((i + (Minecraft.getInstance().isPaused() ? Minecraft.getInstance().pausePartialTick : Minecraft.getInstance().getFrameTime())) * (this.sprites.size() - 1) / (float)j),0,sprites.size() - 1)));
    }
}
