package wily.legacy.skins.client.render.boxloader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import wily.legacy.compat.cpm.CpmRenderCompat;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoxAddonLayer extends RenderLayer {
    public BoxAddonLayer(RenderLayerParent parent) {
        super(parent);
    }

    private static boolean consoleskins$isInvisible(AvatarRenderState ars, RenderStateSkinIdAccess a) {
        if (ars.isInvisible) return true;
        UUID u = a.consoleskins$getEntityUuid();
        if (u == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        if (mc.level == null) return false;
        Player p = mc.level.getPlayerByUUID(u);
        return p != null && p.isInvisible();
    }

    private static void renderSlot(ModelPart limb, List<ModelPart> parts, PoseStack ps, VertexConsumer vc, int light, float partScale) {
        if (parts == null || parts.isEmpty()) return;
        ps.pushPose();
        limb.translateAndRotate(ps);
        if (partScale != 1.0F) ps.scale(partScale, partScale, partScale);
        for (ModelPart p : parts) p.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static void renderHat(ModelPart head, ModelPart hat, boolean hatChildLike, List<ModelPart> parts, PoseStack ps, VertexConsumer vc, int light, float partScale) {
        if (parts == null || parts.isEmpty()) return;
        ps.pushPose();
        if (hatChildLike) head.translateAndRotate(ps);
        hat.translateAndRotate(ps);
        if (partScale != 1.0F) ps.scale(partScale, partScale, partScale);
        for (ModelPart p : parts) p.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static boolean isHatChildLike(ModelPart h, ModelPart hat) {
        float e = 1.0E-4F;
        if (Math.abs(hat.x - h.x) > e) return true;
        if (Math.abs(hat.y - h.y) > e) return true;
        if (Math.abs(hat.z - h.z) > e) return true;
        if (Math.abs(hat.xRot - h.xRot) > e) return true;
        if (Math.abs(hat.yRot - h.yRot) > e) return true;
        return Math.abs(hat.zRot - h.zRot) > e;
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

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, EntityRenderState state, float partialTick, float ageInTicks) {
        if (!(state instanceof AvatarRenderState ars)) return;
        if (!(ars instanceof RenderStateSkinIdAccess a)) return;
        if (CpmRenderCompat.isCpmModelActive(ars)) return;
        if (consoleskins$isInvisible(ars, a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || SkinIdUtil.AUTO_SELECT.equals(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(a);
        Identifier texture = resolved == null ? null : resolved.texture();
        if (texture == null) return;
        BuiltBoxModel built = resolved == null ? null : resolved.boxModel();
        if (built == null) return;
        Identifier boxTexture = resolved == null || resolved.boxTexture() == null ? texture : resolved.boxTexture();
        var parentModel = this.getParentModel();
        if (!(parentModel instanceof PlayerModel pm)) return;

        final BuiltBoxModel baked = built;
        final Identifier texFinal = boxTexture;
        final ModelPart head = snapshotPart(pm.head);
        final ModelPart hat = snapshotPart(pm.hat);
        final ModelPart body = snapshotPart(pm.body);
        final ModelPart jacket = snapshotPart(pm.jacket);
        final ModelPart rightArm = snapshotPart(pm.rightArm);
        final ModelPart leftArm = snapshotPart(pm.leftArm);
        final ModelPart rightSleeve = snapshotPart(pm.rightSleeve);
        final ModelPart leftSleeve = snapshotPart(pm.leftSleeve);
        final ModelPart rightLeg = snapshotPart(pm.rightLeg);
        final ModelPart leftLeg = snapshotPart(pm.leftLeg);
        final ModelPart rightPants = snapshotPart(pm.rightPants);
        final ModelPart leftPants = snapshotPart(pm.leftPants);
        final boolean hatChildLike = isHatChildLike(head, hat);
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutNoCull(texFinal),
                (pose, vc) -> {
                    PoseStack ps = new PoseStack();
                    ps.last().set(pose);

                    float partScale = baked.partScale();
                    renderSlot(head, baked.get(AttachSlot.HEAD), ps, vc, packedLight, partScale);
                    renderHat(head, hat, hatChildLike, baked.get(AttachSlot.HAT), ps, vc, packedLight, partScale);
                    renderSlot(body, baked.get(AttachSlot.BODY), ps, vc, packedLight, partScale);
                    renderSlot(jacket, baked.get(AttachSlot.JACKET), ps, vc, packedLight, partScale);
                    renderSlot(rightArm, baked.get(AttachSlot.RIGHT_ARM), ps, vc, packedLight, partScale);
                    renderSlot(leftArm, baked.get(AttachSlot.LEFT_ARM), ps, vc, packedLight, partScale);
                    renderSlot(rightSleeve, baked.get(AttachSlot.RIGHT_SLEEVE), ps, vc, packedLight, partScale);
                    renderSlot(leftSleeve, baked.get(AttachSlot.LEFT_SLEEVE), ps, vc, packedLight, partScale);
                    renderSlot(rightLeg, baked.get(AttachSlot.RIGHT_LEG), ps, vc, packedLight, partScale);
                    renderSlot(leftLeg, baked.get(AttachSlot.LEFT_LEG), ps, vc, packedLight, partScale);
                    renderSlot(rightPants, baked.get(AttachSlot.RIGHT_PANTS), ps, vc, packedLight, partScale);
                    renderSlot(leftPants, baked.get(AttachSlot.LEFT_PANTS), ps, vc, packedLight, partScale);
                }
        );
    }
}
