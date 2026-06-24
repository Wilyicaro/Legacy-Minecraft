package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if <1.21.2 {
import net.minecraft.client.player.AbstractClientPlayer;
//?} else {
/*import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
*///?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.ArmorSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;
import java.util.EnumSet;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {
    @Unique
    private static final ThreadLocal<Boolean> consoleskins$posePushed = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Boolean> consoleskins$layerActive = new ThreadLocal<>();
    //? if >=1.21.2 {
    /*@Unique
    private static final ThreadLocal<HumanoidRenderState> consoleskins$currentState = new ThreadLocal<>();
    *///?}

    @Unique
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

    @Unique
    private HumanoidModel<?> consoleskins$getParentHumanoidModel() {
        Object model = ((HumanoidArmorLayer<?, ?, ?>) (Object) this).getParentModel();
        return model instanceof HumanoidModel<?> humanoidModel ? humanoidModel : null;
    }

    //? if <1.21.2 {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int packedLight, HumanoidModel<?> armorModel, CallbackInfo callbackInfo) {
        if (!consoleskins$tryEnterLayer()) return;
        consoleskins$posePushed.set(Boolean.FALSE);
        consoleskins$setForceRender(armorModel, true);
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin(entity);
        consoleskins$applyArmorContext(poseStack, slot, armorModel, resolved, callbackInfo);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int packedLight, HumanoidModel<?> armorModel, CallbackInfo callbackInfo) {
        consoleskins$popArmorOffsets(poseStack);
    }
    //?} else {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), require = 0)
    private void consoleskins$captureRenderState(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, HumanoidRenderState state, float yRot, float xRot, CallbackInfo callbackInfo) {
        consoleskins$currentState.set(state);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("RETURN"), require = 0)
    private void consoleskins$clearRenderState(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, HumanoidRenderState state, float yRot, float xRot, CallbackInfo callbackInfo) {
        consoleskins$currentState.remove();
    }

    @Inject(method = "getArmorModel", at = @At("HEAD"), require = 0)
    private void consoleskins$captureArmorModelState(HumanoidRenderState state, EquipmentSlot slot, CallbackInfoReturnable<HumanoidModel<?>> callbackInfo) {
        consoleskins$currentState.set(state);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true/^? if forge || neoforge {^//^, remap = false^//^?}^/)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack item, EquipmentSlot slot, int packedLight, HumanoidModel<?> armorModel/^? if forge {^//^, HumanoidRenderState state^//^?}^/, CallbackInfo callbackInfo) {
        if (!consoleskins$tryEnterLayer()) return;
        consoleskins$posePushed.set(Boolean.FALSE);
        consoleskins$setForceRender(armorModel, true);
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin(/^? if forge {^//^state^//^?} else {^/consoleskins$currentState.get()/^?}^/);
        consoleskins$applyArmorContext(poseStack, slot, armorModel, resolved, callbackInfo);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0/^? if forge || neoforge {^//^, remap = false^//^?}^/)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack item, EquipmentSlot slot, int packedLight, HumanoidModel<?> armorModel/^? if forge {^//^, HumanoidRenderState state^//^?}^/, CallbackInfo callbackInfo) {
        consoleskins$popArmorOffsets(poseStack);
    }
    *///?}

    @Unique
    private void consoleskins$applyArmorContext(PoseStack poseStack, EquipmentSlot slot, HumanoidModel<?> armorModel, ClientSkinAssets.ResolvedSkin resolved, CallbackInfo callbackInfo) {
        if (poseStack == null || slot == null || resolved == null) return;
        ResourceLocation modelId = consoleskins$armorModelId(resolved);
        if (modelId == null) return;
        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        if (armorSlot == null) return;
        EnumSet<ArmorSlot> armorHide = BoxModelManager.getArmorHide(modelId);
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);
        BuiltBoxModel boxModel = resolved.boxModel();
        if (boxModel == null) boxModel = BoxModelManager.get(modelId);
        boolean hideArmor = false;
        if (LegacyOptions.hideArmorOnAllBoxSkins.get() && boxModel != null) hideArmor = true;
        else if (armorHide != null && armorHide.contains(armorSlot)) hideArmor = true;
        if (hideArmor) {
            consoleskins$setForceRender(armorModel, false);
            consoleskins$clearContext();
            callbackInfo.cancel();
            return;
        }
        if (armorOffsets == null || armorOffsets.isEmpty()) return;
        float[] offset = armorOffsets.get(armorSlot);
        if (offset == null || offset.length < 3 || (offset[0] == 0.0F && offset[1] == 0.0F && offset[2] == 0.0F)) return;
        float x = offset[0];
        float y = offset[1];
        float z = offset[2];
        if (slot == EquipmentSlot.HEAD) {
            float[] rotated = consoleskins$rotateHeadOffset(x, y, z);
            x = rotated[0];
            y = rotated[1];
            z = rotated[2];
        }
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        consoleskins$posePushed.set(Boolean.TRUE);
    }

    @Unique
    private static ResourceLocation consoleskins$armorModelId(ClientSkinAssets.ResolvedSkin resolved) {
        if (resolved == null) return null;
        ResourceLocation modelId = resolved.modelId();
        if (consoleskins$hasArmorData(modelId)) return modelId;
        ResourceLocation textureModelId = ClientSkinAssets.getModelIdFromTexture(resolved.texture());
        if (consoleskins$hasArmorData(textureModelId)) return textureModelId;
        return modelId != null ? modelId : textureModelId;
    }

    @Unique
    private static boolean consoleskins$hasArmorData(ResourceLocation modelId) {
        if (modelId == null) return false;
        EnumSet<ArmorSlot> armorHide = BoxModelManager.getArmorHide(modelId);
        if (armorHide != null && !armorHide.isEmpty()) return true;
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);
        if (armorOffsets != null && !armorOffsets.isEmpty()) return true;
        return BoxModelManager.get(modelId) != null;
    }

    @Unique
    private float[] consoleskins$rotateHeadOffset(float x, float y, float z) {
        HumanoidModel<?> model = consoleskins$getParentHumanoidModel();
        if (model == null) return new float[]{x, y, z};
        float pitch = model.head.xRot;
        if (pitch != 0.0F) {
            float c = Mth.cos(pitch);
            float s = Mth.sin(pitch);
            float ny = y * c - z * s;
            float nz = y * s + z * c;
            y = ny;
            z = nz;
        }
        float yaw = model.head.yRot;
        if (yaw != 0.0F) {
            float c = Mth.cos(yaw);
            float s = Mth.sin(yaw);
            float nx = x * c + z * s;
            float nz = -x * s + z * c;
            x = nx;
            z = nz;
        }
        float roll = model.head.zRot;
        if (roll != 0.0F) {
            float c = Mth.cos(roll);
            float s = Mth.sin(roll);
            float nx = x * c - y * s;
            float ny = x * s + y * c;
            x = nx;
            y = ny;
        }
        return new float[]{x, y, z};
    }

    @Unique
    private void consoleskins$popArmorOffsets(PoseStack poseStack) {
        if (poseStack != null && Boolean.TRUE.equals(consoleskins$posePushed.get())) poseStack.popPose();
        consoleskins$clearContext();
    }

    //? if <1.21.2 {
    @Unique
    private static ClientSkinAssets.ResolvedSkin consoleskins$resolveSkin(LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayer player)) return null;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        return ClientSkinAssets.resolveSkin(skinId, player.getUUID());
    }
    //?}

    //? if >=1.21.2 {
    /*@Unique
    private static ClientSkinAssets.ResolvedSkin consoleskins$resolveSkin(HumanoidRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access) || SkinIdUtil.isBlankOrAutoSelect(access.consoleskins$getSkinId())) return null;
        return ClientSkinAssets.resolveSkin(access);
    }
    *///?}
}
