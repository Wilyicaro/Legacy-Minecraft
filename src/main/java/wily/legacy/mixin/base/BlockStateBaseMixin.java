package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "getOffset", at = @At("HEAD"), cancellable = true)
    private void getOffset(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        if (asState().getBlock() instanceof TallFlowerBlock) {
            cir.setReturnValue(Vec3.ZERO);
        }
    }
}
