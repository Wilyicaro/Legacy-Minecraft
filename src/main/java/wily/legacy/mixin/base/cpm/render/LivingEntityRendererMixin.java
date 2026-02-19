package wily.legacy.mixin.base.cpm.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.CustomModelSkins.cpm.client.CPMOrderedSubmitNodeCollector.CPMSubmitNodeCollector;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends net.minecraft.world.entity.LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow
    public abstract M getModel();

    @Inject(at = @At("HEAD"), method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V")
    public void onRenderPre(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo cbi, @Local LocalRef<SubmitNodeCollector> snc) {
        if (state instanceof AvatarRenderState && getModel() instanceof PlayerModel) {
            if (CustomPlayerModelsClient.INSTANCE == null || CustomPlayerModelsClient.mc == null) return;
            CPMSubmitNodeCollector.injectSNC(snc);
            CustomPlayerModelsClient.INSTANCE.playerRenderPre((PlayerRenderStateAccess) state, (PlayerModel) getModel(), (AvatarRenderState) state);
        }
    }

    @Inject(at = @At("RETURN"), method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V")
    public void onRenderPost(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo cbi) {
        if (state instanceof AvatarRenderState && getModel() instanceof PlayerModel) {
            if (CustomPlayerModelsClient.INSTANCE == null || CustomPlayerModelsClient.mc == null) return;
            CustomPlayerModelsClient.INSTANCE.playerRenderPost((PlayerModel) getModel());
        }
    }
}
