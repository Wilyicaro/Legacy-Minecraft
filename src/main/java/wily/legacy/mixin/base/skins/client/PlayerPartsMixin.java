package wily.legacy.mixin.base.skins.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.compat.cpm.CpmRenderCompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.util.DebugLog;

@Mixin(PlayerModel.class)
public abstract class PlayerPartsMixin {

    // Cached reflection for isSpectator - resolved once, reused every frame
    @Unique
    private static volatile boolean consoleskins$triedResolveSpectator;
    @Unique
    private static volatile Field consoleskins$spectatorField;
    @Unique
    private static volatile Method consoleskins$spectatorMethod;

    // Log once to confirm this version of hidePart is running (uses skipDraw only, not skipRenderOverride)


    @Unique
    private boolean consoleskins$specBoxHeadInstalled;
    @Unique
    private String consoleskins$specBoxHeadSkinId;

    @Unique
    private Field consoleskins$specHeadCubesField;
    @Unique
    private Field consoleskins$specHatCubesField;
    @Unique
    private Field consoleskins$specHeadChildrenField;
    @Unique
    private Field consoleskins$specHatChildrenField;

    @Unique
    private Object consoleskins$specHeadCubesBackup;
    @Unique
    private Object consoleskins$specHatCubesBackup;
    @Unique
    private Object consoleskins$specHeadChildrenBackup;
    @Unique
    private Object consoleskins$specHatChildrenBackup;

    private static void consoleskins$resetPart(ModelPart part) {
        if (part == null) return;
        part.visible = true;
        ((SkipDrawAccessor) (Object) part).consoleskins$setSkipDraw(false);
    }

    private static void consoleskins$hidePart(ModelPart part) {
        if (part == null) return;
        DebugLog.debug("[ArmorFix] hidePart: skipDraw=true slot={}", part);
        part.visible = true;
        ((SkipDrawAccessor) (Object) part).consoleskins$setSkipDraw(true);
    }

    private static void consoleskins$setSkipDraw(ModelPart part, boolean skip) {
        if (part == null) return;
        ((SkipDrawAccessor) (Object) part).consoleskins$setSkipDraw(skip);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$hidePartsForBoxModels(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;

        if (CpmRenderCompat.isCpmModelActive(state)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$setSkipDraw(self.head, false);
            consoleskins$setSkipDraw(self.hat, false);
            consoleskins$setSkipDraw(self.body, false);
            consoleskins$setSkipDraw(self.rightArm, false);
            consoleskins$setSkipDraw(self.leftArm, false);
            consoleskins$setSkipDraw(self.rightLeg, false);
            consoleskins$setSkipDraw(self.leftLeg, false);
            consoleskins$setSkipDraw(self.jacket, false);
            consoleskins$setSkipDraw(self.rightSleeve, false);
            consoleskins$setSkipDraw(self.leftSleeve, false);
            consoleskins$setSkipDraw(self.rightPants, false);
            consoleskins$setSkipDraw(self.leftPants, false);
            return;
        }

        UUID uuid = null;
        String skinId = null;
        if (state instanceof RenderStateSkinIdAccess a) {
            try {
                uuid = a.consoleskins$getEntityUuid();
            } catch (Throwable ignored) {
            }
            try {
                skinId = a.consoleskins$getSkinId();
            } catch (Throwable ignored) {
            }
        }

        boolean spectator = consoleskins$isSpectator(state, uuid);

        if (!spectator) {
            consoleskins$uninstallSpectatorBoxHead(self);
        }

        if (spectator) {
            consoleskins$applySpectatorBoxHeadIfNeeded(self, skinId);
            return;
        }

        consoleskins$resetPart(self.head);
        consoleskins$resetPart(self.hat);
        consoleskins$resetPart(self.body);
        consoleskins$resetPart(self.rightArm);
        consoleskins$resetPart(self.leftArm);
        consoleskins$resetPart(self.rightLeg);
        consoleskins$resetPart(self.leftLeg);

        consoleskins$resetPart(self.jacket);
        consoleskins$resetPart(self.rightSleeve);
        consoleskins$resetPart(self.leftSleeve);
        consoleskins$resetPart(self.rightPants);
        consoleskins$resetPart(self.leftPants);

        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        // Use values cached on render state by AvatarSkinMixin
        ResourceLocation tex = (state instanceof RenderStateSkinIdAccess ra) ? ra.consoleskins$getCachedTexture() : null;
        BuiltBoxModel model = (state instanceof RenderStateSkinIdAccess ra2) ? ra2.consoleskins$getCachedBoxModel() : null;

        if (tex == null) {
            SkinEntry entry = SkinPackLoader.getSkin(skinId);
            tex = ClientSkinAssets.getTexture(skinId);
            if (tex == null && entry != null) tex = entry.texture();
        }
        if (tex == null) return;

        if (model == null) {
            ResourceLocation modelId = consoleskins$modelIdFromTexture(tex);
            model = BoxModelManager.get(modelId);
            if (model == null) {
                JsonObject mj = ClientSkinAssets.getModelJson(skinId);
                if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
                model = BoxModelManager.get(modelId);
            }
        }
        if (model == null) return;

        if (model.hides(AttachSlot.HEAD)) {
            consoleskins$hidePart(self.head);
            consoleskins$hidePart(self.hat);
        }
        if (model.hides(AttachSlot.HAT)) consoleskins$hidePart(self.hat);

        if (model.hides(AttachSlot.BODY)) {
            consoleskins$hidePart(self.body);
            consoleskins$hidePart(self.jacket);
        }
        if (model.hides(AttachSlot.JACKET)) consoleskins$hidePart(self.jacket);

        if (model.hides(AttachSlot.RIGHT_ARM)) {
            consoleskins$hidePart(self.rightArm);
            consoleskins$hidePart(self.rightSleeve);
        }
        if (model.hides(AttachSlot.RIGHT_SLEEVE)) consoleskins$hidePart(self.rightSleeve);

        if (model.hides(AttachSlot.LEFT_ARM)) {
            consoleskins$hidePart(self.leftArm);
            consoleskins$hidePart(self.leftSleeve);
        }
        if (model.hides(AttachSlot.LEFT_SLEEVE)) consoleskins$hidePart(self.leftSleeve);

        if (model.hides(AttachSlot.RIGHT_LEG)) {
            consoleskins$hidePart(self.rightLeg);
            consoleskins$hidePart(self.rightPants);
        }
        if (model.hides(AttachSlot.RIGHT_PANTS)) consoleskins$hidePart(self.rightPants);

        if (model.hides(AttachSlot.LEFT_LEG)) {
            consoleskins$hidePart(self.leftLeg);
            consoleskins$hidePart(self.leftPants);
        }
        if (model.hides(AttachSlot.LEFT_PANTS)) consoleskins$hidePart(self.leftPants);
    }

    @Unique
    private void consoleskins$applySpectatorBoxHeadIfNeeded(PlayerModel self, String skinId) {
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation tex = ClientSkinAssets.getTexture(skinId);
        if (tex == null && entry != null) tex = entry.texture();
        if (tex == null) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }

        ResourceLocation modelId = consoleskins$modelIdFromTexture(tex);
        BuiltBoxModel model = BoxModelManager.get(modelId);
        if (model == null) {
            JsonObject mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            model = BoxModelManager.get(modelId);
        }
        if (model == null) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }

