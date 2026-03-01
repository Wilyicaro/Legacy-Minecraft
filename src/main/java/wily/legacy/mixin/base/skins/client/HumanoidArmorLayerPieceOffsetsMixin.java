package wily.legacy.mixin.base.skins.client;

import com.google.gson.JsonObject;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.ArmorOffsetContext;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.ArmorSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.EnumMap;
import java.util.EnumSet;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerPieceOffsetsMixin {

    @Unique
    private static java.lang.reflect.Field consoleskins$rendererField;

    @Unique
    private RenderLayerParent<?, ?> consoleskins$getRenderer() {
        try {
            java.lang.reflect.Field f = consoleskins$rendererField;
            if (f == null) {
                Class<?> c = ((Object) this).getClass();
                while (c != null && c != Object.class && f == null) {
                    for (java.lang.reflect.Field ff : c.getDeclaredFields()) {
                        if (RenderLayerParent.class.isAssignableFrom(ff.getType())) {
                            ff.setAccessible(true);
                            f = ff;
                            consoleskins$rendererField = ff;
                            break;
                        }
                    }
                    c = c.getSuperclass();
                }
            }
            if (f != null) return (RenderLayerParent<?, ?>) f.get(this);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void consoleskins$clearContext() {
        ArmorOffsetContext.CURRENT_SLOT.remove();
        ArmorOffsetContext.CURRENT_OFFSET.remove();
        ArmorOffsetContext.POSE_PUSHED.remove();
        ArmorOffsetContext.APPLIED.remove();
        ArmorOffsetContext.FORCE_ARMOR_VISIBLE.remove();
        ArmorOffsetContext.PARENT_MODEL.remove();
        ArmorOffsetContext.PARENT_VIS.remove();
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0, cancellable = true)
    private void consoleskins$pushArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector, ItemStack item, EquipmentSlot slot, int packedLight, HumanoidRenderState renderState, CallbackInfo ci) {
        ArmorOffsetContext.CURRENT_SLOT.set(slot);
        ArmorOffsetContext.CURRENT_OFFSET.remove();
        ArmorOffsetContext.POSE_PUSHED.set(Boolean.FALSE);
        ArmorOffsetContext.APPLIED.set(Boolean.FALSE);
        ArmorOffsetContext.FORCE_ARMOR_VISIBLE.set(Boolean.TRUE);

        try {
            RenderLayerParent<?, ?> renderer = consoleskins$getRenderer();
            Object parentModel = renderer != null ? renderer.getModel() : null;
            ArmorOffsetContext.PARENT_MODEL.set(parentModel);
            if (parentModel instanceof HumanoidModel hm) {
                boolean[] st = new boolean[14];

                ModelPart head = hm.head;
                ModelPart hat = hm.hat;
                ModelPart body = hm.body;
                ModelPart ra = hm.rightArm;
                ModelPart la = hm.leftArm;
                ModelPart rl = hm.rightLeg;
                ModelPart ll = hm.leftLeg;

                ModelPartSkipDrawAccessorMixin headA = (ModelPartSkipDrawAccessorMixin) (Object) head;
                ModelPartSkipDrawAccessorMixin hatA = (ModelPartSkipDrawAccessorMixin) (Object) hat;
                ModelPartSkipDrawAccessorMixin bodyA = (ModelPartSkipDrawAccessorMixin) (Object) body;
                ModelPartSkipDrawAccessorMixin raA = (ModelPartSkipDrawAccessorMixin) (Object) ra;
                ModelPartSkipDrawAccessorMixin laA = (ModelPartSkipDrawAccessorMixin) (Object) la;
                ModelPartSkipDrawAccessorMixin rlA = (ModelPartSkipDrawAccessorMixin) (Object) rl;
                ModelPartSkipDrawAccessorMixin llA = (ModelPartSkipDrawAccessorMixin) (Object) ll;

                st[0] = head.visible; st[1] = headA.consoleskins$getSkipDraw();
                st[2] = hat.visible;  st[3] = hatA.consoleskins$getSkipDraw();
                st[4] = body.visible; st[5] = bodyA.consoleskins$getSkipDraw();
                st[6] = ra.visible;   st[7] = raA.consoleskins$getSkipDraw();
                st[8] = la.visible;   st[9] = laA.consoleskins$getSkipDraw();
                st[10] = rl.visible;  st[11] = rlA.consoleskins$getSkipDraw();
                st[12] = ll.visible;  st[13] = llA.consoleskins$getSkipDraw();
                ArmorOffsetContext.PARENT_VIS.set(st);

                head.visible = true; headA.consoleskins$setSkipDraw(false);
                hat.visible = true;  hatA.consoleskins$setSkipDraw(false);
                body.visible = true; bodyA.consoleskins$setSkipDraw(false);
                ra.visible = true;   raA.consoleskins$setSkipDraw(false);
                la.visible = true;   laA.consoleskins$setSkipDraw(false);
                rl.visible = true;   rlA.consoleskins$setSkipDraw(false);
                ll.visible = true;   llA.consoleskins$setSkipDraw(false);
            }
        } catch (Throwable ignored) {
        }

        if (poseStack == null || slot == null) return;
        if (!(renderState instanceof RenderStateSkinIdAccess a)) return;

        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation tex = entry != null ? entry.texture() : null;
        if (tex == null) tex = ClientSkinAssets.getTexture(skinId);
        if (tex == null) return;

        String p = tex.getPath();
        int slash = p.lastIndexOf('/');
        if (slash != -1) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), p);

        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        if (armorSlot == null) return;

        EnumSet<ArmorSlot> armorHide = BoxModelManager.getArmorHide(modelId);
        EnumMap<ArmorSlot, float[]> armorOffsets = BoxModelManager.getArmorOffsets(modelId);

        boolean hasBoxModel = BoxModelManager.isAvailable(modelId);

        if (!hasBoxModel || ((armorHide == null || armorHide.isEmpty()) && (armorOffsets == null || armorOffsets.isEmpty()))) {
            JsonObject mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            armorHide = BoxModelManager.getArmorHide(modelId);
            armorOffsets = BoxModelManager.getArmorOffsets(modelId);
            hasBoxModel = BoxModelManager.isAvailable(modelId);
        }

        boolean hideArmor = false;
        if (ConsoleSkinsClientSettings.isHideArmorOnAllBoxSkins() && hasBoxModel) {
            hideArmor = true;
        } else if (armorHide != null && armorHide.contains(armorSlot)) {
            hideArmor = true;
        }

        if (hideArmor) {
            try {
                Object parentModel = ArmorOffsetContext.PARENT_MODEL.get();
                boolean[] st = ArmorOffsetContext.PARENT_VIS.get();
                if (parentModel instanceof HumanoidModel hm && st != null && st.length >= 14) {
                    ModelPart head = hm.head;
                    ModelPart hat = hm.hat;
                    ModelPart body = hm.body;
                    ModelPart ra = hm.rightArm;
                    ModelPart la = hm.leftArm;
                    ModelPart rl = hm.rightLeg;
                    ModelPart ll = hm.leftLeg;

                    ((ModelPartSkipDrawAccessorMixin) (Object) head).consoleskins$setSkipDraw(st[1]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) hat).consoleskins$setSkipDraw(st[3]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) body).consoleskins$setSkipDraw(st[5]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) ra).consoleskins$setSkipDraw(st[7]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) la).consoleskins$setSkipDraw(st[9]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) rl).consoleskins$setSkipDraw(st[11]);
                    ((ModelPartSkipDrawAccessorMixin) (Object) ll).consoleskins$setSkipDraw(st[13]);

                    head.visible = st[0];
                    hat.visible  = st[2];
                    body.visible = st[4];
                    ra.visible   = st[6];
                    la.visible   = st[8];
                    rl.visible   = st[10];
                    ll.visible   = st[12];
                }
            } catch (Throwable ignored) {
            }

            consoleskins$clearContext();
            ci.cancel();
            return;
        }

        if (armorOffsets == null || armorOffsets.isEmpty()) return;
        float[] v = armorOffsets.get(armorSlot);
        if (v == null) return;
        if (v[0] == 0 && v[1] == 0 && v[2] == 0) return;

        ArmorOffsetContext.CURRENT_OFFSET.set(new float[]{v[0], v[1], v[2]});

        if (slot == EquipmentSlot.HEAD) {
            float x = v[0];
            float y = v[1];
            float z = v[2];

            float pitch = renderState.xRot * ((float) Math.PI / 180.0F);
            if (pitch != 0.0F) {
                float c = Mth.cos(pitch);
                float s = Mth.sin(pitch);
                float ny = y * c - z * s;
                float nz = y * s + z * c;
                y = ny;
                z = nz;
            }

            float yaw = renderState.yRot * ((float) Math.PI / 180.0F);
            if (yaw != 0.0F) {
                float c = Mth.cos(yaw);
                float s = Mth.sin(yaw);
                float nx = x * c + z * s;
                float nz = -x * s + z * c;
                x = nx;
                z = nz;
            }

            poseStack.pushPose();
            poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
            ArmorOffsetContext.POSE_PUSHED.set(Boolean.TRUE);
            ArmorOffsetContext.APPLIED.set(Boolean.TRUE);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(v[0] / 16.0F, v[1] / 16.0F, v[2] / 16.0F);
        ArmorOffsetContext.POSE_PUSHED.set(Boolean.TRUE);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void consoleskins$popArmorOffsets(PoseStack poseStack, SubmitNodeCollector nodeCollector, ItemStack item, EquipmentSlot slot, int packedLight, HumanoidRenderState renderState, CallbackInfo ci) {
        if (poseStack != null) {
            try {
                if (Boolean.TRUE.equals(ArmorOffsetContext.POSE_PUSHED.get())) poseStack.popPose();
            } catch (Throwable ignored) {
            }
        }

        try {
            Object parentModel = ArmorOffsetContext.PARENT_MODEL.get();
            boolean[] st = ArmorOffsetContext.PARENT_VIS.get();
            if (parentModel instanceof HumanoidModel hm && st != null && st.length >= 14) {
                ModelPart head = hm.head;
                ModelPart hat = hm.hat;
                ModelPart body = hm.body;
                ModelPart ra = hm.rightArm;
                ModelPart la = hm.leftArm;
                ModelPart rl = hm.rightLeg;
                ModelPart ll = hm.leftLeg;

                ((ModelPartSkipDrawAccessorMixin) (Object) head).consoleskins$setSkipDraw(st[1]);
                ((ModelPartSkipDrawAccessorMixin) (Object) hat).consoleskins$setSkipDraw(st[3]);
                ((ModelPartSkipDrawAccessorMixin) (Object) body).consoleskins$setSkipDraw(st[5]);
                ((ModelPartSkipDrawAccessorMixin) (Object) ra).consoleskins$setSkipDraw(st[7]);
                ((ModelPartSkipDrawAccessorMixin) (Object) la).consoleskins$setSkipDraw(st[9]);
                ((ModelPartSkipDrawAccessorMixin) (Object) rl).consoleskins$setSkipDraw(st[11]);
                ((ModelPartSkipDrawAccessorMixin) (Object) ll).consoleskins$setSkipDraw(st[13]);

                head.visible = st[0];
                hat.visible  = st[2];
                body.visible = st[4];
                ra.visible   = st[6];
                la.visible   = st[8];
                rl.visible   = st[10];
                ll.visible   = st[12];
            }
        } catch (Throwable ignored) {
        }
        consoleskins$clearContext();
    }

    @Redirect(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;visible:Z"),
            require = 0
    )
    private boolean consoleskins$dontSkipArmorWhenParentHidden(ModelPart part) {
        if (Boolean.TRUE.equals(ArmorOffsetContext.FORCE_ARMOR_VISIBLE.get())) return true;
        return part.visible;
    }

    @Redirect(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;skipDraw:Z"),
            require = 0
    )
    private boolean consoleskins$dontSkipArmorWhenParentSkipDraw(ModelPart part) {
        if (Boolean.TRUE.equals(ArmorOffsetContext.FORCE_ARMOR_VISIBLE.get())) return false;
        try {
            return ((ModelPartSkipDrawAccessorMixin) (Object) part).consoleskins$getSkipDraw();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Redirect(
            method = "render",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;skipDraw:Z"),
            require = 0
    )
    private boolean consoleskins$dontSkipArmorWhenParentSkipDraw_render(ModelPart part) {
        return false;
    }

    @Redirect(
            method = "render",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;visible:Z"),
            require = 0
    )
    private boolean consoleskins$dontSkipArmorWhenParentHidden_render(ModelPart part) {
        return true;
    }
}
