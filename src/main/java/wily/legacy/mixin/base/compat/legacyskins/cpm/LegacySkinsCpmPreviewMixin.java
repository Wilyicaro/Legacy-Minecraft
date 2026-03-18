package wily.legacy.mixin.base.compat.legacyskins.cpm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.compat.legacyskins.LegacySkinsCompat;
import wily.legacy.Skins.client.gui.DollRenderIds;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;

@Pseudo
@Mixin(targets = "io.github.redrain0o0.legacyskins.client.screen.CPMPlayerSkinQ", remap = false)
public abstract class LegacySkinsCpmPreviewMixin {
    @Inject(method = "render0", at = @At("HEAD"), require = 0, remap = false)
    private void legacy$beginEmbeddedPreviewRender(@Coerce Object model,
                                                   @Coerce Object widget,
                                                   PoseStack stack,
                                                   @Coerce Object playerSkin,
                                                   MultiBufferSource source,
                                                   float tickDelta,
                                                   CallbackInfo ci) {
        LegacySkinsCompat.beginEmbeddedCpmPreviewRender(model);
    }

    @Inject(method = "render0", at = @At("TAIL"), require = 0, remap = false)
    private void legacy$endEmbeddedPreviewRender(@Coerce Object model,
                                                 @Coerce Object widget,
                                                 PoseStack stack,
                                                 @Coerce Object playerSkin,
                                                 MultiBufferSource source,
                                                 float tickDelta,
                                                 CallbackInfo ci) {
        LegacySkinsCompat.endEmbeddedCpmPreviewRender(model);
    }

    @WrapOperation(
            method = "render0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tom/cpm/client/CustomPlayerModelsClient;playerRenderPre(Lcom/tom/cpm/client/PlayerRenderStateAccess;Lnet/minecraft/class_591;Lnet/minecraft/class_10055;)V",
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private void legacy$applyEmbeddedPreviewState(@Coerce Object instance,
                                                  @Coerce Object playerRenderStateAccess,
                                                  PlayerModel playerModel,
                                                  AvatarRenderState renderState,
                                                  Operation<Void> original,
                                                  @Coerce Object model,
                                                  @Coerce Object safeWidget,
                                                  PoseStack stack,
                                                  @Coerce Object playerSkin,
                                                  MultiBufferSource source,
                                                  float tickDelta) {
        LegacySkinsCompat.prepareEmbeddedCpmPreviewPose(model, safeWidget, playerModel);
        legacy$configurePreviewRenderState(renderState, safeWidget);
        original.call(instance, playerRenderStateAccess, playerModel, renderState);
    }

    @WrapOperation(
            method = "render0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;eyes(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private RenderType legacy$forceOpaqueCarouselEyes(ResourceLocation texture,
                                                      Operation<RenderType> original) {
        if (LegacySkinsCompat.isRenderingEmbeddedCpmPreview()) {
            return LegacySkinsCompat.opaqueEmbeddedCpmRenderType(texture);
        }
        return original.call(texture);
    }

    @WrapOperation(
            method = "render0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private RenderType legacy$forceOpaqueCarouselCape(ResourceLocation texture,
                                                      Operation<RenderType> original) {
        if (LegacySkinsCompat.isRenderingEmbeddedCpmPreview()) {
            return LegacySkinsCompat.opaqueEmbeddedCpmRenderType(texture);
        }
        return original.call(texture);
    }

    private static void legacy$configurePreviewRenderState(AvatarRenderState renderState, Object safeWidget) {
        if (!LegacySkinsCompat.isRenderingEmbeddedCpmPreview() || renderState == null) return;

        boolean crouching = LegacySkinsCompat.isEmbeddedCpmPreviewSneaking();
        boolean punching = LegacySkinsCompat.isEmbeddedCpmPreviewPunching();
        String skinId = LegacySkinsCompat.getEmbeddedCpmPreviewSkinId();

        renderState.id = DollRenderIds.MENU_DOLL_ID;
        renderState.pose = crouching ? Pose.CROUCHING : Pose.STANDING;
        renderState.isCrouching = crouching;
        renderState.attackArm = HumanoidArm.RIGHT;
        renderState.attackTime = punching ? legacy$previewAttackTime() : 0.0F;
        if (renderState instanceof RenderStateSkinIdAccess access) {
            access.consoleskins$setSkinId(skinId);
            access.consoleskins$setMoving(true);
            access.consoleskins$setMoveSpeedSq(1.0F);
            access.consoleskins$setSitting(false);
            access.consoleskins$setUsingItem(false);
            access.consoleskins$setBlocking(false);
        }
    }

    private static float legacy$previewAttackTime() {
        long swingMs = 300L;
        long phase = System.currentTimeMillis() % (swingMs + 5L);
        return phase < swingMs ? phase / (float) swingMs : 0.0F;
    }
}
