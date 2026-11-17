//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Arrays;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class DefaultFluidRendererMixin {
    @Shadow
    @Final
    private QuadLightData quadLightData;

    @Shadow
    @Final
    private int[] quadColors;

    @Inject(method = "updateQuad", at = @At("RETURN"))
    private void legacy$useWaterAppearance(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter, Direction direction, ModelQuadFacing facing, float brightness, ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {
        if (!fluidState.is(FluidTags.WATER)) return;
        Arrays.fill(quadLightData.lm, LevelRenderer.getLightCoords(level, pos));
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        int alpha = Math.round(LegacyBiomeOverride.getOrDefault(clientLevel.getBiome(pos).unwrapKey()).getWaterTransparency() * 255);
        for (int i = 0; i < quadColors.length; i++) quadColors[i] = ARGB.color(alpha, quadColors[i]);
    }
}
//?}
