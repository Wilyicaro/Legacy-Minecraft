package wily.legacy.fabric.mixin.sodium;

import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(FluidRendererImpl.FabricFactory.class)
public class FluidRendererFactoryMixin {
    @Inject(method = "getWaterColorProvider", at = @At("HEAD"), cancellable = true, remap = false)
    public void getWaterColorProvider(CallbackInfoReturnable<BlendedColorProvider<FluidState>> cir) {
        cir.setReturnValue(new BlendedColorProvider<>() {
            @Override
            protected int getColor(LevelSlice levelSlice, FluidState fluidState, BlockPos blockPos) {
                return LegacyBiomeOverride.getOrDefault(levelSlice.getBiomeFabric(blockPos).unwrapKey()).getWaterARGBOrDefault(BiomeColors.getAverageWaterColor(levelSlice,blockPos));
            }
        });
    }
    @Inject(method = "getWaterBlockColorProvider", at = @At("HEAD"), cancellable = true, remap = false)
    public void getWaterBlockColorProvider(CallbackInfoReturnable<BlendedColorProvider<FluidState>> cir) {
        cir.setReturnValue(new BlendedColorProvider<>() {
            @Override
            protected int getColor(LevelSlice levelSlice, FluidState fluidState, BlockPos blockPos) {
                return LegacyBiomeOverride.getOrDefault(levelSlice.getBiomeFabric(blockPos).unwrapKey()).getWaterARGBOrDefault(BiomeColors.getAverageWaterColor(levelSlice,blockPos));
            }
        });
    }
}
