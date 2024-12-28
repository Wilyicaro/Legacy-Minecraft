package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
//?}
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.LoyaltyLinesRenderState;

@Mixin(ThrownTridentRenderer.class)
public abstract class ThrownTridentRendererMixin extends EntityRenderer<ThrownTrident/*? if >=1.21.2 {*/, ThrownTridentRenderState/*?}*/> {
    protected ThrownTridentRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = /*? if <1.21.2 {*//*"render(Lnet/minecraft/world/entity/projectile/ThrownTrident;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"*//*?} else {*/ "render(Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"/*?}*/,at = @At("RETURN"))
    public void render(/*? if <1.21.2 {*//*ThrownTrident thrownTrident, float f, float g*//*?} else {*/ ThrownTridentRenderState renderState/*?}*/, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (LegacyOption.loyaltyLines.get()) renderLoyaltyLines(LoyaltyLinesRenderState.of(/*? if <1.21.2 {*//*thrownTrident, g*//*?} else {*/renderState/*?}*/),poseStack,multiBufferSource, LightTexture.FULL_BLOCK);
    }
    @Unique
    private void renderLoyaltyLines(LoyaltyLinesRenderState renderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        if (renderState.canRender) {
            Matrix4f matrix4f = poseStack.last().pose();
            double d1 = Math.cos(renderState.horizontalMovementFactor);
            double d2 = Math.sin(renderState.horizontalMovementFactor);
            double d6 = d1 - d2;
            double d7 = d2 + d1;
            double d14 = Math.sqrt(renderState.x * renderState.x + renderState.y * renderState.y + renderState.z * renderState.z);
            double d15 = renderState.uniqueAge * -0.1D;
            double d16 = Math.min(0.5F, d14 / 30.0F);
            int k = 7 - ((int)renderState.uniqueAge) % 7;

            renderLoyalityLine(renderState, matrix4f,multiBufferSource.getBuffer(RenderType.leash()),k,d6,d7,d15,d16,0.02D,light);
            renderLoyalityLine(renderState, matrix4f,multiBufferSource.getBuffer(RenderType.leash()),k,d6,d7,d15,d16,0.01D,light);
        }
    }
    @Unique
    private void renderLoyalityLine(LoyaltyLinesRenderState renderState, Matrix4f matrix4f, VertexConsumer consumer, int k, double d6, double d7, double d15, double d16, double lineYD, int light){
        for(int l = 0; l <= 37; ++l) {
            float d18 = l / 37.0F;
            float f = 1.0F - (float)((l + k) % 7) / 7.0F;
            double d19 = d18 * 2.0D - 1.0D;
            d19 = (float) ((1.0F - d19 * d19) * d16);
            double d20 = renderState.x * d18 + Math.sin(d18 * Math.PI * 8.0D + d15) * d6 * d19;
            double d21 = renderState.y * d18 + Math.cos(d18 * Math.PI * 8.0D + d15) * lineYD + (0.1D + d19);
            double d22 = renderState.z * d18 + Math.sin(d18 * Math.PI * 8.0D + d15) * d7 * d19;
            float f1 = 0.20F * f + 0.34F;
            float f2 = 0.07F * f + 0.18F;
            float f3 = 0.14F * f + 0.52F;
            //? if <1.20.5 {
            /*consumer.vertex(matrix4f, (float) d20, (float) d21, (float) d22).color(f1, f2, f3, 1.0F).uv2(light).endVertex();
            consumer.vertex(matrix4f, (float) (d20 + 0.1F * d19), (float) (d21 + 0.1F * d19), (float) d22).color(f1, f2, f3, 1.0F).uv2(light).endVertex();
            *///?} else {
            consumer.addVertex(matrix4f, (float) d20, (float) d21, (float) d22).setColor(f1, f2, f3, 1.0F).setLight(light);
            consumer.addVertex(matrix4f, (float) (d20 + 0.1F * d19), (float) (d21 + 0.1F * d19), (float) d22).setColor(f1, f2, f3, 1.0F).setLight(light);
            //?}
            if (l > renderState.clientSideReturnTridentTickCount * 2) {
                break;
            }
        }
    }

}
