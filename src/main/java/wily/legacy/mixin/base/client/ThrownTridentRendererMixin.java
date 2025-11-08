package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
//?}
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySubmitNodeCollector;
import wily.legacy.client.LoyaltyLinesRenderState;

@Mixin(ThrownTridentRenderer.class)
public abstract class ThrownTridentRendererMixin extends /*? if >=1.21.2 {*/EntityRenderer<ThrownTrident, ThrownTridentRenderState>/*?} else {*//*EntityRenderer<ThrownTrident>*//*?}*/ {
    protected ThrownTridentRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    public void render(ThrownTridentRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (LegacyOptions.loyaltyLines.get())
            ((LegacySubmitNodeCollector) submitNodeCollector).submitLoyaltyLines(poseStack, LoyaltyLinesRenderState.of(renderState));
    }
}
