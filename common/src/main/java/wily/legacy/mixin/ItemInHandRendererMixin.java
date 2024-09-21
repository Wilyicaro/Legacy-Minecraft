package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void renderPlayerArm(PoseStack arg, MultiBufferSource arg2, int i, float g, float h, HumanoidArm arg3);

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow protected abstract void applyItemArmTransform(PoseStack arg, HumanoidArm arg2, float f);

    @Shadow protected abstract void applyItemArmAttackTransform(PoseStack arg, HumanoidArm arg2, float g);

    @Shadow public abstract void renderItem(LivingEntity arg, ItemStack arg2, ItemDisplayContext arg3, boolean bl, PoseStack arg4, MultiBufferSource arg5, int i);

    @Inject(method = "renderPlayerArm", at = @At(value = "HEAD"), cancellable = true)
    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f, float g, HumanoidArm humanoidArm, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.player.isRemoved()) ci.cancel();
    }

    @Inject(method = "renderMapHand", at = @At(value = "HEAD"), cancellable = true)
    private void renderMapHand(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, HumanoidArm humanoidArm, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.player.isRemoved()) ci.cancel();
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void renderItemInHand(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci) {
        int screenWidth = minecraft.getWindow().getScreenWidth();
        int screenHeight = minecraft.getWindow().getScreenHeight();
        HumanoidArm humanoidArm = interactionHand == InteractionHand.MAIN_HAND ? abstractClientPlayer.getMainArm() : abstractClientPlayer.getMainArm().getOpposite();
        float d = (screenWidth - screenHeight * 16f / 9f) / screenWidth;
        if (d != 0) poseStack.translate((humanoidArm == HumanoidArm.RIGHT ? 1 : -1) * d / 4, 0, d / 4);
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER), cancellable = true)
    private void renderArmWithItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci) {
        if (!abstractClientPlayer.getUseItem().is(Items.CROSSBOW)) return;
        HumanoidArm humanoidArm = interactionHand == InteractionHand.MAIN_HAND ? abstractClientPlayer.getMainArm() : abstractClientPlayer.getMainArm().getOpposite();
        int k = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
        if (abstractClientPlayer.isUsingItem() && abstractClientPlayer.getUseItemRemainingTicks() > 0 && abstractClientPlayer.getUsedItemHand() == interactionHand) {
            if (!abstractClientPlayer.isInvisible()) {
                poseStack.pushPose();
                float duration = (float) CrossbowItem.getChargeDuration(abstractClientPlayer.getUseItem());
                float c = Mth.clamp((float) abstractClientPlayer.getTicksUsingItem() + f, 0.0F, duration);
                poseStack.mulPose(Axis.YN.rotationDegrees(k * 20.0F));
                poseStack.translate(k * Mth.lerp(c / duration, 0.2F, 0.85F), 0, 0.54);
                this.renderPlayerArm(poseStack, multiBufferSource, j, -1.0f, 0.5f, humanoidArm.getOpposite());
                poseStack.popPose();
            }
            applyItemTransforms(poseStack,h,humanoidArm,i,k);
            float l = (float)itemStack.getUseDuration() - ((float)abstractClientPlayer.getUseItemRemainingTicks() - f + 1.0F);
            float m = l / (float)CrossbowItem.getChargeDuration(itemStack);
            if (m > 1.0F) {
                m = 1.0F;
            }

            if (m > 0.1F) {
                float n = Mth.sin((l - 0.1F) * 1.3F);
                float o = m - 0.1F;
                float p = n * o;
                poseStack.translate(p * 0.0F, p * 0.004F, p * 0.0F);
            }

            poseStack.translate(k*0.2, -0.1F, m * 0.04F - 0.2);
            poseStack.scale(1.0F, 1.0F, 1.0F + m * 0.2F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)k * 25.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees((float)k * 25.0F));
        } else {
            applyItemTransforms(poseStack,h,humanoidArm,i,k);
            if (CrossbowItem.isCharged(itemStack) && h < 0.001F && interactionHand == InteractionHand.MAIN_HAND) {
                poseStack.translate((float) k * -0.641864F, 0.0F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) k * 10.0F));
            }
        }
        this.renderItem(abstractClientPlayer, itemStack, humanoidArm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, humanoidArm == HumanoidArm.LEFT, poseStack, multiBufferSource, j);
        ci.cancel();
        poseStack.popPose();

    }
    private void applyItemTransforms(PoseStack poseStack, float h, HumanoidArm humanoidArm, float i, int k){
        float lx = -0.4F * Mth.sin(Mth.sqrt(h) * (float) Math.PI);
        float mx = 0.2F * Mth.sin(Mth.sqrt(h) * (float) (Math.PI * 2));
        float n = -0.2F * Mth.sin(h * (float) Math.PI);
        poseStack.translate((float)k * lx, mx, n);
        this.applyItemArmTransform(poseStack, humanoidArm, i);
        this.applyItemArmAttackTransform(poseStack, humanoidArm, h);
    }
}
