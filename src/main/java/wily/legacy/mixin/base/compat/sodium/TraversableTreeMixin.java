//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.config.LegacyCommonOptions;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.tree.TraversableTree", remap = false)
public abstract class TraversableTreeMixin {
    @Inject(method = "cylindricalDistanceTest", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacy$useSquareViewDistance(float x, float y, float z, float limit, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) {
            cir.setReturnValue(Math.abs(x) < limit && Math.abs(y) < limit && Math.abs(z) < limit);
        }
    }
}
//?}
