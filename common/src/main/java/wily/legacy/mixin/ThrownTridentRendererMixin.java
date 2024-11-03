package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;

@Mixin(ThrownTridentRenderer.class)
public abstract class ThrownTridentRendererMixin extends EntityRenderer<ThrownTrident> {
    protected ThrownTridentRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/ThrownTrident;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at = @At("RETURN"))
    public void render(ThrownTrident thrownTrident, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (LegacyOption.loyaltyLines.get()) renderLoyaltyLines(thrownTrident,g,poseStack,multiBufferSource, LightTexture.FULL_BLOCK);
    }
    @Unique
    private void renderLoyaltyLines(ThrownTrident trident, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        Entity owner = trident.getOwner();
        if (owner != null && trident.isNoPhysics()) {
            Matrix4f matrix4f = poseStack.last().pose();
            double d0 = Mth.lerp(partialTicks * 0.5F, owner.yRotO, owner.getYRot()) * (Math.PI / 180);
            double d1 = Math.cos(d0);
            double d2 = Math.sin(d0);
            double d3 = Mth.lerp(partialTicks,owner.xOld, owner.getX());
            double d4 = Mth.lerp(partialTicks,owner.yOld + owner.getEyeHeight() * 0.8D, owner.getY() + owner.getEyeHeight() * 0.8D);
            double d5 = Mth.lerp(partialTicks,owner.zOld, owner.getZ());
            double d6 = d1 - d2;
            double d7 = d2 + d1;
            double d8 = Mth.lerp(partialTicks,trident.xOld, trident.getX());
            double d9 = Mth.lerp(partialTicks,trident.yOld, trident.getY());
            double d10 = Mth.lerp(partialTicks,trident.zOld, trident.getZ());
            double d11 = d3 - d8;
            double d12 = d4 - d9;
            double d13 = d5 - d10;
            double d14 = Math.sqrt(d11 * d11 + d12 * d12 + d13 * d13);
            int i = trident.getId() + trident.tickCount;
            double d15 = (i + partialTicks) * -0.1D;
            double d16 = Math.min(0.5F, d14 / 30.0F);
            int k = 7 - i % 7;

            renderLoyalityLine(trident,matrix4f,multiBufferSource.getBuffer(RenderType.leash()),k,d6,d7,d11,d12,d13,d15,d16,0.02D,light);
            renderLoyalityLine(trident,matrix4f,multiBufferSource.getBuffer(RenderType.leash()),k,d6,d7,d11,d12,d13,d15,d16,0.01D,light);
        }
    }
    @Unique
    private void renderLoyalityLine(ThrownTrident trident, Matrix4f matrix4f, VertexConsumer consumer, int k, double d6, double d7, double d11, double d12, double d13, double d15, double d16, double lineYD, int light){
        for(int l = 0; l <= 37; ++l) {
            float d18 = l / 37.0F;
            float f = 1.0F - (float)((l + k) % 7) / 7.0F;
            double d19 = d18 * 2.0D - 1.0D;
            d19 = (float) ((1.0F - d19 * d19) * d16);
            double d20 = d11 * d18 + Math.sin(d18 * Math.PI * 8.0D + d15) * d6 * d19;
            double d21 = d12 * d18 + Math.cos(d18 * Math.PI * 8.0D + d15) * lineYD + (0.1D + d19);
            double d22 = d13 * d18 + Math.sin(d18 * Math.PI * 8.0D + d15) * d7 * d19;
            float f1 = 0.20F * f + 0.34F;
            float f2 = 0.07F * f + 0.18F;
            float f3 = 0.14F * f + 0.52F;
            consumer.addVertex(matrix4f, (float) d20, (float) d21, (float) d22).setColor(f1, f2, f3, 1.0F).setLight(light);
            consumer.addVertex(matrix4f, (float) (d20 + 0.1F * d19), (float) (d21 + 0.1F * d19), (float) d22).setColor(f1, f2, f3, 1.0F).setLight(light);
            if (l > trident.clientSideReturnTridentTickCount * 2) {
                break;
            }
        }
    }
}
