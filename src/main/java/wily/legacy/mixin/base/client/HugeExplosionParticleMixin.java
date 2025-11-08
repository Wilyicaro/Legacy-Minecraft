package wily.legacy.mixin.base.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HugeExplosionParticle.class)
public abstract class HugeExplosionParticleMixin extends SingleQuadParticle {
    @Shadow
    @Final
    private SpriteSet sprites;

    protected HugeExplosionParticleMixin(ClientLevel clientLevel, double d, double e, double f, TextureAtlasSprite textureAtlasSprite) {
        super(clientLevel, d, e, f, textureAtlasSprite);
    }

    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), ordinal = 0)
    protected float init(float h) {
        return h * 0.6f;
    }


    @Override
    public void extract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f) {
        this.setSpriteFromAge(this.sprites);
        super.extract(quadParticleRenderState, camera, f);
    }
}
