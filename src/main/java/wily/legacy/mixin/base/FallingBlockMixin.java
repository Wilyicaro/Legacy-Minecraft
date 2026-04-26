package wily.legacy.mixin.base;

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.block.LegacyBlockExtension;

@Mixin(FallingBlock.class)
public class FallingBlockMixin {
    @Inject(method = "isFree", at = @At("HEAD"), cancellable = true)
    private static void isFree(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof LegacyBlockExtension extension)
            cir.setReturnValue(extension.l4j$isFreeForFalling(state));
    }
}
