package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;

@Mixin(DrownedRenderer.class)
public class DrownedRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/world/entity/monster/Drowned;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/AbstractZombieRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", shift = At.Shift.AFTER), cancellable = true)
    protected void setupRotations(Drowned drowned, PoseStack poseStack, float f, float g, float h, float i, CallbackInfo ci) {
        if (!LegacyOption.legacyDrownedAnimation.get()) return;
        ci.cancel();
        float j = drowned.getSwimAmount(h);
        if (j <= 0) return;
        float k = drowned.getViewXRot(h);
        float l = drowned.isInWater() ? -90.0F - k : -90.0F;
        float m = Mth.lerp(j, 0.0F, l);
        poseStack.mulPose(Axis.XP.rotationDegrees(m));
        if (drowned.isVisuallySwimming()) {
            poseStack.translate(0.0F, -1.0F, 0.3F);
        }
    }
}
