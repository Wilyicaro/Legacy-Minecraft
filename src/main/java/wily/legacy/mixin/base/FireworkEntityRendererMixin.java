package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import net.minecraft.client.model.ArrowModel;
import wily.legacy.client.LegacyFireworkRenderState;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;

@Mixin(FireworkEntityRenderer.class)
public abstract class FireworkEntityRendererMixin extends EntityRenderer<FireworkRocketEntity/*? if >=1.21.2 {*/, FireworkRocketRenderState/*?}*/> {
    private static final ResourceLocation FIREWORK_LOCATION = Legacy4J.createModLocation( "textures/entity/projectiles/firework.png");
    protected FireworkEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }
    //? if <1.21.2 {
    /*@Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;)Lnet/minecraft/resources/ResourceLocation;",at = @At("HEAD"), cancellable = true)
    public void getTextureLocation(FireworkRocketEntity fireworkRocketEntity, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(FIREWORK_LOCATION);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at = @At("HEAD"), cancellable = true)
    public void render(FireworkRocketEntity entity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        ci.cancel();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(g, entity.yRotO, entity.getYRot()) - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(g, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0f));
        poseStack.scale(0.05625f, 0.05625f, 0.05625f);
        poseStack.translate(-4.0f, 0.0f, 0.0f);
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutout(this.getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        this.vertex(matrix4f, pose, vertexConsumer, -7, -2, -2, 0.0f, 0.15625f, -1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, -2, 2, 0.15625f, 0.15625f, -1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, 2, 2, 0.15625f, 0.3125f, -1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, 2, -2, 0.0f, 0.3125f, -1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, 2, -2, 0.0f, 0.15625f, 1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, 2, 2, 0.15625f, 0.15625f, 1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, -2, 2, 0.15625f, 0.3125f, 1, 0, 0, i);
        this.vertex(matrix4f, pose, vertexConsumer, -7, -2, -2, 0.0f, 0.3125f, 1, 0, 0, i);
        for (int u = 0; u < 4; ++u) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            this.vertex(matrix4f, pose, vertexConsumer, -8, -2, 0, 0.0f, 0.0f, 0, 1, 0, i);
            this.vertex(matrix4f, pose, vertexConsumer, 8, -2, 0, 0.5f, 0.0f, 0, 1, 0, i);
            this.vertex(matrix4f, pose, vertexConsumer, 8, 2, 0, 0.5f, 0.15625f, 0, 1, 0, i);
            this.vertex(matrix4f, pose, vertexConsumer, -8, 2, 0, 0.0f, 0.15625f, 0, 1, 0, i);
        }
        poseStack.popPose();
        super.render(entity, f, g, poseStack, multiBufferSource, i);

    }
    @Unique
    public void vertex(Matrix4f matrix4f, PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, int k, float f, float g, int l, int m, int n, int o) {
        //? if <1.20.5 {
        /^vertexConsumer.vertex(matrix4f, i, j, k).color(255, 255, 255, 255).uv(f, g).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(o).normal(pose.normal(), l, n, m).endVertex();
        ^///?} else {
        vertexConsumer.addVertex(matrix4f, i, j, k).setColor(255, 255, 255, 255).setUv(f, g).setOverlay(OverlayTexture.NO_OVERLAY).setLight(o).setNormal(pose, l, n, m);
        //?}
    }
    *///?} else {
    @Unique
    private ArrowModel model;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, CallbackInfo ci){
        this.model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;Lnet/minecraft/client/renderer/entity/state/FireworkRocketRenderState;F)V", at = @At("RETURN"))
    private void extractRenderState(FireworkRocketEntity fireworkRocketEntity, FireworkRocketRenderState fireworkRocketRenderState, float f, CallbackInfo ci){

    }
    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/FireworkRocketRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at = @At("HEAD"), cancellable = true)
    public void render(FireworkRocketRenderState fireworkRocketRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        ci.cancel();
        LegacyFireworkRenderState renderState = FactoryRenderStateExtension.Accessor.of(fireworkRocketRenderState).getExtension(LegacyFireworkRenderState.class);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.xRot));
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutout(FIREWORK_LOCATION));
        this.model.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(fireworkRocketRenderState, poseStack, multiBufferSource, i);
    }

    //?}
}
