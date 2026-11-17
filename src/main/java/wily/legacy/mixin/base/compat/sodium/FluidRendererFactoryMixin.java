//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
//? if fabric {
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
//?} else if forge || neoforge {
/*import net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl;
 *///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Arrays;

@Mixin(value = /*? if fabric {*/FluidRendererImpl.FabricFactory /*?} else {*/ /*FluidRendererImpl.ForgeFactory*//*?}*/.class, remap = false)
public class FluidRendererFactoryMixin {
    private static final BlendedColorProvider<FluidState> legacy$waterColorProvider = new BlendedColorProvider<>() {
        @Override
        protected int getColor(LevelSlice levelSlice, FluidState fluidState, BlockPos blockPos) {
            return LegacyBiomeOverride.getOrDefault(Minecraft.getInstance().level.getBiome(blockPos).unwrapKey()).getWaterARGBOrDefault(BiomeColors.getAverageWaterColor(levelSlice, blockPos));
        }

        @Override
        public void getColors(LevelSlice levelSlice, BlockPos blockPos, BlockPos.MutableBlockPos mutableBlockPos, FluidState fluidState, ModelQuadView quad, int[] colors, boolean tint) {
            Arrays.fill(colors, getColor(levelSlice, fluidState, blockPos));
        }
    };

    @Inject(method = "getWaterColorProvider", at = @At("HEAD"), cancellable = true)
    public void getWaterColorProvider(CallbackInfoReturnable<BlendedColorProvider<FluidState>> cir) {
        cir.setReturnValue(legacy$waterColorProvider);
    }

    @Inject(method = "getWaterBlockColorProvider", at = @At("HEAD"), cancellable = true)
    public void getWaterBlockColorProvider(CallbackInfoReturnable<BlendedColorProvider<FluidState>> cir) {
        cir.setReturnValue(legacy$waterColorProvider);
    }
}
//?}
