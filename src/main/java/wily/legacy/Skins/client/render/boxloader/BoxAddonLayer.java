package wily.legacy.Skins.client.render.boxloader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class BoxAddonLayer extends RenderLayer {
    public BoxAddonLayer(RenderLayerParent parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, EntityRenderState state, float partialTick, float ageInTicks) {
        if (!(state instanceof AvatarRenderState ars)) return;
        if (!(ars instanceof RenderStateSkinIdAccess a)) return;

        if (consoleskins$isInvisible(ars, a)) return;

        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation texture = ClientSkinAssets.getTexture(skinId);
        if (texture == null && entry != null) texture = entry.texture();
        if (texture == null) return;

        String p = texture.getPath();
        int slash = p.lastIndexOf('/');
        if (slash != -1) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);

        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), p);

        BuiltBoxModel built = BoxModelManager.get(modelId);
        if (built == null) {
            var mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            built = BoxModelManager.get(modelId);
            if (built == null) return;
        }

        final BuiltBoxModel baked = built;
        final ResourceLocation texFinal = texture;

        collector.submitCustomGeometry(
                poseStack,
                RenderType.entityCutoutNoCull(texFinal),
                (pose, vc) -> {
                    var parentModel = this.getParentModel();
                    if (!(parentModel instanceof PlayerModel pm)) return;

                    PoseStack ps = new PoseStack();
                    ps.last().set(pose);

                    renderSlot(pm.head, baked.get(AttachSlot.HEAD), ps, vc, packedLight);
                    renderHat(pm, baked.get(AttachSlot.HAT), ps, vc, packedLight);
                    renderSlot(pm.body, baked.get(AttachSlot.BODY), ps, vc, packedLight);
                    renderSlot(pm.jacket, baked.get(AttachSlot.JACKET), ps, vc, packedLight);
                    renderSlot(pm.rightArm, baked.get(AttachSlot.RIGHT_ARM), ps, vc, packedLight);
                    renderSlot(pm.leftArm, baked.get(AttachSlot.LEFT_ARM), ps, vc, packedLight);
                    renderSlot(pm.rightSleeve, baked.get(AttachSlot.RIGHT_SLEEVE), ps, vc, packedLight);
                    renderSlot(pm.leftSleeve, baked.get(AttachSlot.LEFT_SLEEVE), ps, vc, packedLight);
                    renderSlot(pm.rightLeg, baked.get(AttachSlot.RIGHT_LEG), ps, vc, packedLight);
                    renderSlot(pm.leftLeg, baked.get(AttachSlot.LEFT_LEG), ps, vc, packedLight);
                    renderSlot(pm.rightPants, baked.get(AttachSlot.RIGHT_PANTS), ps, vc, packedLight);
                    renderSlot(pm.leftPants, baked.get(AttachSlot.LEFT_PANTS), ps, vc, packedLight);
                }
        );
    }

    private static boolean consoleskins$isInvisible(AvatarRenderState ars, RenderStateSkinIdAccess a) {
        if (ars != null) {
            try {
                Field f = ars.getClass().getField("isInvisible");
                if (f.getType() == boolean.class) return f.getBoolean(ars);
            } catch (Throwable ignored) {
            }
            try {
                Method m = ars.getClass().getMethod("isInvisible");
                if (m.getReturnType() == boolean.class) return (boolean) m.invoke(ars);
            } catch (Throwable ignored) {
            }
        }

        try {
            UUID u = a.consoleskins$getEntityUuid();
            if (u != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player p = mc.level.getPlayerByUUID(u);
                    if (p != null) return p.isInvisible();
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static void renderSlot(ModelPart limb, List<ModelPart> parts, PoseStack ps, VertexConsumer vc, int light) {
        if (parts == null || parts.isEmpty()) return;
        ps.pushPose();
        limb.translateAndRotate(ps);
        for (ModelPart p : parts) p.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static void renderHat(PlayerModel pm, List<ModelPart> parts, PoseStack ps, VertexConsumer vc, int light) {
        if (parts == null || parts.isEmpty()) return;
        ps.pushPose();
        if (isHatChildLike(pm)) pm.head.translateAndRotate(ps);
        pm.hat.translateAndRotate(ps);
        for (ModelPart p : parts) p.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static boolean isHatChildLike(PlayerModel pm) {
        ModelPart h = pm.head;
        ModelPart hat = pm.hat;
        float e = 1.0E-4F;
        if (Math.abs(hat.x - h.x) > e) return true;
        if (Math.abs(hat.y - h.y) > e) return true;
        if (Math.abs(hat.z - h.z) > e) return true;
        if (Math.abs(hat.xRot - h.xRot) > e) return true;
        if (Math.abs(hat.yRot - h.yRot) > e) return true;
        return Math.abs(hat.zRot - h.zRot) > e;
    }
}
