package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.ArmorSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {
    @Unique private static final ThreadLocal<Boolean> consoleskins$posePushed = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> consoleskins$layerActive = new ThreadLocal<>();
    @Unique
    private Set<ModelPart> consoleskins$cachedArmorParts;
    @Unique
    private HumanoidModel<?> consoleskins$getParentHumanoidModel() {
        try {
            Object model = ((HumanoidArmorLayer<?, ?, ?>) (Object) this).getParentModel();
            return model instanceof HumanoidModel<?> humanoidModel ? humanoidModel : null;
        } catch (RuntimeException ignored) { return null; }
    }
    @Unique
    private Set<ModelPart> consoleskins$getArmorParts() {
        Set<ModelPart> armorParts = consoleskins$cachedArmorParts;
        if (armorParts != null) return armorParts;
        java.util.LinkedHashSet<ModelPart> parts = new java.util.LinkedHashSet<>();
        HumanoidArmorLayerAccessor<?, ?, ?> accessor = (HumanoidArmorLayerAccessor<?, ?, ?>) this;
        consoleskins$collectArmorParts(parts, accessor.consoleskins$getModelSet());
        consoleskins$collectArmorParts(parts, accessor.consoleskins$getBabyModelSet());
        armorParts = Set.copyOf(parts);
        consoleskins$cachedArmorParts = armorParts;
        return armorParts;
    }
    @Unique
    private static void consoleskins$collectArmorParts(Set<ModelPart> out, ArmorModelSet<?> modelSet) {
        if (modelSet == null) return;
        consoleskins$collectModelParts(out, (HumanoidModel<?>) modelSet.head());
        consoleskins$collectModelParts(out, (HumanoidModel<?>) modelSet.chest());
        consoleskins$collectModelParts(out, (HumanoidModel<?>) modelSet.legs());
        consoleskins$collectModelParts(out, (HumanoidModel<?>) modelSet.feet());
    }
    @Unique
    private static void consoleskins$collectModelParts(Set<ModelPart> out, HumanoidModel<?> model) {
        if (model == null) return;
        out.add(model.head);
        out.add(model.body);
        out.add(model.rightArm);
        out.add(model.leftArm);
        out.add(model.rightLeg);
        out.add(model.leftLeg);
        if (model instanceof PlayerModel playerModel) {
            out.add(playerModel.hat);
            out.add(playerModel.jacket);
            out.add(playerModel.leftSleeve);
            out.add(playerModel.rightSleeve);
            out.add(playerModel.leftPants);
            out.add(playerModel.rightPants);
        }
    }
    private static void consoleskins$clearContext() {
        consoleskins$posePushed.remove();
        consoleskins$layerActive.remove();
    }
    @Unique
    private static boolean consoleskins$tryEnterLayer() {
        if (Boolean.TRUE.equals(consoleskins$layerActive.get())) return false;
        consoleskins$layerActive.set(Boolean.TRUE);
        return true;
    }
    @Unique
    private static void consoleskins$setForceRender(ModelPart part, boolean value) {
        try {
            ((ModelPartSkipRenderOverrideAccess) (Object) part).consoleskins$setForceRender(value);
        } catch (RuntimeException ignored) { }
    }
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                               ItemStack item, EquipmentSlot slot, int packedLight,
                                               HumanoidRenderState renderState, CallbackInfo ci) {
        if (!consoleskins$tryEnterLayer()) return;
        Set<ModelPart> armorParts = consoleskins$getArmorParts();
        if (armorParts != null && !armorParts.isEmpty()) { for (ModelPart part : armorParts) consoleskins$setForceRender(part, true); }
        consoleskins$posePushed.set(Boolean.FALSE);
        if (poseStack == null || slot == null) return;
        if (!(renderState instanceof RenderStateSkinIdAccess access)) return;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(
                skinId,
                access.consoleskins$getCachedTexture(),
                access.consoleskins$getCachedModelId(),
                access.consoleskins$getCachedBoxModel()
        );
        ResourceLocation modelId = resolved == null ? null : resolved.modelId();
        if (modelId == null) return;
        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        if (armorSlot == null) return;
        EnumSet<ArmorSlot> armorHide = BoxModelManager.getArmorHide(modelId);
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);
        boolean hasBoxModel = resolved.boxModel() != null;
        boolean hideArmor = false;
        if (ConsoleSkinsClientSettings.isHideArmorOnAllBoxSkins() && hasBoxModel) hideArmor = true;
        else if (armorHide != null && armorHide.contains(armorSlot)) hideArmor = true;
        if (hideArmor) {
            if (armorParts != null) { for (ModelPart part : armorParts) consoleskins$setForceRender(part, false); }
            consoleskins$clearContext();
            ci.cancel();
            return;
        }
        if (armorOffsets == null || armorOffsets.isEmpty()) return;
        float[] offset = armorOffsets.get(armorSlot);
        if (offset == null || (offset[0] == 0 && offset[1] == 0 && offset[2] == 0)) return;
        if (slot == EquipmentSlot.HEAD) {
            float x = offset[0];
            float y = offset[1];
            float z = offset[2];
            HumanoidModel<?> model = consoleskins$getParentHumanoidModel();
            float pitch = model != null ? model.head.xRot : renderState.xRot * ((float) Math.PI / 180f);
            if (pitch != 0f) {
                float c = Mth.cos(pitch);
                float s = Mth.sin(pitch);
                float ny = y * c - z * s;
                float nz = y * s + z * c;
                y = ny;
                z = nz;
            }
            float yaw = model != null ? model.head.yRot : renderState.yRot * ((float) Math.PI / 180f);
            if (yaw != 0f) {
                float c = Mth.cos(yaw);
                float s = Mth.sin(yaw);
                float nx = x * c + z * s;
                float nz = -x * s + z * c;
                x = nx;
                z = nz;
            }
            float roll = model != null ? model.head.zRot : 0f;
            if (roll != 0f) {
                float c = Mth.cos(roll);
                float s = Mth.sin(roll);
                float nx = x * c - y * s;
                float ny = x * s + y * c;
                x = nx;
                y = ny;
            }
            poseStack.pushPose();
            poseStack.translate(x / 16f, y / 16f, z / 16f);
            consoleskins$posePushed.set(Boolean.TRUE);
            return;
        }
        poseStack.pushPose();
        poseStack.translate(offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
        consoleskins$posePushed.set(Boolean.TRUE);
    }
    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                              ItemStack item, EquipmentSlot slot, int packedLight,
                                              HumanoidRenderState renderState, CallbackInfo ci) {
        if (poseStack != null) {
            try {
                if (Boolean.TRUE.equals(consoleskins$posePushed.get())) poseStack.popPose();
            } catch (RuntimeException ignored) { }
        }
        consoleskins$clearContext();
    }
}
