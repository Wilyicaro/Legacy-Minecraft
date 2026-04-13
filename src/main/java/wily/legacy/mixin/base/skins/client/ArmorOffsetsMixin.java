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
import wily.legacy.Skins.client.render.boxloader.*;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.*;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import java.util.EnumMap;
import java.util.EnumSet;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {
    @Unique private static final ThreadLocal<Boolean> consoleskins$posePushed = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> consoleskins$layerActive = new ThreadLocal<>();
    @Unique
    private HumanoidModel<?> consoleskins$getParentHumanoidModel() {
        Object model = ((HumanoidArmorLayer<?, ?, ?>) (Object) this).getParentModel();
        return model instanceof HumanoidModel<?> humanoidModel ? humanoidModel : null;
    }
    @Unique
    private HumanoidModel<?> consoleskins$getArmorModel(EquipmentSlot slot, HumanoidRenderState renderState) {
        if (slot == null || renderState == null) return null;
        HumanoidArmorLayerAccessor<?, ?, ?> accessor = (HumanoidArmorLayerAccessor<?, ?, ?>) this;
        ArmorModelSet<?> modelSet = renderState.isBaby ? accessor.consoleskins$getBabyModelSet() : accessor.consoleskins$getModelSet();
        if (modelSet == null) return null;
        return switch (slot) {
            case HEAD -> (HumanoidModel<?>) modelSet.head();
            case CHEST -> (HumanoidModel<?>) modelSet.chest();
            case LEGS -> (HumanoidModel<?>) modelSet.legs();
            case FEET -> (HumanoidModel<?>) modelSet.feet();
            default -> null;
        };
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
    private static void consoleskins$setForceRender(HumanoidModel<?> model, boolean value) {
        if (model == null) return;
        consoleskins$setForceRender(model.head, value);
        consoleskins$setForceRender(model.body, value);
        consoleskins$setForceRender(model.rightArm, value);
        consoleskins$setForceRender(model.leftArm, value);
        consoleskins$setForceRender(model.rightLeg, value);
        consoleskins$setForceRender(model.leftLeg, value);
        if (model instanceof PlayerModel playerModel) {
            consoleskins$setForceRender(playerModel.hat, value);
            consoleskins$setForceRender(playerModel.jacket, value);
            consoleskins$setForceRender(playerModel.leftSleeve, value);
            consoleskins$setForceRender(playerModel.rightSleeve, value);
            consoleskins$setForceRender(playerModel.leftPants, value);
            consoleskins$setForceRender(playerModel.rightPants, value);
        }
    }
    @Unique
    private static void consoleskins$setForceRender(ModelPart part, boolean value) {
        if (part != null) ((ModelPartSkipRenderOverrideAccess) (Object) part).consoleskins$setForceRender(value);
    }
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                               ItemStack item, EquipmentSlot slot, int packedLight,
                                               HumanoidRenderState renderState, CallbackInfo ci) {
        if (!consoleskins$tryEnterLayer()) return;
        consoleskins$posePushed.set(Boolean.FALSE);
        HumanoidModel<?> armorModel = consoleskins$getArmorModel(slot, renderState);
        consoleskins$setForceRender(armorModel, true);
        if (poseStack == null || slot == null) return;
        if (!(renderState instanceof RenderStateSkinIdAccess access)) return;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
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
            consoleskins$setForceRender(armorModel, false);
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
        if (poseStack != null && Boolean.TRUE.equals(consoleskins$posePushed.get())) poseStack.popPose();
        consoleskins$clearContext();
    }
}
