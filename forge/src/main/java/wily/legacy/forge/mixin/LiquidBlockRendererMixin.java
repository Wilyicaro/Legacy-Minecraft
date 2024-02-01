package wily.legacy.forge.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Redirect(method = "tesselate",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDFFFFFFI)V", remap = false))
    public void tesselate(LiquidBlockRenderer instance, VertexConsumer vertexConsumer, double d, double e, double f, float g, float h, float i, float alpha, float j, float k, int l, BlockAndTintGetter getter, BlockPos pos, VertexConsumer arg3, BlockState state, FluidState arg5) {
        LevelReader reader = getter instanceof RenderChunkRegion r ? r.level : getter instanceof LevelAccessor a ? a : null;
        vertexConsumer.vertex(d, e, f).color(g, h, i,arg5.is(FluidTags.WATER) ?  LegacyBiomeOverride.getOrDefault(reader.getBiome(pos).unwrapKey()).waterTransparency() : alpha).uv(j, k).uv2(l).normal(0.0f, 1.0f, 0.0f).endVertex();
    }
}
