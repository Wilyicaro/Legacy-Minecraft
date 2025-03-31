//? if >=1.21.5 {
/*package wily.legacy.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import wily.factoryapi.mixin.base.RenderPipelinesAccessor;
import wily.legacy.Legacy4J;

public class LegacyRenderPipelines {
    public static final RenderPipeline LIGHTMAP = RenderPipelinesAccessor.register(RenderPipeline.builder().withLocation(Legacy4J.createModLocation("pipeline/lightmap")).withVertexShader("core/blit_screen").withFragmentShader("core/lightmap").withUniform("AmbientLightFactor",UniformType.FLOAT).withUniform("SkyFactor",UniformType.FLOAT).withUniform("BlockFactor",UniformType.FLOAT).withUniform("UseBrightLightmap",UniformType.INT).withUniform("BlockLightColor",UniformType.VEC3).withUniform("SkyLightColor",UniformType.VEC3).withUniform("NightVisionFactor",UniformType.FLOAT).withUniform("DarknessScale",UniformType.FLOAT).withUniform("DarkenWorldFactor",UniformType.FLOAT).withUniform("BrightnessFactor",UniformType.FLOAT).withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
    public static final RenderPipeline LEGACY_SKY = RenderPipelinesAccessor.register(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET).withLocation(Legacy4J.createModLocation("pipeline/sky")).withVertexShader("core/position").withFragmentShader("core/position").withDepthWrite(false).withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS).build());

}
*///?}
