package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer {

    public PlayerRendererMixin(EntityRendererProvider.Context context, EntityModel entityModel, float f) {
        super(context, entityModel, f);
    }

    @Shadow
    public abstract AvatarRenderState createRenderState();

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"), cancellable = true, require = 0)
    private void renderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, Identifier resourceLocation, ModelPart modelPart, boolean bl, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        AvatarRenderState state = createRenderState();
        state.swimAmount = mc.player.getSwimAmount(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        getModel().setupAnim(state);

        String skinId = ClientSkinCache.get(mc.player.getUUID());
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        if (SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.HIDE_HAND, skinId)) {
            ci.cancel();
            return;
        }

        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId);
        Identifier texture = resolved == null ? null : resolved.texture();
        if (texture == null) return;

        BuiltBoxModel built = resolved == null ? null : resolved.boxModel();
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

        Identifier boxTexture = resolved == null || resolved.boxTexture() == null ? texture : resolved.boxTexture();
        final Identifier texFinal = boxTexture;
        final var partsFinal = parts;
        final float partScale = built.partScale();
        final ModelPart modelPartSnapshot = snapshotPart(modelPart);
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutNoCull(texFinal),
                (pose, vc) -> {
                    PoseStack ps = new PoseStack();
                    ps.last().set(pose);
                    ps.pushPose();
                    modelPartSnapshot.translateAndRotate(ps);
                    if (partScale != 1.0F) ps.scale(partScale, partScale, partScale);
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

    private static ModelPart snapshotPart(ModelPart part) {
        ModelPart snapshot = new ModelPart(List.of(), Map.of());
        if (part == null) return snapshot;
        snapshot.visible = part.visible;
        snapshot.x = part.x;
        snapshot.y = part.y;
        snapshot.z = part.z;
        snapshot.xRot = part.xRot;
        snapshot.yRot = part.yRot;
        snapshot.zRot = part.zRot;
        snapshot.xScale = part.xScale;
        snapshot.yScale = part.yScale;
        snapshot.zScale = part.zScale;
        return snapshot;
    }
}
