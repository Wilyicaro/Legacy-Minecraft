package wily.legacy.mixin.base.skins.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
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
import wily.legacy.Skins.client.render.ArmorOffsetContext;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.ArmorSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.client.ModelPartSkipRenderOverrideAccess;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorOffsetsMixin {

    @Unique private static Field consoleskins$rendererField;

    @Unique private Set<ModelPart> consoleskins$cachedArmorParts;
    @Unique private boolean consoleskins$cachedArmorPartsFailed;


    @Unique
    private RenderLayerParent<?, ?> consoleskins$getRenderer() {
        try {
            Field f = consoleskins$rendererField;
            if (f == null) {
                Class<?> c = ((Object) this).getClass();
                outer:
                while (c != null && c != Object.class) {
                    for (Field ff : c.getDeclaredFields()) {
                        if (RenderLayerParent.class.isAssignableFrom(ff.getType())) {
                            ff.setAccessible(true);
                            consoleskins$rendererField = ff;
                            f = ff;
                            break outer;
                        }
                    }
                    c = c.getSuperclass();
                }
            }
            if (f != null) return (RenderLayerParent<?, ?>) f.get(this);
        } catch (Throwable ignored) {}
        return null;
    }

    @Unique
    private static void consoleskins$collectParts(Object obj, int depth, Set<ModelPart> out, Set<Object> visited) {
        if (obj == null || depth > 3 || !visited.add(obj)) return;
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                Class<?> ft = f.getType();
                if (ft.isPrimitive() || ft == String.class || ft == Class.class) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val == null) continue;
                    if (val instanceof ModelPart mp) {
                        out.add(mp);
                        if (depth < 3) consoleskins$collectParts(mp, depth + 1, out, visited);
                    } else if (val instanceof java.util.Map<?,?> map) {
                        for (Object v : map.values()) {
                            if (v instanceof ModelPart mp) {
                                out.add(mp);
                                if (depth < 3) consoleskins$collectParts(mp, depth + 1, out, visited);
                            }
                        }
                    } else if (depth < 2) {
                        consoleskins$collectParts(val, depth + 1, out, visited);
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
    }

    @Unique
    private void consoleskins$collectArmorOnlyParts(Set<ModelPart> out, Set<Object> visited) {
        RenderLayerParent<?, ?> renderer = consoleskins$getRenderer();
        Object playerModel = null;
        try { if (renderer != null) playerModel = renderer.getModel(); } catch (Throwable ignored) {}

        Set<ModelPart> playerParts = new HashSet<>();
        if (playerModel != null) consoleskins$collectParts(playerModel, 0, playerParts, new HashSet<>());

        Class<?> c = ((Object) this).getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                if (RenderLayerParent.class.isAssignableFrom(f.getType())) continue;
                Class<?> ft = f.getType();
                if (ft.isPrimitive() || ft == String.class || ft == Class.class) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(this);
                    if (val == null || val == renderer || val == playerModel) continue;
                    consoleskins$collectParts(val, 0, out, visited);
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }

        out.removeAll(playerParts);
    }

    private static void consoleskins$clearContext() {
        ArmorOffsetContext.CURRENT_SLOT.remove();
        ArmorOffsetContext.CURRENT_OFFSET.remove();
        ArmorOffsetContext.POSE_PUSHED.remove();
        ArmorOffsetContext.APPLIED.remove();
        ArmorOffsetContext.FORCE_ARMOR_VISIBLE.remove();
        ArmorOffsetContext.LAYER_ACTIVE.remove();
        ArmorOffsetContext.PARENT_MODEL.remove();
        ArmorOffsetContext.PARENT_VIS.remove();
    }

    @Unique
    private static boolean consoleskins$tryEnterLayer() {
        if (Boolean.TRUE.equals(ArmorOffsetContext.LAYER_ACTIVE.get())) return false;
        ArmorOffsetContext.LAYER_ACTIVE.set(Boolean.TRUE);
        return true;
    }

    @Unique
    private static void consoleskins$setForceRender(ModelPart p, boolean value) {
        try { ((ModelPartSkipRenderOverrideAccess)(Object)p).consoleskins$setForceRender(value); }
        catch (Throwable ignored) {}
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
            } catch (Throwable t) {
                consoleskins$cachedArmorPartsFailed = true;
            }
        }

        if (armorParts != null && !armorParts.isEmpty()) {
            for (ModelPart p : armorParts) consoleskins$setForceRender(p, true);
        }

        ArmorOffsetContext.CURRENT_SLOT.set(slot);
        ArmorOffsetContext.CURRENT_OFFSET.remove();
        ArmorOffsetContext.POSE_PUSHED.set(Boolean.FALSE);
        ArmorOffsetContext.APPLIED.set(Boolean.FALSE);
        ArmorOffsetContext.FORCE_ARMOR_VISIBLE.set(Boolean.TRUE);

        if (poseStack == null || slot == null) return;
        if (!(renderState instanceof RenderStateSkinIdAccess a)) return;

        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        ResourceLocation tex = a.consoleskins$getCachedTexture();
        if (tex == null) {
            SkinEntry entry = SkinPackLoader.getSkin(skinId);
            tex = ClientSkinAssets.getTexture(skinId);
            if (tex == null && entry != null) tex = entry.texture();
        }
        if (tex == null) return;

        ResourceLocation modelId = a.consoleskins$getCachedModelId();
        if (modelId == null) modelId = ClientSkinAssets.getModelIdFromTexture(tex);

        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        if (armorSlot == null) return;

        EnumSet<ArmorSlot> armorHide    = BoxModelManager.getArmorHide(modelId);
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);
        boolean hasBoxModel = BoxModelManager.isAvailable(modelId);

        if (!hasBoxModel || ((armorHide == null || armorHide.isEmpty()) && (armorOffsets == null || armorOffsets.isEmpty()))) {
            JsonObject mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            armorHide    = BoxModelManager.getArmorHide(modelId);
            armorOffsets = BoxModelManager.getArmorOffsets(modelId);
            hasBoxModel  = BoxModelManager.isAvailable(modelId);
        }

        boolean hideArmor = false;
        if (ConsoleSkinsClientSettings.isHideArmorOnAllBoxSkins() && hasBoxModel) hideArmor = true;
        else if (armorHide != null && armorHide.contains(armorSlot))              hideArmor = true;

        if (hideArmor) {
            DebugLog.debug("[ArmorFix] HIDE slot={} armorHide={}", slot, armorHide);
            if (armorParts != null) for (ModelPart mp : armorParts) consoleskins$setForceRender(mp, false);
            consoleskins$clearContext();
            ci.cancel();
            return;
        }

        if (armorOffsets == null || armorOffsets.isEmpty()) return;
        float[] v = armorOffsets.get(armorSlot);
        if (v == null || (v[0] == 0 && v[1] == 0 && v[2] == 0)) return;

        ArmorOffsetContext.CURRENT_OFFSET.set(new float[]{v[0], v[1], v[2]});

        if (slot == EquipmentSlot.HEAD) {
            float x = v[0], y = v[1], z = v[2];
            float pitch = renderState.xRot * ((float) Math.PI / 180f);
            if (pitch != 0f) { float c = Mth.cos(pitch), s = Mth.sin(pitch); float ny=y*c-z*s, nz=y*s+z*c; y=ny; z=nz; }
            float yaw   = renderState.yRot * ((float) Math.PI / 180f);
            if (yaw   != 0f) { float c = Mth.cos(yaw),   s = Mth.sin(yaw);   float nx=x*c+z*s, nz2=-x*s+z*c; x=nx; z=nz2; }
            poseStack.pushPose();
            poseStack.translate(x / 16f, y / 16f, z / 16f);
            ArmorOffsetContext.POSE_PUSHED.set(Boolean.TRUE);
            ArmorOffsetContext.APPLIED.set(Boolean.TRUE);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(v[0] / 16f, v[1] / 16f, v[2] / 16f);
        ArmorOffsetContext.POSE_PUSHED.set(Boolean.TRUE);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector,
            ItemStack item, EquipmentSlot slot, int packedLight,
            HumanoidRenderState renderState, CallbackInfo ci) {
        if (poseStack != null) {
            try { if (Boolean.TRUE.equals(ArmorOffsetContext.POSE_PUSHED.get())) poseStack.popPose(); }
            catch (Throwable ignored) {}
        }
        consoleskins$clearContext();
    }
}
