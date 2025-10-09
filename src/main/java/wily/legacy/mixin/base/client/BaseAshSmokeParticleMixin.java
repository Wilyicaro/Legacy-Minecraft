package wily.legacy.mixin.base.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseAshSmokeParticle.class)
public abstract class BaseAshSmokeParticleMixin extends SingleQuadParticle {

    @Shadow @Final private SpriteSet sprites;

    protected BaseAshSmokeParticleMixin(ClientLevel clientLevel, double d, double e, double f, TextureAtlasSprite textureAtlasSprite) {
        super(clientLevel, d, e, f, textureAtlasSprite);
    }

    @Override
    public void extract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f) {
        this.setSpriteFromAge(this.sprites);
        super.extract(quadParticleRenderState, camera, f);
    }
}
