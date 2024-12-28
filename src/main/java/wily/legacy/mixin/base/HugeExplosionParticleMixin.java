package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HugeExplosionParticle.class)
public abstract class HugeExplosionParticleMixin extends TextureSheetParticle {
    @Shadow @Final private SpriteSet sprites;

    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), ordinal = 0)
    protected float init(float h) {
        return h * 0.6f;
    }

    protected HugeExplosionParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
        this.setSpriteFromAge(this.sprites);
        super.render(vertexConsumer, camera, f);
    }
}
