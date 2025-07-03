//? if >=1.21.2 {
package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin implements Explosion {
    @Shadow @Final private ServerLevel level;

    @Shadow @Final private Vec3 center;

    @Shadow @Final private float radius;

    @Inject(method = "interactWithBlocks", at = @At("RETURN"))
    private void explode(List<BlockPos> list, CallbackInfo ci){
        for (ServerPlayer player : level.players()) {
            double d = Math.sqrt(player.distanceToSqr(center));
            if (d >= 21) return;
            for (BlockPos blockPos : list) {
                if (level.random.nextInt(d >= 15 ? 40 : 10) != 0) continue;
                double d0 = blockPos.getX() + level.random.nextFloat();
                double d1 = blockPos.getY() + level.random.nextFloat();
                double d2 = blockPos.getZ() + level.random.nextFloat();
                double d3 = d0 - center.x;
                double d4 = d1 - center.y;
                double d5 = d2 - center.z;
                double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                d3 = d3 / d6;
                d4 = d4 / d6;
                d5 = d5 / d6;
                double d7 = 0.5D / (d6 / (double)this.radius + 0.1D);
                d7 = d7 * (double)(level.random.nextFloat() * level.random.nextFloat() + 0.3F);
                d3 = d3 * d7;
                d4 = d4 * d7;
                d5 = d5 * d7;
                if (d <= 18) level.sendParticles(player, ParticleTypes.POOF, false,/*? if >=1.21.4 {*/false, /*?}*/ (d0 + center.x) / 2.0D, (d1 + center.y) / 2.0D, (d2 + center.z) / 2.0D, 0, d3, d4, d5, 1);
                level.sendParticles(player, ParticleTypes.SMOKE, false,/*? if >=1.21.4 {*/false, /*?}*/ d0, d1, d2, 0, d3, d4, d5,1);
            }
        }
    }
}
//?}
