package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyBiomeOverride;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    //? if <1.20.5 {
    /*@Redirect(method = "tesselate",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDFFFF"  + /^? if neoforge || forge && <1.20.5 {^//^"FFI)V"^//^?} else if forge {^//^ "FIF)V" ^//^?} else {^/"FI)V"/^?}^//^? if forge || neoforge {^//^, remap = false^//^?}^/))
    public void tesselate(LiquidBlockRenderer instance, VertexConsumer vertexConsumer, /^? if <1.20.5 {^//^double d, double e, double f^//^?} else {^/float d, float e, float f/^?}^/, float g, float h, float i,/^? if neoforge || forge && <1.20.5 {^/ /^float alpha, ^//^?}^/float j, float k, int l,/^? if forge && >=1.20.5 {^//^ float alpha, ^//^?}^/ BlockAndTintGetter getter, BlockPos pos, VertexConsumer arg3, BlockState state, FluidState arg5) {
        LevelReader reader = Minecraft.getInstance().level;
        vertexConsumer.vertex(d, e, f).color(g, h, i, (arg5.is(FluidTags.WATER)) && reader != null ? LegacyBiomeOverride.getOrDefault(reader.getBiome(pos).unwrapKey()).waterTransparency() : /^? if forge || neoforge {^/ /^alpha^//^?} else {^/1.0f/^?}^/).uv(j, k).uv2(l).normal(0.0f, 1.0f, 0.0f).endVertex();
    }
    *///?} else {
    @Redirect(method = "tesselate",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFF" + /*? if forge {*/ /*"FIF)V" *//*?} else if neoforge {*//*"FFI)V"*//*?} else {*/"FI)V"/*?}*//*? if forge || neoforge {*//*, remap = false*//*?}*/))
    public void tesselate(LiquidBlockRenderer instance, VertexConsumer vertexConsumer, float d, float e, float f, float g, float h, float i,/*? if neoforge {*/ /*float alpha, *//*?}*/float j, float k, int l,/*? if forge {*/ /*float alpha, *//*?}*/ BlockAndTintGetter getter, BlockPos pos, VertexConsumer arg3, BlockState state, FluidState arg5) {
        LevelReader reader = Minecraft.getInstance().level;
        vertexConsumer.addVertex(d, e, f).setColor(g, h, i, (arg5.is(FluidTags.WATER)) && reader != null ? LegacyBiomeOverride.getOrDefault(reader.getBiome(pos).unwrapKey()).waterTransparency() : /*? if forge || neoforge {*/ /*alpha*//*?} else {*/1.0f/*?}*/).setUv(j, k).setLight(l).setNormal(0.0f, 1.0f, 0.0f);
    }
    //?}
}
