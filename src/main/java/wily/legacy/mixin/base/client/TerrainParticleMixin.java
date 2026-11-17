package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.BedrockSnowyLeaves;

@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin {
    @WrapOperation(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockTintSource;colorAsTerrainParticle(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int legacy$applySnowyLeaves(BlockTintSource source, BlockState state, BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        return BedrockSnowyLeaves.applyParticleTint(state, level, pos, original.call(source, state, level, pos));
    }
}
