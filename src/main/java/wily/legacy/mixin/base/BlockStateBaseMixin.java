package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4JClient;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "getOffset", at = @At("HEAD"), cancellable = true)
    private void getOffset(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        if (legacy$hasModOnServer() && asState().getBlock() instanceof TallFlowerBlock) {
            cir.setReturnValue(Vec3.ZERO);
        }
    }

    @Inject(method = "getLightBlock", at = @At("RETURN"), cancellable = true)
    private void legacy$waterLightBlock(
            //? if <1.21.3 {
            /*BlockGetter level, BlockPos pos,
            *///?}
            CallbackInfoReturnable<Integer> cir) {
        if (asState().getFluidState().is(FluidTags.WATER)) cir.setReturnValue(Math.max(cir.getReturnValueI(), 2));
    }

    @Unique
    private static boolean legacy$hasModOnServer() {
        return !FactoryAPI.isClient() || Legacy4JClient.hasModOnServer();
    }
}
