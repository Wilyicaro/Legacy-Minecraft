package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
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
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {
    @Unique private static final ThreadLocal<EquipmentSlot> consoleskins$currentSlot = new ThreadLocal<>();
    @Unique private static final ThreadLocal<float[]> consoleskins$currentOffset = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> consoleskins$posePushed = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> consoleskins$layerActive = new ThreadLocal<>();
    @Unique
    private static Field consoleskins$rendererField;
    @Unique
    private Set<ModelPart> consoleskins$cachedArmorParts;
    @Unique
    private boolean consoleskins$cachedArmorPartsFailed;
    @Unique
    private RenderLayerParent<?, ?> consoleskins$getRenderer() {
        try {
            Field field = consoleskins$rendererField;
            if (field == null) {
                Class<?> type = ((Object) this).getClass();
                outer:
                while (type != null && type != Object.class) {
                    for (Field candidate : type.getDeclaredFields()) {
                        if (RenderLayerParent.class.isAssignableFrom(candidate.getType())) {
                            candidate.setAccessible(true);
                            consoleskins$rendererField = candidate;
                            field = candidate;
                            break outer;
                        }
                    }
                    type = type.getSuperclass();
                }
            }
            if (field != null) return (RenderLayerParent<?, ?>) field.get(this);
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        return null;
    }
    @Unique
    private HumanoidModel<?> consoleskins$getParentHumanoidModel() {
        RenderLayerParent<?, ?> renderer = consoleskins$getRenderer();
        if (renderer == null) return null;
        try {
            Object model = renderer.getModel();
            return model instanceof HumanoidModel<?> humanoidModel ? humanoidModel : null;
        } catch (RuntimeException ignored) { return null; }
    }
    @Unique
    private static void consoleskins$collectParts(Object obj, int depth, Set<ModelPart> out, Set<Object> visited) {
        if (obj == null || depth > 3 || !visited.add(obj)) return;
        Class<?> type = obj.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic()) continue;
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive() || fieldType == String.class || fieldType == Class.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value == null) continue;
                    if (value instanceof ModelPart modelPart) {
                        out.add(modelPart);
                        if (depth < 3) consoleskins$collectParts(modelPart, depth + 1, out, visited);
                    } else if (value instanceof java.util.Map<?, ?> map) {
                        for (Object child : map.values()) {
                            if (child instanceof ModelPart modelPart) {
                                out.add(modelPart);
                                if (depth < 3) consoleskins$collectParts(modelPart, depth + 1, out, visited);
                            }
                        }
                    } else if (depth < 2) { consoleskins$collectParts(value, depth + 1, out, visited); }
                } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
            type = type.getSuperclass();
        }
    }
    @Unique
    private void consoleskins$collectArmorOnlyParts(Set<ModelPart> out, Set<Object> visited) {
        RenderLayerParent<?, ?> renderer = consoleskins$getRenderer();
        Object playerModel = null;
        try {
            if (renderer != null) playerModel = renderer.getModel();
        } catch (RuntimeException ignored) { }
        Set<ModelPart> playerParts = new HashSet<>();
        if (playerModel != null) consoleskins$collectParts(playerModel, 0, playerParts, new HashSet<>());
        Class<?> type = ((Object) this).getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic()) continue;
                if (RenderLayerParent.class.isAssignableFrom(field.getType())) continue;
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive() || fieldType == String.class || fieldType == Class.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value == null || value == renderer || value == playerModel) continue;
                    consoleskins$collectParts(value, 0, out, visited);
                } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
            type = type.getSuperclass();
        }
        out.removeAll(playerParts);
    }
    private static void consoleskins$clearContext() {
        consoleskins$currentSlot.remove();
        consoleskins$currentOffset.remove();
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
        Set<ModelPart> armorParts = consoleskins$cachedArmorParts;
        if (armorParts == null && !consoleskins$cachedArmorPartsFailed) {
            try {
                Set<ModelPart> parts = new HashSet<>();
                consoleskins$collectArmorOnlyParts(parts, new HashSet<>());
                armorParts = Set.copyOf(parts);
                consoleskins$cachedArmorParts = armorParts;
            } catch (RuntimeException ignored) { consoleskins$cachedArmorPartsFailed = true; }
        }
        if (armorParts != null && !armorParts.isEmpty()) { for (ModelPart part : armorParts) consoleskins$setForceRender(part, true); }
        consoleskins$currentSlot.set(slot);
        consoleskins$currentOffset.remove();
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
        consoleskins$currentOffset.set(new float[]{offset[0], offset[1], offset[2]});
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
