package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.compat.bedrockskins.BedrockSkinsCompat;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer {

    public PlayerRendererMixin(EntityRendererProvider.Context context, EntityModel entityModel, float f) {
        super(context, entityModel, f);
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"), cancellable = true, require = 0)
    private void renderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, ResourceLocation resourceLocation, ModelPart modelPart, boolean bl, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        String skinId = ClientSkinCache.get(mc.player.getUUID());
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;
        if (BedrockSkinsCompat.isBedrockSkinId(skinId)) return;

        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.HIDE_HAND, skinId)) {
            ci.cancel();
            return;
        }

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation texture = ClientSkinAssets.getTexture(skinId);
        if (texture == null && entry != null) texture = entry.texture();
        if (texture == null) return;

        String path = texture.getPath();
        int slash = path.lastIndexOf('/');
        if (slash != -1) path = path.substring(slash + 1);
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);

        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);

        BuiltBoxModel built = BoxModelManager.get(modelId);
        if (built == null) {
            var mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            built = BoxModelManager.get(modelId);
        }
        if (built == null) return;

        EntityModel m = getModel();
        if (!(m instanceof PlayerModel pm)) return;

        AttachSlot slot = null;
        if (modelPart == pm.rightArm) slot = AttachSlot.RIGHT_ARM;
        else if (modelPart == pm.leftArm) slot = AttachSlot.LEFT_ARM;
        if (slot == null) return;

        if (!built.hides(slot)) return;

        var parts = built.get(slot);
        if (parts == null || parts.isEmpty()) return;

        final ResourceLocation texFinal = texture;
        final var partsFinal = parts;

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderType.entityCutoutNoCull(texFinal),
                (pose, vc) -> {
                    PoseStack ps = new PoseStack();
                    ps.last().set(pose);
                    ps.pushPose();
                    modelPart.translateAndRotate(ps);
                    for (ModelPart p : partsFinal) p.render(ps, vc, packedLight, OverlayTexture.NO_OVERLAY);
                    ps.popPose();
                }
        );

        ci.cancel();
    }

    @Redirect(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isFallFlying:Z"))
    private boolean render(AvatarRenderState instance) {
        return instance.isFallFlying && instance.hasPose(Pose.FALL_FLYING);
    }
}
