package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer {
    public PlayerRendererMixin(EntityRendererProvider.Context context, EntityModel entityModel, float f) {
        super(context, entityModel, f);
    }

    @Redirect(method = "renderHand", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/PlayerModel;swimAmount:F", opcode = Opcodes.PUTFIELD))
    private void renderHand(PlayerModel instance, float value, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer) {
        instance.swimAmount = abstractClientPlayer.getSwimAmount(Minecraft.getInstance().isPaused() ? Minecraft.getInstance().pausePartialTick : Minecraft.getInstance().getFrameTime());
        ((PlayerModel)getModel()).rightArmPose = HumanoidModel.ArmPose.EMPTY;
        ((PlayerModel)getModel()).leftArmPose = HumanoidModel.ArmPose.EMPTY;
    }
}
