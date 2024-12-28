package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseAshSmokeParticle.class)
public abstract class BaseAshSmokeParticleMixin extends TextureSheetParticle {

    @Shadow @Final private SpriteSet sprites;

    protected BaseAshSmokeParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }
    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
        this.setSpriteFromAge(this.sprites);
        super.render(vertexConsumer, camera, f);
    }
}
