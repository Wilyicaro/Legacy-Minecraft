package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;
import wily.legacy.skins.client.render.ArmorOffsetRenderContext;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.ArmorSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;
import java.util.EnumSet;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {
    @Unique
    private static final ThreadLocal<Boolean> consoleskins$layerActive = new ThreadLocal<>();
    @Unique
    private static final ArmorPart[] consoleskins$NO_PARTS = new ArmorPart[0];
    @Unique
    private static final ArmorPart[] consoleskins$HEAD_PARTS = {
            new ArmorPart(0, AttachSlot.HEAD, AttachSlot.HAT, 8.0F, 8.0F, 8.0F)
    };
    @Unique
    private static final ArmorPart[] consoleskins$CHEST_PARTS = {
            new ArmorPart(1, AttachSlot.BODY, AttachSlot.JACKET, 8.0F, 12.0F, 4.0F),
            new ArmorPart(2, AttachSlot.RIGHT_ARM, AttachSlot.RIGHT_SLEEVE, 4.0F, 12.0F, 4.0F),
            new ArmorPart(3, AttachSlot.LEFT_ARM, AttachSlot.LEFT_SLEEVE, 4.0F, 12.0F, 4.0F)
    };
    @Unique
    private static final ArmorPart[] consoleskins$LEGS_PARTS = {
            new ArmorPart(1, AttachSlot.BODY, AttachSlot.JACKET, 8.0F, 12.0F, 4.0F),
            new ArmorPart(4, AttachSlot.RIGHT_LEG, AttachSlot.RIGHT_PANTS, 4.0F, 12.0F, 4.0F),
            new ArmorPart(5, AttachSlot.LEFT_LEG, AttachSlot.LEFT_PANTS, 4.0F, 12.0F, 4.0F)
    };
    @Unique
    private static final ArmorPart[] consoleskins$FEET_PARTS = {
            new ArmorPart(4, AttachSlot.RIGHT_LEG, AttachSlot.RIGHT_PANTS, 4.0F, 12.0F, 4.0F),
            new ArmorPart(5, AttachSlot.LEFT_LEG, AttachSlot.LEFT_PANTS, 4.0F, 12.0F, 4.0F)
    };

    private static void consoleskins$clearContext() {
        ArmorOffsetRenderContext.clearSubmitOffsets();
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
    private static void consoleskins$setForceRender(net.minecraft.client.model.geom.ModelPart part, boolean value) {
        if (part != null) ((ModelPartSkipRenderOverrideAccess) (Object) part).consoleskins$setForceRender(value);
    }

    @Unique
    private static boolean consoleskins$isZero(float[] offset) {
        return offset == null || offset.length < 3 || offset[0] == 0.0F && offset[1] == 0.0F && offset[2] == 0.0F;
    }

    @Unique
    private static boolean consoleskins$hasOffsets(float[][] offsets) {
        if (offsets == null) return false;
        for (float[] offset : offsets) {
            if (!consoleskins$isZero(offset)) return true;
        }
        return false;
    }

    @Unique
    private static boolean consoleskins$isIdentity(float[] scale) {
        return scale == null || scale.length < 3 || scale[0] == 1.0F && scale[1] == 1.0F && scale[2] == 1.0F;
    }

    @Unique
    private static boolean consoleskins$hasScales(float[][] scales) {
        if (scales == null) return false;
        for (float[] scale : scales) {
            if (!consoleskins$isIdentity(scale)) return true;
        }
        return false;
    }

    @Unique
    private static void consoleskins$setScale(float[][] scales, int index, float[] scale) {
        if (scales == null || consoleskins$isIdentity(scale)) return;
        scales[index] = scale;
    }

    @Unique
    private static ArmorPart[] consoleskins$parts(EquipmentSlot slot) {
        if (slot == null) return consoleskins$NO_PARTS;
        return switch (slot) {
            case HEAD -> consoleskins$HEAD_PARTS;
            case CHEST -> consoleskins$CHEST_PARTS;
            case LEGS -> consoleskins$LEGS_PARTS;
            case FEET -> consoleskins$FEET_PARTS;
            default -> consoleskins$NO_PARTS;
        };
    }

    @Unique
    private static void consoleskins$overrideScale(float[][] scales, ArmorPart part, EnumMap<AttachSlot, float[]> overrides) {
        if (scales == null || part == null || overrides == null) return;
        float[] scale;
        if (overrides.containsKey(part.base())) scale = overrides.get(part.base());
        else if (part.overlay() != null && overrides.containsKey(part.overlay())) scale = overrides.get(part.overlay());
        else return;
        scales[part.index()] = consoleskins$isIdentity(scale) ? null : scale;
    }

    @Unique
    private static float[][] consoleskins$renderOffsets(EquipmentSlot slot, float[] offset) {
        float[][] offsets = new float[6][];
        if (consoleskins$isZero(offset)) return offsets;
        for (ArmorPart part : consoleskins$parts(slot)) {
            offsets[part.index()] = offset;
        }
        return offsets;
    }

    @Unique
    private static float consoleskins$clampScale(float scale) {
        if (scale < 0.5F) return 0.5F;
        return Math.min(scale, 3.0F);
    }

    @Unique
    private static float consoleskins$axisScale(float size, float base) {
        if (base <= 0.0F || size <= 0.01F) return 1.0F;
        float scale = size / base;
        if (scale > 0.8F && scale < 1.2F) return 1.0F;
        return consoleskins$clampScale(scale);
    }

    @Unique
    private static float[] consoleskins$coreScaleFor(BuiltBoxModel model, ArmorPart part) {
        if (model == null || part == null) return null;
        float[] size = model.coreSlotSize(part.base());
        if (size == null && part.overlay() != null) size = model.coreSlotSize(part.overlay());
        if (size == null || size.length < 3) return null;
        float x = consoleskins$axisScale(size[0], part.baseX());
        float y = consoleskins$axisScale(size[1], part.baseY());
        float z = consoleskins$axisScale(size[2], part.baseZ());
        return x == 1.0F && y == 1.0F && z == 1.0F ? null : new float[]{x, y, z};
    }

    @Unique
    private static float[][] consoleskins$autoScales(EquipmentSlot slot, BuiltBoxModel model) {
        float[][] scales = new float[6][];
        if (model == null) return scales;
        for (ArmorPart part : consoleskins$parts(slot)) {
            consoleskins$setScale(scales, part.index(), consoleskins$coreScaleFor(model, part));
        }
        return scales;
    }

    @Unique
    private static void consoleskins$applyScaleOverrides(EquipmentSlot slot, float[][] scales, EnumMap<AttachSlot, float[]> overrides) {
        if (slot == null || scales == null || overrides == null || overrides.isEmpty()) return;
        for (ArmorPart part : consoleskins$parts(slot)) {
            consoleskins$overrideScale(scales, part, overrides);
        }
    }

    @Unique
    private static ArmorOffsetRenderContext.Offsets consoleskins$offsetData(float[][] renderOffsets, float[][] scales) {
        float[][] renders = consoleskins$hasOffsets(renderOffsets) ? renderOffsets : null;
        float[][] autoScales = consoleskins$hasScales(scales) ? scales : null;
        return renders == null && autoScales == null ? null : new ArmorOffsetRenderContext.Offsets(renders, autoScales);
    }

    @Unique
    private static void consoleskins$setSubmitOffsets(float[][] renderOffsets, float[][] scales) {
        ArmorOffsetRenderContext.setSubmitOffsets(consoleskins$offsetData(renderOffsets, scales));
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

    private record ArmorPart(int index, AttachSlot base, AttachSlot overlay, float baseX, float baseY, float baseZ) {
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                               ItemStack item, EquipmentSlot slot, int packedLight,
                                               HumanoidRenderState renderState, CallbackInfo ci) {
        if (!consoleskins$tryEnterLayer()) return;
        ArmorOffsetRenderContext.clearSubmitOffsets();
        HumanoidModel<?> armorModel = consoleskins$getArmorModel(slot, renderState);
        consoleskins$setForceRender(armorModel, true);
        if (slot == null) return;
        if (!(renderState instanceof RenderStateSkinIdAccess access)) return;
        String skinId = access.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        Identifier modelId = resolved == null ? null : resolved.modelId();
        if (modelId == null) return;
        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        if (armorSlot == null) return;
        EnumSet<ArmorSlot> armorHide = BoxModelManager.getArmorHide(modelId);
        BuiltBoxModel boxModel = resolved.boxModel();
        boolean hasBoxModel = boxModel != null;
        boolean hideArmor = false;
        if (LegacyOptions.hideArmorOnAllBoxSkins.get() && hasBoxModel) hideArmor = true;
        else if (armorHide != null && armorHide.contains(armorSlot)) hideArmor = true;
        if (hideArmor) {
            consoleskins$setForceRender(armorModel, false);
            consoleskins$clearContext();
            ci.cancel();
            return;
        }
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);
        float[][] renderOffsets = consoleskins$renderOffsets(slot, armorOffsets == null ? null : armorOffsets.get(armorSlot));
        float[][] scales = Boolean.TRUE.equals(BoxModelManager.getArmorStretch(modelId)) ? consoleskins$autoScales(slot, boxModel) : new float[6][];
        consoleskins$applyScaleOverrides(slot, scales, BoxModelManager.getArmorScales(modelId));
        if (!consoleskins$hasOffsets(renderOffsets) && !consoleskins$hasScales(scales)) return;
        consoleskins$setSubmitOffsets(renderOffsets, scales);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                              ItemStack item, EquipmentSlot slot, int packedLight,
                                              HumanoidRenderState renderState, CallbackInfo ci) {
        consoleskins$clearContext();
    }
}
