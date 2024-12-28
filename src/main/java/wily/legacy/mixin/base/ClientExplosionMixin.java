//? if <1.21.2 {
/*package wily.legacy.mixin.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ClientExplosionMixin {
    @Shadow @Final private Level level;

    @Shadow public abstract boolean interactsWithBlocks();

    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;

    @Shadow @Final private double x;

    @Shadow @Final private double y;

    @Shadow @Final private double z;

    @Shadow @Final private float radius;

    @Inject(method = "finalizeExplosion",at = @At("RETURN"))
    public void finalizeExplosion(boolean bl, CallbackInfo ci) {
        if (level.isClientSide && this.interactsWithBlocks()) {
            double d = Math.sqrt(Minecraft.getInstance().player.distanceToSqr(x,y,z));
            if (d >= 21) return;
            for (BlockPos blockPos : toBlow) {
                if (level.random.nextInt(d >= 15 ? 40 : 10) != 0) continue;
                double d0 = blockPos.getX() + level.random.nextFloat();
                double d1 = blockPos.getY() + level.random.nextFloat();
                double d2 = blockPos.getZ() + level.random.nextFloat();
                double d3 = d0 - this.x;
                double d4 = d1 - this.y;
                double d5 = d2 - this.z;
                double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                d3 = d3 / d6;
                d4 = d4 / d6;
                d5 = d5 / d6;
                double d7 = 0.5D / (d6 / (double)this.radius + 0.1D);
                d7 = d7 * (double)(level.random.nextFloat() * level.random.nextFloat() + 0.3F);
                d3 = d3 * d7;
                d4 = d4 * d7;
                d5 = d5 * d7;
                if (d <= 18) level.addParticle(ParticleTypes.POOF, (d0 + this.x) / 2.0D, (d1 + this.y) / 2.0D, (d2 + this.z) / 2.0D, d3, d4, d5);
                level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
            }
        }
    }
}
*///?}
