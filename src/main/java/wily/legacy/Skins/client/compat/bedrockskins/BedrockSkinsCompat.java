package wily.legacy.Skins.client.compat.bedrockskins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import wily.legacy.Skins.SkinsClientBootstrap;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.client.screen.LegacyScreen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class BedrockSkinsCompat {
    private static final String PREFIX = "bedrockskins$";
    private static final String REMOTE_PACK_ID = "skinpack.Remote";
    private static final String BEDROCK_LEGACY_SCREEN_CLASS = "com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen";
    private static final String BEDROCK_PREVIEW_PLAYER_CLASS = "com.brandonitaly.bedrockskins.client.gui.PreviewPlayer";
    private static final ResourceLocation FALLBACK_PACK_ICON = ResourceLocation.fromNamespaceAndPath("bedrockskins", "icon.png");
    private static final int PREVIEW_BASE_W = 106;
    private static final int PREVIEW_BASE_H = 150;

    private static volatile boolean resolvedCore;
    private static volatile boolean coreAvailable;
    private static volatile boolean checkedPresence;
    private static volatile boolean present;

    private static Class<?> skinManagerClass;
    private static Class<?> guiSkinUtilsClass;
    private static Class<?> skinPackLoaderClass;
    private static Class<?> loadedSkinClass;
    private static Class<?> skinIdClass;
    private static Class<?> assetSourceResourceClass;

    private static Method skinManagerLoadMethod;
    private static Method skinManagerGetLocalSelectedKeyMethod;
    private static Method guiApplySelectedSkinMethod;
    private static Method guiResetSelectedSkinMethod;
    private static Method guiGetSkinDisplayNameTextMethod;
    private static Method guiGetPackDisplayNameMethod;
    private static Method skinPackLoaderLoadPacksMethod;
    private static Method loadedSkinGetIdMethod;
    private static Method loadedSkinGetSkinIdMethod;
    private static Method loadedSkinGetTextureMethod;
    private static Method assetSourceResourceGetIdMethod;

    private static Field skinPackLoaderLoadedSkinsField;
    private static Field skinPackLoaderPackTypesField;
    private static Field skinPackLoaderPackOrderField;

    private static volatile boolean selectionStatePrimed;

    private static volatile boolean resolvedPreview;
    private static volatile boolean previewAvailable;

    private static Class<?> previewWidgetClass;
    private static Class<?> previewPoseClass;
    private static Class<?> previewPlayerClass;

    private static Constructor<?> previewWidgetCtor;
    private static Method previewWidgetVisibleMethod;
    private static Method previewWidgetCleanupMethod;
    private static Method previewWidgetGetPreviewPoseMethod;
    private static Method previewWidgetSetPreviewPoseMethod;
    private static Method previewWidgetSetPendingPoseMethod;
    private static Method previewWidgetIsInterpolatingMethod;
    private static Method previewWidgetSetupDummyPlayerSkinMethod;
    private static Method previewWidgetAdvancePreviewSimulationMethod;
    private static Method previewWidgetApplyLegacyWalkAnimationMethod;
    private static Method previewWidgetApplySwingPoseMethod;
    private static Method previewPlayerTickMethod;
    private static Field previewWidgetRotationXField;
    private static Field previewWidgetRotationYField;
    private static Field previewWidgetSkinSetupCompleteField;
    private static Field previewWidgetDummyPlayerField;
    private static Field previewWidgetWalkSyncEpochField;
    private static Field previewPlayerHandSwingingField;
    private static Field previewPlayerHandSwingTicksField;
    private static Field previewPlayerSwingingArmField;
    private static Field previewPlayerAttackAnimField;
    private static Method previewPlayerSetShiftKeyDownMethod;
    private static Method guiUtilsRenderEntityInRectMethod;
    private static Object previewPoseStanding;
    private static Object previewPoseSneaking;
    private static Object previewPosePunching;

    private static final Map<String, Object> SKIN_BY_ID = new ConcurrentHashMap<>();
    private static final Map<PlayerSkinWidget, PreviewWidgetHolder> PREVIEW_WIDGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<PreviewRenderContext> ACTIVE_PREVIEW_RENDER = new ThreadLocal<>();

    private BedrockSkinsCompat() {
    }

    public static boolean isAvailable() {
        return resolveCore();
    }

    public static boolean isPresent() {
        if (checkedPresence) return present;
        synchronized (BedrockSkinsCompat.class) {
            if (checkedPresence) return present;
            checkedPresence = true;
            try {
                Class.forName("com.brandonitaly.bedrockskins.client.SkinManager");
                present = true;
            } catch (Throwable ignored) {
                present = false;
            }
            return present;
        }
    }

    public static boolean isBedrockSkinId(String skinId) {
        return skinId != null && skinId.startsWith(PREFIX);
    }

    public static boolean isBedrockPreviewPlayer(Object entity) {
        if (entity == null) return false;
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            if (BEDROCK_PREVIEW_PLAYER_CLASS.equals(type.getName())) return true;
        }
        return false;
    }

    public static void clearRegisteredSkins() {
        SKIN_BY_ID.clear();
        cleanupPreviewWidgets();
    }

    public static void resetPreviewCache() {
        cleanupPreviewWidgets();
    }

    public static List<PackDescriptor> loadPackDescriptors() {
        if (!resolveCore()) return List.of();

        clearRegisteredSkins();

        try {
            skinPackLoaderLoadPacksMethod.invoke(null);
        } catch (Throwable t) {
            DebugLog.warn("BedrockSkinsCompat failed to load packs: {}", t.toString());
            return List.of();
        }

        primeSelectionState();

        LinkedHashMap<String, Object> loadedSkinSnapshot = snapshotLoadedSkins();
        if (loadedSkinSnapshot.isEmpty()) {
            importCurrentSelectionIfAbsent();
            return List.of();
        }

        LinkedHashMap<String, List<Object>> skinsByPack = new LinkedHashMap<>();
        for (Object loadedSkin : loadedSkinSnapshot.values()) {
            if (loadedSkin == null) continue;

            String packId = loadedSkinPackId(loadedSkin);
            String actualKey = loadedSkinActualKey(loadedSkin);
            if (packId == null || packId.isBlank() || actualKey == null || actualKey.isBlank()) continue;
            if (REMOTE_PACK_ID.equals(packId) || actualKey.startsWith("Remote:")) continue;

            String syntheticId = syntheticSkinId(actualKey);
            SKIN_BY_ID.put(syntheticId, loadedSkin);
            skinsByPack.computeIfAbsent(packId, k -> new ArrayList<>()).add(loadedSkin);
        }

        reconcileCurrentSelection(loadedSkinSnapshot);

        if (skinsByPack.isEmpty()) {
            importCurrentSelectionIfAbsent();
            return List.of();
        }

        Map<String, String> packTypes = snapshotStringMap(skinPackLoaderPackTypesField);
        List<String> packOrder = snapshotStringList(skinPackLoaderPackOrderField);
        HashMap<String, Integer> packOrderIndex = new HashMap<>();
        for (int i = 0; i < packOrder.size(); i++) {
            String packId = packOrder.get(i);
            if (packId != null && !packId.isBlank()) packOrderIndex.putIfAbsent(packId, i);
        }

        LinkedHashSet<String> orderedPackIds = new LinkedHashSet<>();
        for (String packId : packOrder) {
            if (skinsByPack.containsKey(packId)) orderedPackIds.add(packId);
        }
        orderedPackIds.addAll(skinsByPack.keySet());

        ArrayList<PackDescriptor> descriptors = new ArrayList<>(orderedPackIds.size());
        for (String packId : orderedPackIds) {
            List<Object> packSkins = skinsByPack.get(packId);
            if (packSkins == null || packSkins.isEmpty()) continue;

            Object firstSkin = packSkins.get(0);
            ArrayList<SkinDescriptor> skinDescriptors = new ArrayList<>(packSkins.size());
            for (Object loadedSkin : packSkins) {
                String actualKey = loadedSkinActualKey(loadedSkin);
                if (actualKey == null || actualKey.isBlank()) continue;
                String syntheticId = syntheticSkinId(actualKey);
                String skinName = loadedSkinDisplayName(loadedSkin);
                skinDescriptors.add(new SkinDescriptor(syntheticId, skinName));
            }
            if (skinDescriptors.isEmpty()) continue;

            String packName = packDisplayName(packId, firstSkin);
            String packType = packTypes.getOrDefault(packId, "");
            ResourceLocation icon = resolvePackIcon(firstSkin);
            Integer sortIndex = packOrderIndex.containsKey(packId) ? packOrderIndex.get(packId) : null;
            descriptors.add(new PackDescriptor(packId, packName, packType, icon, skinDescriptors, sortIndex));
        }

        importCurrentSelectionIfAbsent();
        return descriptors;
    }

    public static boolean canRenderPreview(String skinId) {
        return skinId != null && SKIN_BY_ID.containsKey(skinId) && resolvePreview();
    }

    public static boolean isPreviewSupported() {
        return resolvePreview();
    }

    public static boolean isPreviewPoseSupported() {
        return resolvePreview()
                && previewWidgetSetPreviewPoseMethod != null
                && previewPoseStanding != null
                && previewPoseSneaking != null
                && previewPosePunching != null;
    }

    public static boolean applySelectedSkin(String skinId) {
        if (!resolveCore()) return false;
        Object loadedSkin = SKIN_BY_ID.get(skinId);
        if (loadedSkin == null) return false;

        try {
            guiApplySelectedSkinMethod.invoke(null, Minecraft.getInstance(), loadedSkin);
            return true;
        } catch (Throwable t) {
            DebugLog.warn("BedrockSkinsCompat failed to apply {}: {}", skinId, t.toString());
            return false;
        }
    }

    public static void clearSelectedSkin() {
        if (!resolveCore()) return;
        if (!hasSelectedSkin()) return;
        try {
            guiResetSelectedSkinMethod.invoke(null, Minecraft.getInstance());
        } catch (Throwable t) {
            DebugLog.warn("BedrockSkinsCompat failed to clear selection: {}", t.toString());
        }
    }

    public static boolean hasSelectedSkin() {
        String actualKey = getCurrentSelectedActualKey();
        return actualKey != null && !actualKey.isBlank();
    }

    public static String getCurrentSelectedSkinId() {
        String actualKey = getCurrentSelectedActualKey();
        if (actualKey == null || actualKey.isBlank()) return null;
        return syntheticSkinId(actualKey);
    }

    public static boolean canResolveSelectedSkinId(String skinId) {
        if (skinId == null || skinId.isBlank() || !isBedrockSkinId(skinId)) return false;
        if (SKIN_BY_ID.containsKey(skinId)) return true;
        String selectedId = getCurrentSelectedSkinId();
        return skinId.equals(selectedId);
    }

    public static void importCurrentSelectionIfAbsent() {
        String currentId = getCurrentSelectedSkinId();
        if (currentId == null || currentId.isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        UUID self = mc.player != null ? mc.player.getUUID() : (mc.getUser() != null ? mc.getUser().getProfileId() : null);
        if (self == null) return;

        String existing = ClientSkinCache.get(self);
        if (currentId.equals(existing)) return;

        ClientSkinCache.set(self, currentId);
    }

    public static boolean renderPreview(PlayerSkinWidget owner,
                                        GuiGraphics guiGraphics,
                                        String skinId,
                                        float rotationX,
                                        float rotationY,
                                        boolean crouching,
                                        boolean punchLoop,
                                        float partialTick,
                                        int left,
                                        int top,
                                        int right,
                                        int bottom) {
        if (owner == null || guiGraphics == null || !resolvePreview()) return false;

        Object loadedSkin = SKIN_BY_ID.get(skinId);
        if (loadedSkin == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        EntityModelSet entityModels = mc.getEntityModels();
        if (entityModels == null) return false;

        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);

        try {
            PreviewWidgetHolder holder = PREVIEW_WIDGETS.get(owner);
            if (holder == null || holder.widget() == null || holder.entityModelsIdentity() != entityModels) {
                cleanupPreviewWidget(holder);
                holder = createPreviewWidgetHolder(entityModels);
                if (holder == null) return false;
                PREVIEW_WIDGETS.put(owner, holder);
            }

            LoadedSkinSupplier supplier = holder.supplier();
            if (supplier.skin != loadedSkin) {
                supplier.skin = loadedSkin;
                previewWidgetSkinSetupCompleteField.setBoolean(holder.widget(), false);
            }

            AbstractWidget widget = (AbstractWidget) holder.widget();
            widget.active = true;
            widget.visible = true;
            widget.setAlpha(1.0F);
            widget.setX(left);
            widget.setY(top);
            widget.setWidth(width);
            widget.setHeight(height);

            previewWidgetRotationXField.setFloat(holder.widget(), rotationX);
            previewWidgetRotationYField.setFloat(holder.widget(), rotationY);
            syncPreviewPose(holder.widget(), punchLoop ? previewPosePunching : (crouching ? previewPoseSneaking : previewPoseStanding));
            previewWidgetVisibleMethod.invoke(holder.widget());
            Object previewPlayer = resolvePreviewDummyPlayer(holder.widget());
            beginPreviewRenderState(previewPlayer, skinId, crouching, punchLoop);
            try {
                widget.render(guiGraphics, 0, 0, partialTick);
                return true;
            } catch (Throwable widgetRenderFailure) {
                return renderPreviewDummy(holder.widget(), guiGraphics, skinId, rotationY, crouching, punchLoop, left, top, right, bottom);
            } finally {
                endPreviewRenderState(previewPlayer);
            }
        } catch (Throwable t) {
            PreviewWidgetHolder holder = PREVIEW_WIDGETS.remove(owner);
            cleanupPreviewWidget(holder);
            return false;
        }
    }

    private static boolean renderPreviewDummy(Object widget,
                                             GuiGraphics guiGraphics,
                                             String skinId,
                                             float rotationY,
                                             boolean crouching,
                                             boolean punchLoop,
                                             int left,
                                             int top,
                                             int right,
                                             int bottom) throws ReflectiveOperationException {
        if (widget == null || guiGraphics == null) return false;
        if (previewWidgetDummyPlayerField == null
                || previewWidgetSkinSetupCompleteField == null
                || previewWidgetSetupDummyPlayerSkinMethod == null
                || previewWidgetSetPreviewPoseMethod == null
                || guiUtilsRenderEntityInRectMethod == null) {
            return false;
        }

        if (!previewWidgetSkinSetupCompleteField.getBoolean(widget)) {
            previewWidgetSetupDummyPlayerSkinMethod.invoke(widget);
            previewWidgetSkinSetupCompleteField.setBoolean(widget, true);
        }

        Object previewPlayer = previewWidgetDummyPlayerField.get(widget);
        if (!(previewPlayer instanceof LivingEntity dummyPlayer)) return false;

        Object desiredPose = punchLoop ? previewPosePunching : (crouching ? previewPoseSneaking : previewPoseStanding);
        syncPreviewPose(widget, desiredPose);
        advancePreviewSimulation(widget, previewPlayer);
        dummyPlayer.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        setPreviewPlayerShiftKey(dummyPlayer, crouching);
        applyPreviewWalkAnimation(widget, previewPlayer);
        if (punchLoop) applyPreviewSwingPose(widget, previewPlayer);
        else clearPreviewSwingPose(previewPlayer);

        float crouchOffset = crouching ? 5.0F : 0.0F;
        beginPreviewRenderState(previewPlayer, skinId, crouching, punchLoop);
        try {
            guiUtilsRenderEntityInRectMethod.invoke(null, guiGraphics, dummyPlayer, rotationY, crouchOffset, left, top, right, bottom, 110);
        } finally {
            endPreviewRenderState(previewPlayer);
        }
        return true;
    }

    private static Object resolvePreviewDummyPlayer(Object widget) {
        if (widget == null || previewWidgetDummyPlayerField == null) return null;
        try {
            return previewWidgetDummyPlayerField.get(widget);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void syncPreviewPose(Object widget, Object desiredPose) throws ReflectiveOperationException {
        if (widget == null || previewWidgetSetPreviewPoseMethod == null) return;
        Object resolvedPose = desiredPose != null ? desiredPose : previewPoseStanding;
        try {
            if (previewWidgetSetPendingPoseMethod != null) previewWidgetSetPendingPoseMethod.invoke(widget, new Object[]{null});
        } catch (Throwable ignored) {
        }
        Object currentPose = null;
        try {
            if (previewWidgetGetPreviewPoseMethod != null) currentPose = previewWidgetGetPreviewPoseMethod.invoke(widget);
        } catch (Throwable ignored) {
        }
        if (currentPose == resolvedPose) return;
        previewWidgetSetPreviewPoseMethod.invoke(widget, resolvedPose);
    }

    private static void beginPreviewRenderState(Object previewPlayer, String skinId, boolean crouching, boolean punchLoop) {
        if (previewPlayer == null) return;
        ACTIVE_PREVIEW_RENDER.set(new PreviewRenderContext(previewPlayer, skinId, crouching, punchLoop));
    }

    private static void endPreviewRenderState(Object previewPlayer) {
        PreviewRenderContext context = ACTIVE_PREVIEW_RENDER.get();
        if (context == null || context.previewPlayer() != previewPlayer) return;
        ACTIVE_PREVIEW_RENDER.remove();
    }

    public static boolean applyPreviewRenderState(Object avatar, AvatarRenderState state) {
        PreviewRenderContext context = ACTIVE_PREVIEW_RENDER.get();
        if (context == null || context.previewPlayer() != avatar || state == null) return false;

        state.pose = context.crouching() ? Pose.CROUCHING : Pose.STANDING;
        state.isCrouching = context.crouching();
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = context.punchLoop() ? previewAttackTime() : 0.0F;

        if (state instanceof RenderStateSkinIdAccess access) {
            access.consoleskins$setSkinId(context.skinId());
            if (avatar instanceof LivingEntity livingEntity) {
                try {
                    access.consoleskins$setEntityUuid(livingEntity.getUUID());
                } catch (Throwable ignored) {
                }
            }
            access.consoleskins$setMoving(false);
            access.consoleskins$setMoveSpeedSq(0.0F);
            access.consoleskins$setSitting(false);
            access.consoleskins$setUsingItem(false);
            access.consoleskins$setBlocking(false);
        }
        return true;
    }

    private static void setPreviewPlayerShiftKey(LivingEntity previewPlayer, boolean crouching) {
        if (previewPlayer == null || previewPlayerSetShiftKeyDownMethod == null) return;
        try {
            previewPlayerSetShiftKeyDownMethod.invoke(previewPlayer, crouching);
        } catch (Throwable ignored) {
        }
    }

    private static void advancePreviewSimulation(Object widget, Object previewPlayer) {
        if (widget != null && previewWidgetAdvancePreviewSimulationMethod != null) {
            try {
                previewWidgetAdvancePreviewSimulationMethod.invoke(widget, previewPlayer);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (previewPlayer == null || previewPlayerTickMethod == null) return;
        try {
            previewPlayerTickMethod.invoke(previewPlayer);
        } catch (Throwable ignored) {
        }
    }

    private static void applyPreviewWalkAnimation(Object widget, Object previewPlayer) {
        if (widget == null || previewPlayer == null || previewWidgetApplyLegacyWalkAnimationMethod == null) return;
        try {
            previewWidgetApplyLegacyWalkAnimationMethod.invoke(widget, previewPlayer);
        } catch (Throwable ignored) {
        }
    }

    private static void applyPreviewSwingPose(Object widget, Object previewPlayer) {
        if (widget != null && previewPlayer != null && previewWidgetApplySwingPoseMethod != null) {
            try {
                previewWidgetApplySwingPoseMethod.invoke(widget, previewPlayer);
                return;
            } catch (Throwable ignored) {
            }
        }
        applyPreviewSwingPoseDirect(previewPlayer);
    }

    private static void applyPreviewSwingPoseDirect(Object previewPlayer) {
        if (previewPlayer == null
                || previewPlayerHandSwingingField == null
                || previewPlayerHandSwingTicksField == null
                || previewPlayerSwingingArmField == null
                || previewPlayerAttackAnimField == null) {
            return;
        }

        float swingTicks = 6.0F;
        long swingWindowMs = (long) (swingTicks * 50.0F);
        long epochMs = resolvePreviewWalkSyncEpochMs();
        long phaseMs = Math.floorMod(System.currentTimeMillis() - epochMs, swingWindowMs);
        float progress = phaseMs / (float) swingWindowMs;

        try {
            previewPlayerHandSwingingField.setBoolean(previewPlayer, true);
            previewPlayerHandSwingTicksField.setInt(previewPlayer, Math.max(0, Math.min(5, (int) (progress * swingTicks))));
            previewPlayerSwingingArmField.set(previewPlayer, InteractionHand.MAIN_HAND);
            previewPlayerAttackAnimField.setFloat(previewPlayer, progress);
        } catch (Throwable ignored) {
        }
    }

    private static void clearPreviewSwingPose(Object previewPlayer) {
        if (previewPlayer == null) return;
        try {
            if (previewPlayerHandSwingingField != null) previewPlayerHandSwingingField.setBoolean(previewPlayer, false);
        } catch (Throwable ignored) {
        }
        try {
            if (previewPlayerHandSwingTicksField != null) previewPlayerHandSwingTicksField.setInt(previewPlayer, 0);
        } catch (Throwable ignored) {
        }
        try {
            if (previewPlayerSwingingArmField != null) previewPlayerSwingingArmField.set(previewPlayer, InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {
        }
        try {
            if (previewPlayerAttackAnimField != null) previewPlayerAttackAnimField.setFloat(previewPlayer, 0.0F);
        } catch (Throwable ignored) {
        }
    }

    private static long resolvePreviewWalkSyncEpochMs() {
        if (previewWidgetWalkSyncEpochField == null) return 0L;
        try {
            return previewWidgetWalkSyncEpochField.getLong(null);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static float previewAttackTime() {
        long swingMs = 300L;
        long epochMs = resolvePreviewWalkSyncEpochMs();
        long now = System.currentTimeMillis();
        long phase = epochMs > 0L ? (now - epochMs) % swingMs : now % swingMs;
        if (phase < 0L) phase += swingMs;
        return phase / (float) swingMs;
    }

    public static void redirectLegacyScreenIfNeeded(Minecraft minecraft) {
        if (minecraft == null) return;
        Screen screen = minecraft.screen;
        if (screen == null) return;
        if (!BEDROCK_LEGACY_SCREEN_CLASS.equals(screen.getClass().getName())) return;

        Screen parent = screen instanceof LegacyScreen legacyScreen ? legacyScreen.parent : null;
        try {
            minecraft.setScreen(SkinsClientBootstrap.createChangeSkinScreen(parent));
        } catch (Throwable ignored) {
        }
    }

    private static String getCurrentSelectedActualKey() {
        if (!resolveCore()) return null;
        primeSelectionState();
        try {
            Object selected = skinManagerGetLocalSelectedKeyMethod.invoke(null);
            return selected == null ? null : selected.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void primeSelectionState() {
        if (!resolveCore() || selectionStatePrimed) return;
        selectionStatePrimed = true;
        try {
            if (skinManagerLoadMethod != null) skinManagerLoadMethod.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private static String syntheticSkinId(String actualKey) {
        return actualKey == null || actualKey.isBlank() ? null : PREFIX + actualKey;
    }

    private static void reconcileCurrentSelection(Map<String, Object> loadedSkinSnapshot) {
        String actualKey = getCurrentSelectedActualKey();
        if (actualKey == null || actualKey.isBlank()) return;

        Object loadedSkin = loadedSkinSnapshot.get(actualKey);
        if (loadedSkin == null) {
            DebugLog.warn("BedrockSkinsCompat selected skin {} is active but missing from loaded descriptors", actualKey);
            return;
        }

        String syntheticId = syntheticSkinId(actualKey);
        if (syntheticId == null || syntheticId.isBlank()) return;
        SKIN_BY_ID.putIfAbsent(syntheticId, loadedSkin);
    }

    private static LinkedHashMap<String, Object> snapshotLoadedSkins() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        try {
            Object value = skinPackLoaderLoadedSkinsField.get(null);
            if (!(value instanceof Map<?, ?> map)) return out;
            synchronized (map) {
                for (Object entryObj : map.entrySet()) {
                    if (!(entryObj instanceof Map.Entry<?, ?> entry)) continue;
                    Object loadedSkin = entry.getValue();
                    String actualKey = loadedSkinActualKey(loadedSkin);
                    if (actualKey == null || actualKey.isBlank()) continue;
                    out.put(actualKey, loadedSkin);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static Map<String, String> snapshotStringMap(Field field) {
        HashMap<String, String> out = new HashMap<>();
        if (field == null) return out;
        try {
            Object value = field.get(null);
            if (!(value instanceof Map<?, ?> map)) return out;
            synchronized (map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = stringValue(entry.getKey());
                    String val = stringValue(entry.getValue());
                    if (key != null && val != null) out.put(key, val);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static List<String> snapshotStringList(Field field) {
        if (field == null) return List.of();
        try {
            Object value = field.get(null);
            if (!(value instanceof List<?> list) || list.isEmpty()) return List.of();
            ArrayList<String> out = new ArrayList<>(list.size());
            for (Object element : list) {
                String valueStr = stringValue(element);
                if (valueStr != null && !valueStr.isBlank()) out.add(valueStr);
            }
            return out;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static String loadedSkinPackId(Object loadedSkin) {
        if (loadedSkin == null) return null;
        try {
            return stringValue(loadedSkinGetIdMethod.invoke(loadedSkin));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String loadedSkinActualKey(Object loadedSkin) {
        if (loadedSkin == null) return null;
        try {
            Object skinId = loadedSkinGetSkinIdMethod.invoke(loadedSkin);
            return skinId == null ? null : skinId.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String loadedSkinDisplayName(Object loadedSkin) {
        if (loadedSkin == null) return "";
        try {
            String name = stringValue(guiGetSkinDisplayNameTextMethod.invoke(null, loadedSkin));
            if (name != null && !name.isBlank()) return name;
        } catch (Throwable ignored) {
        }
        String actualKey = loadedSkinActualKey(loadedSkin);
        return actualKey == null ? "" : actualKey;
    }

    private static String packDisplayName(String packId, Object firstSkin) {
        try {
            String name = stringValue(guiGetPackDisplayNameMethod.invoke(null, packId, firstSkin));
            if (name != null && !name.isBlank()) return name;
        } catch (Throwable ignored) {
        }
        return packId == null ? "" : packId;
    }

    private static ResourceLocation resolvePackIcon(Object firstSkin) {
        if (firstSkin == null) return FALLBACK_PACK_ICON;
        try {
            Object textureSource = loadedSkinGetTextureMethod.invoke(firstSkin);
            if (textureSource != null && assetSourceResourceClass.isInstance(textureSource)) {
                Object id = assetSourceResourceGetIdMethod.invoke(textureSource);
                if (id instanceof ResourceLocation textureId) {
                    String path = textureId.getPath();
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash > 0) {
                        ResourceLocation packIcon = ResourceLocation.fromNamespaceAndPath(textureId.getNamespace(), path.substring(0, lastSlash) + "/pack_icon.png");
                        Minecraft mc = Minecraft.getInstance();
                        if (mc == null || mc.getResourceManager() == null || mc.getResourceManager().getResource(packIcon).isPresent()) {
                            return packIcon;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return FALLBACK_PACK_ICON;
    }

    private static void cleanupPreviewWidgets() {
        synchronized (PREVIEW_WIDGETS) {
            for (PreviewWidgetHolder holder : PREVIEW_WIDGETS.values()) {
                cleanupPreviewWidget(holder);
            }
            PREVIEW_WIDGETS.clear();
        }
    }

    private static void cleanupPreviewWidget(PreviewWidgetHolder holder) {
        if (holder == null || holder.widget() == null || !resolvePreview()) return;
        try {
            previewWidgetCleanupMethod.invoke(holder.widget());
        } catch (Throwable ignored) {
        }
    }

    private static PreviewWidgetHolder createPreviewWidgetHolder(EntityModelSet entityModels) {
        if (!resolvePreview()) return null;
        try {
            LoadedSkinSupplier supplier = new LoadedSkinSupplier();
            Object widget = previewWidgetCtor.newInstance(PREVIEW_BASE_W, PREVIEW_BASE_H, entityModels, null, supplier);
            if (!(widget instanceof AbstractWidget abstractWidget)) return null;
            abstractWidget.active = true;
            abstractWidget.visible = true;
            abstractWidget.setAlpha(1.0F);
            abstractWidget.setWidth(PREVIEW_BASE_W);
            abstractWidget.setHeight(PREVIEW_BASE_H);
            previewWidgetVisibleMethod.invoke(widget);
            previewWidgetSetPreviewPoseMethod.invoke(widget, previewPoseStanding);
            return new PreviewWidgetHolder(widget, supplier, entityModels);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean resolveCore() {
        if (resolvedCore) return coreAvailable;
        synchronized (BedrockSkinsCompat.class) {
            if (resolvedCore) return coreAvailable;
            resolvedCore = true;
            try {
                skinManagerClass = Class.forName("com.brandonitaly.bedrockskins.client.SkinManager");
                guiSkinUtilsClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.GuiSkinUtils");
                skinPackLoaderClass = Class.forName("com.brandonitaly.bedrockskins.pack.SkinPackLoader");
                loadedSkinClass = Class.forName("com.brandonitaly.bedrockskins.pack.LoadedSkin");
                skinIdClass = Class.forName("com.brandonitaly.bedrockskins.pack.SkinId");
                assetSourceResourceClass = Class.forName("com.brandonitaly.bedrockskins.pack.AssetSource$Resource");

                skinManagerLoadMethod = skinManagerClass.getMethod("load");
                skinManagerGetLocalSelectedKeyMethod = skinManagerClass.getMethod("getLocalSelectedKey");
                guiApplySelectedSkinMethod = guiSkinUtilsClass.getMethod("applySelectedSkin", Minecraft.class, loadedSkinClass);
                guiResetSelectedSkinMethod = guiSkinUtilsClass.getMethod("resetSelectedSkin", Minecraft.class);
                guiGetSkinDisplayNameTextMethod = guiSkinUtilsClass.getMethod("getSkinDisplayNameText", loadedSkinClass);
                guiGetPackDisplayNameMethod = guiSkinUtilsClass.getMethod("getPackDisplayName", String.class, loadedSkinClass);
                skinPackLoaderLoadPacksMethod = skinPackLoaderClass.getMethod("loadPacks");
                loadedSkinGetIdMethod = loadedSkinClass.getMethod("getId");
                loadedSkinGetSkinIdMethod = loadedSkinClass.getMethod("getSkinId");
                loadedSkinGetTextureMethod = loadedSkinClass.getMethod("getTexture");
                assetSourceResourceGetIdMethod = assetSourceResourceClass.getMethod("getId");

                skinPackLoaderLoadedSkinsField = skinPackLoaderClass.getField("loadedSkins");
                skinPackLoaderPackTypesField = skinPackLoaderClass.getField("packTypesByPackId");
                skinPackLoaderPackOrderField = skinPackLoaderClass.getField("packOrder");

                coreAvailable = true;
            } catch (Throwable ignored) {
                coreAvailable = false;
            }
            return coreAvailable;
        }
    }

    private static boolean resolvePreview() {
        if (resolvedPreview) return previewAvailable;
        synchronized (BedrockSkinsCompat.class) {
            if (resolvedPreview) return previewAvailable;
            resolvedPreview = true;
            if (!resolveCore()) {
                previewAvailable = false;
                return false;
            }
            try {
                previewWidgetClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.PlayerSkinWidget");
                previewPoseClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.PlayerSkinWidget$PreviewPose");
                previewPlayerClass = Class.forName(BEDROCK_PREVIEW_PLAYER_CLASS);
                Class<?> guiUtilsClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.GuiUtils");

                previewWidgetCtor = previewWidgetClass.getConstructor(int.class, int.class, EntityModelSet.class, Supplier.class, Supplier.class);
                previewWidgetVisibleMethod = previewWidgetClass.getMethod("visible");
                previewWidgetCleanupMethod = previewWidgetClass.getMethod("cleanup");
                previewWidgetGetPreviewPoseMethod = findMethod(previewWidgetClass, "getPreviewPose");
                previewWidgetSetPreviewPoseMethod = previewWidgetClass.getMethod("setPreviewPose", previewPoseClass);
                previewWidgetSetPendingPoseMethod = findMethod(previewWidgetClass, "setPendingPose", previewPoseClass);
                previewWidgetIsInterpolatingMethod = findMethod(previewWidgetClass, "isInterpolating");
                previewWidgetSetupDummyPlayerSkinMethod = findDeclaredMethod(previewWidgetClass, "setupDummyPlayerSkin");
                previewWidgetAdvancePreviewSimulationMethod = findDeclaredMethod(previewWidgetClass, "advancePreviewSimulation", previewPlayerClass);
                previewWidgetApplyLegacyWalkAnimationMethod = findDeclaredMethod(previewWidgetClass, "applyLegacyWalkAnimation", previewPlayerClass);
                previewWidgetApplySwingPoseMethod = findDeclaredMethod(previewWidgetClass, "applySwingPose", previewPlayerClass);
                previewPlayerTickMethod = findHierarchyMethod(previewPlayerClass, "tick");
                if (previewPlayerTickMethod == null) previewPlayerTickMethod = findHierarchyMethod(previewPlayerClass, "method_5773");
                previewWidgetRotationXField = previewWidgetClass.getDeclaredField("rotationX");
                previewWidgetRotationXField.setAccessible(true);
                previewWidgetRotationYField = previewWidgetClass.getDeclaredField("rotationY");
                previewWidgetRotationYField.setAccessible(true);
                previewWidgetSkinSetupCompleteField = previewWidgetClass.getDeclaredField("skinSetupComplete");
                previewWidgetSkinSetupCompleteField.setAccessible(true);
                previewWidgetDummyPlayerField = findDeclaredField(previewWidgetClass, "dummyPlayer");
                previewWidgetWalkSyncEpochField = findDeclaredField(previewWidgetClass, "WALK_SYNC_EPOCH_MS");
                previewPlayerHandSwingingField = findHierarchyField(previewPlayerClass, boolean.class, "handSwinging", "swinging", "field_6252");
                previewPlayerHandSwingTicksField = findHierarchyField(previewPlayerClass, int.class, "handSwingTicks", "swingTime", "field_6279");
                previewPlayerSwingingArmField = findHierarchyField(previewPlayerClass, InteractionHand.class, "swingingArm", "handSwingingArm", "field_6266");
                previewPlayerAttackAnimField = findHierarchyField(previewPlayerClass, float.class, "attackAnim", "handSwingProgress", "field_6251");
                previewPlayerSetShiftKeyDownMethod = findHierarchyMethod(previewPlayerClass, "setShiftKeyDown", boolean.class);
                guiUtilsRenderEntityInRectMethod = findMethod(guiUtilsClass, "renderEntityInRect", GuiGraphics.class, LivingEntity.class, float.class, float.class, int.class, int.class, int.class, int.class, int.class);

                previewPoseStanding = enumConstant(previewPoseClass, "STANDING", null);
                previewPoseSneaking = enumConstant(previewPoseClass, "SNEAKING", previewPoseStanding);
                previewPosePunching = enumConstant(previewPoseClass, "PUNCHING", previewPoseStanding);
                previewAvailable = previewPoseStanding != null;
            } catch (Throwable ignored) {
                previewAvailable = false;
            }
            return previewAvailable;
        }
    }

    private static Object enumConstant(Class<?> enumClass, String name, Object fallback) {
        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object obj = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
            return obj;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null || name == null || name.isBlank()) return null;
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null || name == null || name.isBlank()) return null;
        try {
            Method method = owner.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findDeclaredField(Class<?> owner, String name) {
        if (owner == null || name == null || name.isBlank()) return null;
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findHierarchyMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            Method method = findMethod(type, name, parameterTypes);
            if (method != null) return method;
            method = findDeclaredMethod(type, name, parameterTypes);
            if (method != null) return method;
        }
        return null;
    }

    private static Field findHierarchyField(Class<?> owner, Class<?> expectedType, String... names) {
        if (owner == null || names == null || names.length == 0) return null;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField(name);
                    if (expectedType != null && field.getType() != expectedType) continue;
                    field.setAccessible(true);
                    return field;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) return null;
        String out = value.toString();
        return out == null || out.isBlank() ? null : out;
    }

    public record SkinDescriptor(String id, String name) {
    }

    public record PackDescriptor(String id, String name, String type, ResourceLocation icon, List<SkinDescriptor> skins, Integer sortIndex) {
    }

    private static final class LoadedSkinSupplier implements Supplier<Object> {
        private volatile Object skin;

        @Override
        public Object get() {
            return skin;
        }
    }

    private record PreviewWidgetHolder(Object widget, LoadedSkinSupplier supplier, Object entityModelsIdentity) {
    }

    private record PreviewRenderContext(Object previewPlayer, String skinId, boolean crouching, boolean punchLoop) {
    }
}
