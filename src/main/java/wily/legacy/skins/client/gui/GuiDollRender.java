package wily.legacy.skins.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.2 {
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
*///?}
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import wily.legacy.client.LegacyOptions;
import wily.legacy.mixin.base.skins.client.ModelPartAccessor;
import wily.legacy.skins.client.render.PlayerModelParts;
//? if >=1.21.2 {
/*import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
*///?}
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.pose.MenuDollPose;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class GuiDollRender {
    public static final int MENU_DOLL_ID = -0x5D011;
    private static final float BASE_BBOX_HEIGHT = 1.8F;
    private static final float BASE_BBOX_WIDTH = 0.6F;
    private static final float BASE_TRANSLATE_Y = BASE_BBOX_HEIGHT / 2.0F;
    private static final float SCALE_DIVISOR = 2.75F;
    private static final float CROUCH_Y_OFFSET = -0.125F;
    private static final int MIN_RENDER_SIZE = 20;
    private static final int CAPE_TEXTURE_WIDTH = 64;
    private static final int CAPE_TEXTURE_HEIGHT = 32;
    private static PlayerModel wideModel;
    private static PlayerModel slimModel;
    private static ModelPart capeModel;
    private static final Map<PlayerModel, float[][]> previousVisualOffsets = new IdentityHashMap<>();

    private GuiDollRender() {
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, ResourceLocation skinTexture, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        if (gui == null || skinTexture == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getTextureManager().getTexture(skinTexture);

        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(selectionId, skinTexture, null, null);
        var entry = resolved == null ? null : resolved.entry();
        ResourceLocation resolvedTexture = resolved != null && resolved.texture() != null ? resolved.texture() : skinTexture;
        ResourceLocation boxTexture = resolved != null && resolved.boxTexture() != null ? resolved.boxTexture() : resolvedTexture;
        BuiltBoxModel boxModel = resolved == null ? null : resolved.boxModel();
        ResourceLocation capeTexture = ClientSkinAssets.hasCape(resolved) && entry != null ? entry.cape() : null;
        if (mc != null && boxTexture != null) mc.getTextureManager().getTexture(boxTexture);
        if (mc != null && capeTexture != null) mc.getTextureManager().getTexture(capeTexture);
        renderResolvedDollInRect(gui, selectionId, resolvedTexture, boxTexture, capeTexture, resolved == null ? null : resolved.modelId(), boxModel, ClientSkinAssets.isSlimModel(selectionId, resolved), yawOffset, crouching, attackTime, partialTick, left, top, right, bottom, sizeCap);
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, ResourceLocation skinTexture, boolean slim, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        if (gui == null || skinTexture == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getTextureManager().getTexture(skinTexture);
        renderResolvedDollInRect(gui, selectionId, skinTexture, skinTexture, null, null, null, slim, yawOffset, crouching, attackTime, partialTick, left, top, right, bottom, sizeCap);
    }

    private static void renderResolvedDollInRect(GuiGraphics gui, String selectionId, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation capeTexture, ResourceLocation modelId, BuiltBoxModel boxModel, boolean slim, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        PlayerModel model = getModel(slim);
        if (model == null) return;

        int baseHeight = Math.max(1, bottom - top);
        int size = Math.min((int) (baseHeight / SCALE_DIVISOR), sizeCap);
        size = Math.max(size, MIN_RENDER_SIZE);
        float centerX = (left + right) / 2.0F;
        float centerY = (top + bottom) / 2.0F;

        renderModelInRect(gui, model, selectionId, texture, boxTexture, capeTexture, modelId, boxModel, centerX, centerY, size, yawOffset, crouching, attackTime, partialTick, left, top - baseHeight, right, bottom + baseHeight);
    }

    private static PlayerModel getModel(boolean slim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        if (slim) {
            if (slimModel == null) slimModel = createModel(ModelLayers.PLAYER_SLIM, true);
            return slimModel;
        }
        if (wideModel == null) wideModel = createModel(ModelLayers.PLAYER, false);
        return wideModel;
    }

    private static PlayerModel createModel(ModelLayerLocation layer, boolean slim) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : new PlayerModel(mc.getEntityModels().bakeLayer(layer), slim);
    }

    private static void renderModelInRect(GuiGraphics gui, PlayerModel model, String selectionId, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation capeTexture, ResourceLocation modelId, BuiltBoxModel boxModel, float centerX, float centerY, int size, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom) {
        prepareModel(model, selectionId, texture, boxTexture, modelId, boxModel, yawOffset, crouching, attackTime, partialTick);
        //? if <1.21.2
        applyBoxVisibility(model, boxModel);

        FactoryGuiGraphics factoryGraphics = FactoryGuiGraphics.of(gui);
        gui.enableScissor(left, top, right, bottom);
        var pose = gui.pose();
        pose.pushPose();
        try {
            pose.translate(centerX, centerY, 50.0F);
            pose./*? if <1.20.5 {*//*mulPoseMatrix*//*?} else {*/mulPose/*?}*/(new Matrix4f().scaling(size, size, -size));
            pose.translate(0.0F, BASE_TRANSLATE_Y + (crouching ? CROUCH_Y_OFFSET : 0.0F), 0.0F);
            pose.mulPose(new Quaternionf().rotationZ((float) Math.PI));
            pose.mulPose(new Quaternionf().rotationY(normalizeYaw(-yawOffset) * ((float) Math.PI / 180.0F)));
            applyUpsideDownTransform(pose, selectionId);
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, -1.501F, 0.0F);
            Lighting.setupForEntityInInventory();
            VertexConsumer consumer = ScreenUtil.guiBufferSource(gui).getBuffer(model.renderType(texture));
            renderModel(model, pose, consumer);
            if (capeTexture != null) {
                VertexConsumer capeConsumer = ScreenUtil.guiBufferSource(gui).getBuffer(RenderType.entityCutoutNoCull(capeTexture));
                renderCape(model, pose, capeConsumer);
            }
            if (boxModel != null && boxTexture != null) {
                VertexConsumer boxConsumer = ScreenUtil.guiBufferSource(gui).getBuffer(RenderType.entityCutoutNoCull(boxTexture));
                renderBoxModel(model, boxModel, pose, boxConsumer);
            }
            gui.flush();
        } finally {
            pose.popPose();
            gui.disableScissor();
            Lighting.setupFor3DItems();
        }
    }

    private static void applyUpsideDownTransform(PoseStack pose, String selectionId) {
        if (!LegacyOptions.customSkinAnimation.get()) return;
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, selectionId)) return;
        pose.translate(0.0F, 2.0F, 0.0F);
        pose.mulPose(new Quaternionf().rotationZ((float) Math.PI));
    }

    private static void prepareModel(PlayerModel model, String selectionId, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation modelId, BuiltBoxModel boxModel, float yawOffset, boolean crouching, float attackTime, float partialTick) {
        //? if >=1.21.2 {
        /*prepareModelFromRenderState(model, selectionId, texture, boxTexture, modelId, boxModel, yawOffset, crouching, attackTime, partialTick);
        *///?} else {
        undoVisualOffsets(model);
        resetModel(model);
        model.setAllVisible(true);
        /*? if <1.21.2 {*/
        model.young = false;
        model.riding = false;
        model.attackTime = Math.min(1.0F, Math.max(0.0F, attackTime));
        /*?}*/

        float yaw = normalizeYaw(yawOffset);
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        model.head.yRot = yawRad * 0.18F;
        model.hat.yRot = model.head.yRot;

        if (crouching) applyCrouch(model);
        if (attackTime > 0.0F) applyAttack(model, attackTime);
        MenuDollPose.applySkinPoses(model, MenuDollPose.state(selectionId, crouching, attackTime));
        syncOverlayParts(model);
        applyVisualOffsets(model, modelId, boxModel);
        //?}
    }

    //? if >=1.21.2 {
    /*private static void prepareModelFromRenderState(PlayerModel model, String selectionId, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation modelId, BuiltBoxModel boxModel, float yawOffset, boolean crouching, float attackTime, float partialTick) {
        PlayerRenderState state = new PlayerRenderState();
        if (state instanceof RenderStateSkinIdAccess access) {
            access.consoleskins$setSkinId(selectionId);
            access.consoleskins$setCachedTexture(texture);
            access.consoleskins$setCachedBoxTexture(boxTexture);
            access.consoleskins$setCachedModelId(modelId);
            access.consoleskins$setCachedBoxModel(boxModel);
        }
        float yaw = normalizeYaw(yawOffset + 180.0F);
        state.id = MENU_DOLL_ID;
        setOptionalStateField(state, "entityType", EntityType.PLAYER);
        state.boundingBoxHeight = BASE_BBOX_HEIGHT;
        state.boundingBoxWidth = BASE_BBOX_WIDTH;
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.ageInTicks = (System.currentTimeMillis() % 1_000_000L) / 50.0F + partialTick;
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.speedValue = 1.0F;
        state.mainArm = HumanoidArm.RIGHT;
        state.attackArm = HumanoidArm.RIGHT;
        setOptionalStateField(state, "rightArmPose", HumanoidModel.ArmPose.EMPTY);
        setOptionalStateField(state, "leftArmPose", HumanoidModel.ArmPose.EMPTY);
        state.isCrouching = crouching;
        state.attackTime = Math.min(1.0F, Math.max(0.0F, attackTime));
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        state.isSpectator = false;
        model.setupAnim(state);
    }
    *///?}

    private static void setOptionalStateField(Object state, String fieldName, Object value) {
        if (state == null || fieldName == null) return;
        try {
            state.getClass().getField(fieldName).set(state, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void resetModel(PlayerModel model) {
        resetBoxVisibility(model);
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
        model.leftSleeve.resetPose();
        model.rightSleeve.resetPose();
        model.leftPants.resetPose();
        model.rightPants.resetPose();
        model.jacket.resetPose();
    }

    private static void applyCrouch(PlayerModel model) {
        model.body.xRot = 0.5F;
        model.body.y = 3.2F;
        model.head.xRot += 0.08F;
        model.head.yRot += 0.10F;
        model.head.y = 4.2F;
        model.hat.y = model.head.y;
        model.rightArm.xRot += 0.4F;
        model.leftArm.xRot += 0.4F;
        model.rightArm.y = 5.2F;
        model.leftArm.y = 5.2F;
        model.rightLeg.y = 12.2F;
        model.leftLeg.y = 12.2F;
        model.rightLeg.z = 4.0F;
        model.leftLeg.z = 4.0F;
    }

    private static void applyAttack(PlayerModel model, float attackTime) {
        float swing = Math.min(1.0F, Math.max(0.0F, attackTime));
        model.body.yRot = Mth.sin(Mth.sqrt(swing) * Mth.PI * 2.0F) * 0.2F;
        model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
        model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
        model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
        model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
        model.rightArm.yRot += model.body.yRot;
        model.leftArm.yRot += model.body.yRot;
        model.leftArm.xRot += model.body.yRot;

        float eased = 1.0F - swing;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float armSwing = Mth.sin(eased * Mth.PI);
        float headCompensation = Mth.sin(swing * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        model.rightArm.xRot -= armSwing * 1.2F + headCompensation;
        model.rightArm.yRot += model.body.yRot * 2.0F;
        model.rightArm.zRot += Mth.sin(swing * Mth.PI) * -0.4F;
    }

    private static void syncOverlayParts(PlayerModel model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    private static void applyVisualOffsets(PlayerModel model, ResourceLocation modelId, BuiltBoxModel boxModel) {
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getOffsets(modelId);
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getScales(modelId);
        if ((offsets == null || offsets.isEmpty()) && (scales == null || scales.isEmpty())) return;
        float[][] previousOffsets = previousVisualOffsets.computeIfAbsent(model, ignored -> new float[PlayerModelParts.ALL.length][]);
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            ModelPart part = PlayerModelParts.get(model, slot);
            if (part == null) continue;
            float[] offset = visualOffsetForSlot(offsets, boxModel, slot);
            if (offset != null && offset.length >= 3) {
                part.x += offset[0];
                part.y += offset[1];
                part.z += offset[2];
                previousOffsets[i] = Arrays.copyOf(offset, offset.length);
            } else {
                previousOffsets[i] = null;
            }
            float[] scale = scales == null ? null : scales.get(slot);
            if (scale != null && scale.length >= 3) {
                part.xScale = scale[0];
                part.yScale = scale[1];
                part.zScale = scale[2];
            }
        }
    }

    private static void undoVisualOffsets(PlayerModel model) {
        float[][] previousOffsets = previousVisualOffsets.get(model);
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            ModelPart part = PlayerModelParts.get(model, slot);
            if (part == null) continue;
            float[] offset = previousOffsets == null ? null : previousOffsets[i];
            if (offset != null && offset.length >= 3) {
                part.x -= offset[0];
                part.y -= offset[1];
                part.z -= offset[2];
                previousOffsets[i] = null;
            }
            part.xScale = 1.0F;
            part.yScale = 1.0F;
            part.zScale = 1.0F;
        }
    }

    private static float[] visualOffsetForSlot(EnumMap<AttachSlot, float[]> offsets, BuiltBoxModel boxModel, AttachSlot slot) {
        if (offsets == null || slot == null) return null;
        float[] offset = offsets.get(slot);
        AttachSlot base = overlayBase(slot);
        if (base == null) return offset;
        float[] baseOffset = offsets.get(base);
        if (baseOffset == null) return offset;
        if (offset == null || (isZeroOffset(offset) && !hasBoxParts(boxModel, slot))) return baseOffset;
        return offset;
    }

    private static AttachSlot overlayBase(AttachSlot slot) {
        return switch (slot) {
            case HAT -> AttachSlot.HEAD;
            case JACKET -> AttachSlot.BODY;
            case RIGHT_SLEEVE -> AttachSlot.RIGHT_ARM;
            case LEFT_SLEEVE -> AttachSlot.LEFT_ARM;
            case RIGHT_PANTS -> AttachSlot.RIGHT_LEG;
            case LEFT_PANTS -> AttachSlot.LEFT_LEG;
            default -> null;
        };
    }

    private static boolean hasBoxParts(BuiltBoxModel boxModel, AttachSlot slot) {
        List<ModelPart> parts = boxModel == null || slot == null ? null : boxModel.get(slot);
        return parts != null && !parts.isEmpty();
    }

    private static boolean isZeroOffset(float[] offset) {
        if (offset == null || offset.length < 3) return false;
        return Math.abs(offset[0]) < 1.0E-4F && Math.abs(offset[1]) < 1.0E-4F && Math.abs(offset[2]) < 1.0E-4F;
    }

    private static void applyBoxVisibility(PlayerModel model, BuiltBoxModel boxModel) {
        if (boxModel == null) return;
        hidePair(model, boxModel, AttachSlot.HEAD, AttachSlot.HAT);
        hidePair(model, boxModel, AttachSlot.BODY, AttachSlot.JACKET);
        hidePair(model, boxModel, AttachSlot.RIGHT_ARM, AttachSlot.RIGHT_SLEEVE);
        hidePair(model, boxModel, AttachSlot.LEFT_ARM, AttachSlot.LEFT_SLEEVE);
        hidePair(model, boxModel, AttachSlot.RIGHT_LEG, AttachSlot.RIGHT_PANTS);
        hidePair(model, boxModel, AttachSlot.LEFT_LEG, AttachSlot.LEFT_PANTS);
    }

    private static void hidePair(PlayerModel model, BuiltBoxModel boxModel, AttachSlot base, AttachSlot overlay) {
        ModelPart basePart = PlayerModelParts.get(model, base);
        ModelPart overlayPart = PlayerModelParts.get(model, overlay);
        if (boxModel.hides(base)) {
            hidePart(basePart);
            hidePart(overlayPart);
        }
        if (boxModel.hides(overlay)) hidePart(overlayPart);
    }

    private static void hidePart(ModelPart part) {
        setSkipDraw(part, true);
    }

    private static void resetBoxVisibility(PlayerModel model) {
        for (AttachSlot slot : PlayerModelParts.ALL) {
            setSkipDraw(PlayerModelParts.get(model, slot), false);
        }
    }

    private static void setSkipDraw(ModelPart part, boolean skip) {
        if (part == null) return;
        part.visible = true;
        ((ModelPartAccessor) (Object) part).consoleskins$setSkipDraw(skip);
    }

    private static void renderBoxModel(PlayerModel model, BuiltBoxModel boxModel, PoseStack pose, VertexConsumer consumer) {
        float partScale = boxModel.partScale();
        boolean hatChildLike = isHatChildLike(model.head, model.hat);
        renderSlot(model.head, boxModel.get(AttachSlot.HEAD), pose, consumer, partScale);
        renderHat(model.head, model.hat, hatChildLike, boxModel.get(AttachSlot.HAT), pose, consumer, partScale);
        renderSlot(model.body, boxModel.get(AttachSlot.BODY), pose, consumer, partScale);
        renderSlot(model.jacket, boxModel.get(AttachSlot.JACKET), pose, consumer, partScale);
        renderSlot(model.rightArm, boxModel.get(AttachSlot.RIGHT_ARM), pose, consumer, partScale);
        renderSlot(model.leftArm, boxModel.get(AttachSlot.LEFT_ARM), pose, consumer, partScale);
        renderSlot(model.rightSleeve, boxModel.get(AttachSlot.RIGHT_SLEEVE), pose, consumer, partScale);
        renderSlot(model.leftSleeve, boxModel.get(AttachSlot.LEFT_SLEEVE), pose, consumer, partScale);
        renderSlot(model.rightLeg, boxModel.get(AttachSlot.RIGHT_LEG), pose, consumer, partScale);
        renderSlot(model.leftLeg, boxModel.get(AttachSlot.LEFT_LEG), pose, consumer, partScale);
        renderSlot(model.rightPants, boxModel.get(AttachSlot.RIGHT_PANTS), pose, consumer, partScale);
        renderSlot(model.leftPants, boxModel.get(AttachSlot.LEFT_PANTS), pose, consumer, partScale);
    }

    private static void renderSlot(ModelPart limb, List<ModelPart> parts, PoseStack pose, VertexConsumer consumer, float partScale) {
        if (limb == null || parts == null || parts.isEmpty()) return;
        pose.pushPose();
        limb.translateAndRotate(pose);
        if (partScale != 1.0F) pose.scale(partScale, partScale, partScale);
        for (ModelPart part : parts) renderModelPart(part, pose, consumer);
        pose.popPose();
    }

    private static void renderHat(ModelPart head, ModelPart hat, boolean hatChildLike, List<ModelPart> parts, PoseStack pose, VertexConsumer consumer, float partScale) {
        if (head == null || hat == null || parts == null || parts.isEmpty()) return;
        pose.pushPose();
        if (hatChildLike) head.translateAndRotate(pose);
        hat.translateAndRotate(pose);
        if (partScale != 1.0F) pose.scale(partScale, partScale, partScale);
        for (ModelPart part : parts) renderModelPart(part, pose, consumer);
        pose.popPose();
    }

    private static boolean isHatChildLike(ModelPart head, ModelPart hat) {
        float epsilon = 1.0E-4F;
        if (Math.abs(hat.x - head.x) > epsilon) return true;
        if (Math.abs(hat.y - head.y) > epsilon) return true;
        if (Math.abs(hat.z - head.z) > epsilon) return true;
        if (Math.abs(hat.xRot - head.xRot) > epsilon) return true;
        if (Math.abs(hat.yRot - head.yRot) > epsilon) return true;
        return Math.abs(hat.zRot - head.zRot) > epsilon;
    }

    private static float normalizeYaw(float yaw) {
        while (yaw < 0.0F) yaw += 360.0F;
        return (yaw + 180.0F) % 360.0F - 180.0F;
    }

    private static ModelPart getCapeModel() {
        if (capeModel == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F), PartPose.ZERO);
            capeModel = LayerDefinition.create(mesh, CAPE_TEXTURE_WIDTH, CAPE_TEXTURE_HEIGHT).bakeRoot().getChild("cape");
        }
        return capeModel;
    }

    private static void renderCape(PlayerModel model, PoseStack pose, VertexConsumer consumer) {
        ModelPart cape = getCapeModel();
        if (model == null || cape == null) return;
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.translate(0.0F, 0.0F, 0.125F);
        pose.mulPose(new Quaternionf().rotationX(6.0F * ((float) Math.PI / 180.0F)));
        pose.mulPose(new Quaternionf().rotationY((float) Math.PI));
        renderModelPart(cape, pose, consumer);
        pose.popPose();
    }

    private static void renderModel(PlayerModel model, PoseStack pose, VertexConsumer consumer) {
        model.renderToBuffer(pose, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY/*? <1.21 {*//*, 1.0F, 1.0F, 1.0F, 1.0F*//*?}*/);
    }

    private static void renderModelPart(ModelPart part, PoseStack pose, VertexConsumer consumer) {
        if (part == null) return;
        ModelPartSkipRenderOverrideAccess access = (ModelPartSkipRenderOverrideAccess) (Object) part;
        boolean previousForceRender = access.consoleskins$getForceRender();
        access.consoleskins$setForceRender(true);
        part.render(pose, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY/*? <1.21 {*//*, 1.0F, 1.0F, 1.0F, 1.0F*//*?}*/);
        access.consoleskins$setForceRender(previousForceRender);
    }
}
