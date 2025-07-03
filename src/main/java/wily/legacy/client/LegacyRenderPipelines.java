
package wily.legacy.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import wily.factoryapi.mixin.base.RenderPipelinesAccessor;
import wily.legacy.Legacy4J;

public class LegacyRenderPipelines {
    public static final RenderPipeline LEGACY_SKY = RenderPipelinesAccessor.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET).withLocation(Legacy4J.createModLocation("pipeline/sky")).withVertexShader("core/position").withFragmentShader("core/position").withDepthWrite(false).withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS).build());

}
