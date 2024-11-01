package wily.legacy.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.forge.Legacy4JForgeClient;

import java.util.function.Consumer;

@Mixin(ForgeMod.class)
public class ForgeModMixin {

    @Inject(method = "lambda$static$15", at = @At("RETURN"), cancellable = true)
    private static void init(CallbackInfoReturnable<FluidType> cir){
        cir.setReturnValue(new FluidType(FluidType.Properties.create().descriptionId("block.minecraft.water").fallDistanceModifier(0F).canExtinguish(true).canConvertToSource(true).supportsBoating(true).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY).sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH).canHydrate(true)) {
            @Override
            public @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
                return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
            }

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(Legacy4JForgeClient.CLIENT_WATER_FLUID_TYPE);
            }
        });
    }
}
