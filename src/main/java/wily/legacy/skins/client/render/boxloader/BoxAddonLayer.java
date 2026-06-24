package wily.legacy.skins.client.render.boxloader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if <1.21.2 {
import net.minecraft.client.player.AbstractClientPlayer;
//?} else {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Blocks;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import wily.legacy.compat.cpm.CpmRenderCompat;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.List;
import java.util.UUID;

public class BoxAddonLayer extends RenderLayer</*? if <1.21.2 {*/AbstractClientPlayer, PlayerModel<AbstractClientPlayer>/*?} else {*//*PlayerRenderState, PlayerModel*//*?}*/> {
    public BoxAddonLayer(RenderLayerParent parent) {
        super(parent);
    }

    //? if >=1.21.2 {
    /*private static boolean isInvisible(PlayerRenderState state, RenderStateSkinIdAccess access) {
        if (state.isInvisible) return true;
        UUID uuid = access.consoleskins$getEntityUuid();
        if (uuid == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return false;
        Player player = minecraft.level.getPlayerByUUID(uuid);
        return player != null && player.isInvisible();
    }

    private static int getArmorMask(PlayerRenderState state) {
        if (state == null) return 0;
        int mask = getArmorMaskFromStateFields(state);
        if (mask != 0 || !(state instanceof RenderStateSkinIdAccess access)) return mask;
        UUID uuid = access.consoleskins$getEntityUuid();
        if (uuid == null) return 0;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft == null || minecraft.level == null ? null : minecraft.level.getPlayerByUUID(uuid);
        return getArmorMask(player);
    }
    *///?}

    //? if <1.21.2 {
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player == null || player.isInvisible()) return;
        if (CpmRenderCompat.isCpmModelActive(getParentModel())) return;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
        renderResolvedBox(getParentModel(), resolved, poseStack, bufferSource, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), getArmorMask(player), hasHeadItem(player));
    }
    //?} else {
    /*@Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, PlayerRenderState state, float yRot, float xRot) {
        if (state == null || !(state instanceof RenderStateSkinIdAccess access) || isInvisible(state, access)) return;
        if (CpmRenderCompat.isCpmModelActive(state)) return;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        renderResolvedBox(getParentModel(), resolved, poseStack, bufferSource, packedLight, LivingEntityRenderer.getOverlayCoords(state, 0.0F), getArmorMask(state), hasHeadItem(state));
    }
    *///?}

    private static void renderResolvedBox(PlayerModel model, ClientSkinAssets.ResolvedSkin resolved, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, int armorMask, boolean hideHead) {
        if (model == null || resolved == null) return;
        ResourceLocation texture = resolved.texture();
        if (texture == null) return;
        BuiltBoxModel built = resolved.boxModel();
        if (built == null) return;
        ResourceLocation boxTexture = resolved.boxTexture() == null ? texture : resolved.boxTexture();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(boxTexture));
        float partScale = built.partScale();
        boolean hatChildLike = isHatChildLike(model.head, model.hat);
        if (!hideHead) {
            renderSlot(model.head, built.get(AttachSlot.HEAD, armorMask), poseStack, consumer, packedLight, overlay, partScale);
            renderHat(model.head, model.hat, hatChildLike, built.get(AttachSlot.HAT, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        }
        renderSlot(model.body, built.get(AttachSlot.BODY, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.jacket, built.get(AttachSlot.JACKET, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.rightArm, built.get(AttachSlot.RIGHT_ARM, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.leftArm, built.get(AttachSlot.LEFT_ARM, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.rightSleeve, built.get(AttachSlot.RIGHT_SLEEVE, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.leftSleeve, built.get(AttachSlot.LEFT_SLEEVE, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.rightLeg, built.get(AttachSlot.RIGHT_LEG, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.leftLeg, built.get(AttachSlot.LEFT_LEG, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.rightPants, built.get(AttachSlot.RIGHT_PANTS, armorMask), poseStack, consumer, packedLight, overlay, partScale);
        renderSlot(model.leftPants, built.get(AttachSlot.LEFT_PANTS, armorMask), poseStack, consumer, packedLight, overlay, partScale);
    }

    private static boolean hasItem(ItemStack item) {
        return item != null && !item.isEmpty();
    }

    private static boolean hasHeadItem(Player player) {
        return player != null && isSkullOrPumpkinItem(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    private static boolean hasHeadItem(Object state) {
        if (state == null) return false;
        if (getOptionalStateValue(state, "wornHeadType") != null) return true;
        if (hasItem(getOptionalStateItem(state, "headItem"))) return true;
        return isSkullOrPumpkinItem(getOptionalStateItem(state, "headEquipment"));
    }

    private static boolean isSkullOrPumpkinItem(ItemStack item) {
        if (!hasItem(item) || !(item.getItem() instanceof BlockItem blockItem)) return false;
        var block = blockItem.getBlock();
        return block instanceof AbstractSkullBlock || block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN || block == Blocks.JACK_O_LANTERN;
    }

    private static Object getOptionalStateValue(Object state, String fieldName) {
        if (state == null || fieldName == null) return null;
        try {
            return state.getClass().getField(fieldName).get(state);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static ItemStack getOptionalStateItem(Object state, String fieldName) {
        Object value = getOptionalStateValue(state, fieldName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static int getArmorMaskFromStateFields(Object state) {
        int mask = 0;
        if (hasItem(getOptionalStateItem(state, "headEquipment"))) mask |= ArmorSlot.HELMET.mask();
        if (hasItem(getOptionalStateItem(state, "chestEquipment"))) mask |= ArmorSlot.CHESTPLATE.mask();
        if (hasItem(getOptionalStateItem(state, "legsEquipment"))) mask |= ArmorSlot.LEGGINGS.mask();
        if (hasItem(getOptionalStateItem(state, "feetEquipment"))) mask |= ArmorSlot.BOOTS.mask();
        return mask;
    }

    private static int getArmorMask(Player player) {
        if (player == null) return 0;
        int mask = 0;
        if (hasItem(player.getItemBySlot(EquipmentSlot.HEAD))) mask |= ArmorSlot.HELMET.mask();
        if (hasItem(player.getItemBySlot(EquipmentSlot.CHEST))) mask |= ArmorSlot.CHESTPLATE.mask();
        if (hasItem(player.getItemBySlot(EquipmentSlot.LEGS))) mask |= ArmorSlot.LEGGINGS.mask();
        if (hasItem(player.getItemBySlot(EquipmentSlot.FEET))) mask |= ArmorSlot.BOOTS.mask();
        return mask;
    }

    private static void renderSlot(ModelPart limb, List<ModelPart> parts, PoseStack poseStack, VertexConsumer consumer, int packedLight, int overlay, float partScale) {
        if (limb == null || parts == null || parts.isEmpty()) return;
        poseStack.pushPose();
        limb.translateAndRotate(poseStack);
        if (partScale != 1.0F) poseStack.scale(partScale, partScale, partScale);
        for (ModelPart part : parts) renderModelPart(part, poseStack, consumer, packedLight, overlay);
        poseStack.popPose();
    }

    private static void renderHat(ModelPart head, ModelPart hat, boolean hatChildLike, List<ModelPart> parts, PoseStack poseStack, VertexConsumer consumer, int packedLight, int overlay, float partScale) {
        if (head == null || hat == null || parts == null || parts.isEmpty()) return;
        poseStack.pushPose();
        if (hatChildLike) head.translateAndRotate(poseStack);
        hat.translateAndRotate(poseStack);
        if (partScale != 1.0F) poseStack.scale(partScale, partScale, partScale);
        for (ModelPart part : parts) renderModelPart(part, poseStack, consumer, packedLight, overlay);
        poseStack.popPose();
    }

    private static void renderModelPart(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight, int overlay) {
        if (part == null) return;
        ModelPartSkipRenderOverrideAccess access = (ModelPartSkipRenderOverrideAccess) (Object) part;
        boolean previousForceRender = access.consoleskins$getForceRender();
        access.consoleskins$setForceRender(true);
        part.render(poseStack, consumer, packedLight, overlay/*? <1.21 {*//*, 1.0F, 1.0F, 1.0F, 1.0F*//*?}*/);
        access.consoleskins$setForceRender(previousForceRender);
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
}
