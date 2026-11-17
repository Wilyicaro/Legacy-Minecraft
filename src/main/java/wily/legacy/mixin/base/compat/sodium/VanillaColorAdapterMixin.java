//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.BedrockSnowyLeaves;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.color.DefaultColorProviders$VanillaAdapter", remap = false)
public abstract class VanillaColorAdapterMixin {
    @WrapOperation(method = "getColors(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos$MutableBlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;[IZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockTintSource;colorInWorld(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"), require = 0)
    private int legacy$applySnowyLeaves(BlockTintSource source, BlockState state, BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        return BedrockSnowyLeaves.applyTint(state, level, pos, original.call(source, state, level, pos));
    }
}
//?}
