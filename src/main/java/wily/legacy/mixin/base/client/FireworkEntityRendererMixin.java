package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import net.minecraft.client.model.ArrowModel;
import wily.legacy.client.LegacyFireworkRenderState;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyOptions;

@Mixin(FireworkEntityRenderer.class)
public abstract class FireworkEntityRendererMixin extends EntityRenderer<FireworkRocketEntity, FireworkRocketRenderState> {
    private static final ResourceLocation FIREWORK_LOCATION = Legacy4J.createModLocation( "textures/entity/projectiles/firework.png");
    protected FireworkEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }
    @Unique
    private ArrowModel model;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, CallbackInfo ci){
        this.model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FireworkRocketRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",at = @At("HEAD"), cancellable = true)
    public void submit(FireworkRocketRenderState fireworkRocketRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (LegacyOptions.legacyFireworks.get()) {
            ci.cancel();
            LegacyFireworkRenderState renderState = FactoryRenderStateExtension.Accessor.of(fireworkRocketRenderState).getExtension(LegacyFireworkRenderState.class);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.xRot));
            submitNodeCollector.submitModel(model, new ArrowRenderState(), poseStack, RenderType.entityCutout(FIREWORK_LOCATION), fireworkRocketRenderState.lightCoords, OverlayTexture.NO_OVERLAY, fireworkRocketRenderState.outlineColor, null);
            poseStack.popPose();
            super.submit(fireworkRocketRenderState, poseStack, submitNodeCollector, cameraRenderState);
        }
    }
}
