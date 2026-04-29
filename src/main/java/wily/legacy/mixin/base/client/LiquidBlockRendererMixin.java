package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(FluidRenderer.class)
public class LiquidBlockRendererMixin {
    //? if <1.20.5 {
    /*@Redirect(method = "tesselate",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/FluidRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDFFFF"  + /^? if neoforge || forge && <1.20.5 {^//^"FFI)V"^//^?} else if forge {^/ /^"FIF)V" ^//^?} else {^/"FI)V"/^?}^//^? if forge || neoforge {^//^, remap = false^//^?}^/))
    public void tesselate(FluidRenderer instance, VertexConsumer vertexConsumer, /^? if <1.20.5 {^//^double d, double e, double f^//^?} else {^/float d, float e, float f/^?}^/, float g, float h, float i,/^? if neoforge || forge && <1.20.5 {^/ /^float alpha, ^//^?}^/float j, float k, int l,/^? if forge && >=1.20.5 {^/ /^float alpha, ^//^?}^/ BlockAndTintGetter getter, BlockPos pos, VertexConsumer arg3, BlockState state, FluidState arg5) {
        LevelReader reader = Minecraft.getInstance().level;
        vertexConsumer.vertex(d, e, f).color(g, h, i, (arg5.is(FluidTags.WATER)) && reader != null ? LegacyBiomeOverride.getOrDefault(reader.getBiome(pos).unwrapKey()).waterTransparency() : /^? if forge || neoforge {^/ /^alpha^//^?} else {^/1.0f/^?}^/).uv(j, k).uv2(l).normal(0.0f, 1.0f, 0.0f).endVertex();
    }
    *///?} else {
    @ModifyExpressionValue(method = "tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V", at = @At(value = "INVOKE", target = /*? if neoforge && >=26.1 {*/ /*"Lnet/neoforged/neoforge/client/fluid/FluidTintSource;colorInWorld(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I" *//*?} else {*/ "Lnet/minecraft/client/color/block/BlockTintSource;colorInWorld(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"/*?}*/))
    public int tesselate(int color, BlockAndTintGetter getter, BlockPos pos, FluidRenderer.Output output, BlockState state, FluidState fluidState) {
        LevelReader reader = Minecraft.getInstance().level;
        int alpha = fluidState.is(FluidTags.WATER) && reader != null ? Math.round(LegacyBiomeOverride.getOrDefault(reader.getBiome(pos).unwrapKey()).getWaterTransparency() * 255) : color >>> 24;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    //?}
}
