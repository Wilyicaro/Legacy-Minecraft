package wily.legacy.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import wily.factoryapi.mixin.base.RenderPipelinesAccessor;
import wily.legacy.Legacy4J;

import java.util.Optional;

public class LegacyRenderPipelines {
    public static final RenderPipeline LEGACY_SKY = RenderPipelinesAccessor.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET).withLocation(Legacy4J.createModLocation("pipeline/sky")).withVertexShader("core/sky").withFragmentShader("core/sky").withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS).build());
    public static final RenderPipeline LEGACY_HURT_FLASH = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/hurt_flash"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
                    .withCull(false)
                    .build()
    );
    public static final CloudPipeline LEGACY_CLOUDS = cloudPipeline("clouds", "legacy:core/legacy_clouds", true);
    public static final CloudPipeline LEGACY_CLOUDS_INSIDE = cloudPipeline("clouds_inside", "legacy:core/legacy_clouds", false);
    public static final CloudPipeline LEGACY_WARM_CLOUDS = cloudPipeline("warm_clouds", "legacy:core/legacy_clouds_warm", true);
    public static final CloudPipeline LEGACY_WARM_CLOUDS_INSIDE = cloudPipeline("warm_clouds_inside", "legacy:core/legacy_clouds_warm", false);
    public static final CloudPipeline LEGACY_PACK_CLOUDS = cloudPipeline("pack_clouds", "core/rendertype_clouds", true);
    public static final CloudPipeline LEGACY_PACK_CLOUDS_INSIDE = cloudPipeline("pack_clouds_inside", "core/rendertype_clouds", false);
    public static final RenderPipeline GAMMA = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/gamma"))
                    .withSampler("InSampler")
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Legacy4J.createModLocation("core/gamma"))
                    .withUniform("GammaInfo", UniformType.UNIFORM_BUFFER)
                    .build()
    );

    private static CloudPipeline cloudPipeline(String name, String fragmentShader, boolean cull) {
        RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                .withFragmentShader(Identifier.parse(fragmentShader))
                .withCull(cull)
                .buildSnippet();
        RenderPipeline depth = RenderPipelinesAccessor.register(RenderPipeline.builder(snippet)
                .withLocation(Legacy4J.createModLocation("pipeline/" + name + "_depth"))
                .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                .build());
        RenderPipeline color = RenderPipelinesAccessor.register(RenderPipeline.builder(snippet)
                .withLocation(Legacy4J.createModLocation("pipeline/" + name))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build());
        return new CloudPipeline(depth, color);
    }

    public record CloudPipeline(RenderPipeline depth, RenderPipeline color) {
    }
}
