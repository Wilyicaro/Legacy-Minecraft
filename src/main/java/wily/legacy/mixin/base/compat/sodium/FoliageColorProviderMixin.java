//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.BedrockSnowyLeaves;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.color.DefaultColorProviders$FoliageColorProvider", remap = false)
public abstract class FoliageColorProviderMixin {
    @ModifyReturnValue(method = "getColor", at = @At("RETURN"), require = 0)
    private int legacy$applySnowyLeaves(int color, @Local(argsOnly = true) LevelSlice slice, @Local(argsOnly = true) Object state, @Local(argsOnly = true) BlockPos pos) {
        return state instanceof BlockState blockState ? BedrockSnowyLeaves.applyTint(blockState, slice, pos, color) : color;
    }
}
//?}