        boolean hasHeadParts = model.get(AttachSlot.HEAD) != null && !model.get(AttachSlot.HEAD).isEmpty();
        boolean hasHatParts = model.get(AttachSlot.HAT) != null && !model.get(AttachSlot.HAT).isEmpty();
        boolean wantsHideHead = model.hides(AttachSlot.HEAD);

        if (!(wantsHideHead && (hasHeadParts || hasHatParts))) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }

        if (consoleskins$specBoxHeadInstalled && skinId.equals(consoleskins$specBoxHeadSkinId)) {
            consoleskins$setSkipDraw(self.head, false);
            consoleskins$setSkipDraw(self.hat, false);
            return;
        }

        consoleskins$uninstallSpectatorBoxHead(self);

        consoleskins$setSkipDraw(self.head, false);
        consoleskins$setSkipDraw(self.hat, false);

        consoleskins$specHeadCubesField = consoleskins$findListField(self.head, "cube");
        consoleskins$specHatCubesField = consoleskins$findListField(self.hat, "cube");
        consoleskins$specHeadChildrenField = consoleskins$findMapField(self.head, "child");
        consoleskins$specHatChildrenField = consoleskins$findMapField(self.hat, "child");

        consoleskins$specHeadCubesBackup = consoleskins$getFieldValue(consoleskins$specHeadCubesField, self.head);
        consoleskins$specHatCubesBackup = consoleskins$getFieldValue(consoleskins$specHatCubesField, self.hat);
        consoleskins$specHeadChildrenBackup = consoleskins$getFieldValue(consoleskins$specHeadChildrenField, self.head);
        consoleskins$specHatChildrenBackup = consoleskins$getFieldValue(consoleskins$specHatChildrenField, self.hat);

        if (consoleskins$specHeadCubesField != null) consoleskins$setFieldValue(consoleskins$specHeadCubesField, self.head, List.of());
        if (consoleskins$specHatCubesField != null) consoleskins$setFieldValue(consoleskins$specHatCubesField, self.hat, List.of());

        Map headChildren = consoleskins$copyChildrenMap(consoleskins$specHeadChildrenBackup);
        Map hatChildren = consoleskins$copyChildrenMap(consoleskins$specHatChildrenBackup);

        consoleskins$removeChildrenWithPrefix(headChildren, "consoleskins$spec_head_");
        consoleskins$removeChildrenWithPrefix(hatChildren, "consoleskins$spec_hat_");

        if (hasHeadParts) {
            int i = 0;
            for (Object p : model.get(AttachSlot.HEAD)) {
                if (p instanceof ModelPart mp) headChildren.put("consoleskins$spec_head_" + (i++), mp);
            }
        }

        if (hasHatParts) {
            int i = 0;
            for (Object p : model.get(AttachSlot.HAT)) {
                if (p instanceof ModelPart mp) hatChildren.put("consoleskins$spec_hat_" + (i++), mp);
            }
        }

        if (consoleskins$specHeadChildrenField != null) consoleskins$setFieldValue(consoleskins$specHeadChildrenField, self.head, headChildren);
        if (consoleskins$specHatChildrenField != null) consoleskins$setFieldValue(consoleskins$specHatChildrenField, self.hat, hatChildren);

        consoleskins$specBoxHeadInstalled = true;
        consoleskins$specBoxHeadSkinId = skinId;
    }

    @Unique
    private void consoleskins$uninstallSpectatorBoxHead(PlayerModel self) {
        if (!consoleskins$specBoxHeadInstalled) return;

        if (consoleskins$specHeadCubesField != null) consoleskins$setFieldValue(consoleskins$specHeadCubesField, self.head, consoleskins$specHeadCubesBackup);
        if (consoleskins$specHatCubesField != null) consoleskins$setFieldValue(consoleskins$specHatCubesField, self.hat, consoleskins$specHatCubesBackup);
        if (consoleskins$specHeadChildrenField != null) consoleskins$setFieldValue(consoleskins$specHeadChildrenField, self.head, consoleskins$specHeadChildrenBackup);
        if (consoleskins$specHatChildrenField != null) consoleskins$setFieldValue(consoleskins$specHatChildrenField, self.hat, consoleskins$specHatChildrenBackup);

        consoleskins$specHeadCubesField = null;
        consoleskins$specHatCubesField = null;
        consoleskins$specHeadChildrenField = null;
        consoleskins$specHatChildrenField = null;
        consoleskins$specHeadCubesBackup = null;
        consoleskins$specHatCubesBackup = null;
        consoleskins$specHeadChildrenBackup = null;
        consoleskins$specHatChildrenBackup = null;

        consoleskins$specBoxHeadInstalled = false;
        consoleskins$specBoxHeadSkinId = null;
    }

    private static Object consoleskins$getFieldValue(Field f, Object obj) {
        if (f == null || obj == null) return null;
        try {
            return f.get(obj);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void consoleskins$setFieldValue(Field f, Object obj, Object value) {
        if (f == null || obj == null) return;
        try {
            f.set(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static Field consoleskins$findListField(ModelPart part, String hint) {
        if (part == null) return null;
        try {
            Class<?> c = part.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(f.getType())) continue;
                    String n = f.getName().toLowerCase();
                    if (hint != null && !hint.isBlank() && !n.contains(hint)) continue;
                    f.setAccessible(true);
                    return f;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> c = part.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    return f;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Field consoleskins$findMapField(ModelPart part, String hint) {
        if (part == null) return null;
        try {
            Class<?> c = part.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Map.class.isAssignableFrom(f.getType())) continue;
                    String n = f.getName().toLowerCase();
                    if (hint != null && !hint.isBlank() && !n.contains(hint)) continue;
                    f.setAccessible(true);
                    return f;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> c = part.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Map.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    return f;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map consoleskins$copyChildrenMap(Object mapObj) {
        if (mapObj instanceof Map m) return new HashMap(m);
        return new HashMap();
    }

    private static void consoleskins$removeChildrenWithPrefix(Map map, String prefix) {
        if (map == null || prefix == null) return;
        try {
            map.keySet().removeIf(k -> k != null && k.toString().startsWith(prefix));
        } catch (Throwable ignored) {
        }
    }

    private static ResourceLocation consoleskins$modelIdFromTexture(ResourceLocation texture) {
        return ClientSkinAssets.getModelIdFromTexture(texture);
    }

    private static boolean consoleskins$isSpectator(AvatarRenderState state, UUID uuid) {
        // Use cached reflection instead of per-frame getField/getMethod
        if (state != null) {
            if (!consoleskins$triedResolveSpectator) {
                synchronized (PlayerPartsMixin.class) {
                    if (!consoleskins$triedResolveSpectator) {
                        consoleskins$triedResolveSpectator = true;
                        try {
                            Field f = state.getClass().getField("isSpectator");
                            if (f.getType() == boolean.class) consoleskins$spectatorField = f;
                        } catch (Throwable ignored) {
                            try {
                                Method m = state.getClass().getMethod("isSpectator");
                                if (m.getReturnType() == boolean.class) consoleskins$spectatorMethod = m;
                            } catch (Throwable ignored2) {
                            }
                        }
                    }
                }
            }

            if (consoleskins$spectatorField != null) {
                try { return consoleskins$spectatorField.getBoolean(state); } catch (Throwable ignored) {}
            }
            if (consoleskins$spectatorMethod != null) {
                try { return (boolean) consoleskins$spectatorMethod.invoke(state); } catch (Throwable ignored) {}
            }
        }
        if (uuid != null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player p = mc.level.getPlayerByUUID(uuid);
                    if (p != null) return p.isSpectator();
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}