//? if >1.20.2 {
package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GustParticle.class)
public abstract class GustParticleMixin extends SingleQuadParticle {

    @Shadow
    @Final
    private SpriteSet sprites;

    protected GustParticleMixin(ClientLevel clientLevel, double d, double e, double f, TextureAtlasSprite textureAtlasSprite) {
        super(clientLevel, d, e, f, textureAtlasSprite);
    }

    @Override
    public void extract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f) {
        this.setSpriteFromAge(this.sprites);
        super.extract(quadParticleRenderState, camera, f);
    }
}
//?}