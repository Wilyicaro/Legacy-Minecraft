//? if fabric {
package wily.legacy.mixin.base.compat.indigo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.BedrockSnowyLeaves;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl", remap = false)
public abstract class AltModelBlockRendererImplMixin {
    @ModifyReturnValue(method = "computeTintColor", at = @At("RETURN"), require = 0)
    private int legacy$applySnowyLeaves(int color, @Local(argsOnly = true) BlockAndTintGetter level, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        return BedrockSnowyLeaves.applyTint(state, level, pos, color);
    }
}
//?}
