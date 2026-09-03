package wily.legacy.client;

//? if >=26.2 {
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
//?}
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import wily.factoryapi.mixin.base.RenderPipelinesAccessor;
import wily.legacy.Legacy4J;

public class LegacyRenderPipelines {
    //? if >=26.2 {
    private static final BindGroupLayout GAMMA_BIND_GROUP = BindGroupLayout.builder().withSampler("InSampler").withUniform("GammaInfo", UniformType.UNIFORM_BUFFER).build();
    //?}
    public static final RenderPipeline LEGACY_SKY = RenderPipelinesAccessor.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET).withLocation(Legacy4J.createModLocation("pipeline/sky")).withVertexShader("core/sky").withFragmentShader("core/sky")/*? if >=26.2 {*/.withVertexBinding(0, DefaultVertexFormat.POSITION).withPrimitiveTopology(PrimitiveTopology.QUADS)/*?} else {*//*.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)*//*?}*/.build());
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
    public static final RenderPipeline LEGACY_FLAT_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/flat_clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader(Legacy4J.createModLocation("core/legacy_clouds"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .withCull(false)
                    .build()
    );
    public static final RenderPipeline LEGACY_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader(Legacy4J.createModLocation("core/legacy_clouds"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .build()
    );
    public static final RenderPipeline LEGACY_WARM_FLAT_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/warm_flat_clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader(Legacy4J.createModLocation("core/legacy_clouds_warm"))
                    //? if >=26.2 {
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    //?} else {
                    /*.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    *///?}
                    .withCull(false)
                    .build()
    );
    public static final RenderPipeline LEGACY_WARM_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/warm_clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader(Legacy4J.createModLocation("core/legacy_clouds_warm"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .build()
    );
    public static final RenderPipeline LEGACY_PACK_FLAT_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/pack_flat_clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader("core/rendertype_clouds")
                    //? if >=26.2 {
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    //?} else {
                    /*.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    *///?}
                    .withCull(false)
                    .build()
    );
    public static final RenderPipeline LEGACY_PACK_CLOUDS = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/pack_clouds"))
                    .withVertexShader(Legacy4J.createModLocation("core/legacy_rendertype_clouds"))
                    .withFragmentShader("core/rendertype_clouds")
                    //? if >=26.2 {
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    //?} else {
                    /*.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    *///?}
                    .build()
    );
    public static final RenderPipeline GAMMA = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/gamma"))
                    //? if >=26.2 {
                    .withBindGroupLayout(GAMMA_BIND_GROUP)
                    //?} else {
                    /*.withSampler("InSampler")
                     *///?}
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Legacy4J.createModLocation("core/gamma"))
                    //? if <26.2 {
                    /*.withUniform("GammaInfo", UniformType.UNIFORM_BUFFER)
                     *///?}
                    .build()
    );
}
