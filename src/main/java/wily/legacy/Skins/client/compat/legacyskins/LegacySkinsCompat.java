package wily.legacy.Skins.client.compat.legacyskins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.util.DebugLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class LegacySkinsCompat {
    private static final String PREFIX = "legacyskins$";
    private static final ResourceLocation LEGACY_DEFAULT_PACK = ResourceLocation.fromNamespaceAndPath("legacyskins", "default");
    private static final ResourceLocation LEGACY_FAVORITES_PACK = ResourceLocation.fromNamespaceAndPath("legacyskins", "favorites");
    private static final float MODEL_HEIGHT = 2.125F;
    private static final float PIVOT_Y = -1.0625F;
    private static final int EXTEND_BY = 140;
    private static final float PREVIEW_RECT_SCALE = MODEL_HEIGHT / 2.75F;
    private static final int PREVIEW_BASE_W = 106;
    private static final int PREVIEW_BASE_H = 150;

    private static volatile boolean resolvedCore;
    private static volatile boolean coreAvailable;
    private static volatile boolean checkedPresence;
    private static volatile boolean present;

    private static Class<?> legacySkinsClass;
    private static Class<?> legacyConfigClass;
    private static Class<?> legacySkinsConfigClass;
    private static Class<?> skinReferenceClass;
    private static Class<?> legacySkinPackClass;
    private static Class<?> legacySkinClass;

    private static Method lazyInstanceMethod;
    private static Method configSetSkinMethod;
    private static Method configSaveMethod;
    private static Method configGetActiveSkinsConfigMethod;
    private static Method configShowDevPacksMethod;
    private static Method skinsConfigGetCurrentSkinMethod;
    private static Method legacySkinCanBeUsedMethod;
    private static Field legacySkinPackListField;

    private static Constructor<?> skinReferenceCtor;

    private static volatile boolean resolvedPreview;
    private static volatile boolean previewAvailable;

    private static Class<?> guiCpmSkinRenderStateClass;
    private static Class<?> widgetStateClass;
    private static Class<?> widgetSafeWidgetClass;
    private static Class<?> widgetModelClass;

    private static Constructor<?> guiCpmSkinRenderStateCtor;
    private static Constructor<?> widgetSafeWidgetCtor;
    private static Method widgetModelBakeMethod;
    private static Method widgetModelSetupAnimMethod;
    private static Method widgetSafeWidgetStateMethod;

    private static Object stateNormal;
    private static Object stateSneaking;
    private static Object statePunch;

    private static Object previewModel;
    private static Object previewEntityModelsIdentity;

    private static volatile boolean resolvedPreviewWidget;
    private static volatile boolean previewWidgetAvailable;

    private static Class<?> legacyPlayerSkinWidgetClass;
    private static Constructor<?> legacyPlayerSkinWidgetCtor;
    private static Field previewWidgetRotationXField;
    private static Field previewWidgetRotationYField;
    private static Field previewWidgetStateField;
    private static Field previewWidgetModelField;
    private static Method previewWidgetVisibleMethod;
    private static Method previewWidgetCleanupMethod;

    private static final Map<String, LegacyRef> REF_BY_ID = new ConcurrentHashMap<>();
    private static final Map<PackOrdinalKey, String> ID_BY_PACK_ORDINAL = new ConcurrentHashMap<>();
    private static final Map<PlayerSkinWidget, PreviewWidgetHolder> PREVIEW_WIDGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Object> OPAQUE_CPM_PREVIEW_MODELS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Map<Object, EmbeddedPreviewPose> EMBEDDED_CPM_PREVIEW_POSES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Integer> EMBEDDED_CPM_PREVIEW_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<EmbeddedPreviewPose> CURRENT_EMBEDDED_CPM_PREVIEW_POSE = new ThreadLocal<>();

    private LegacySkinsCompat() {
    }

    public static boolean isLegacySkinId(String skinId) {
        return skinId != null && skinId.startsWith(PREFIX);
    }

    public static boolean isAvailable() {
        return resolveCore();
    }

    public static boolean isPresent() {
        if (checkedPresence) return present;
        synchronized (LegacySkinsCompat.class) {
            if (checkedPresence) return present;
            checkedPresence = true;
            try {
                Class.forName("io.github.redrain0o0.legacyskins.LegacySkins");
                present = true;
            } catch (Throwable ignored) {
                present = false;
            }
            return present;
        }
    }

    public static boolean isPreviewSupported() {
        return resolvePreviewWidget() || resolvePreview();
    }

    public static boolean isPreviewPoseSupported() {
        if (!isPreviewSupported()) return false;
        try {
            ensurePreviewStatesResolved();
            return stateNormal != null && stateSneaking != null && statePunch != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clearRegisteredSkins() {
        cleanupPreviewWidgets();
        REF_BY_ID.clear();
        ID_BY_PACK_ORDINAL.clear();
        OPAQUE_CPM_PREVIEW_MODELS.clear();
        EMBEDDED_CPM_PREVIEW_POSES.clear();
        previewModel = null;
        previewEntityModelsIdentity = null;
    }

    public static void resetPreviewCache() {
        cleanupPreviewWidgets();
        OPAQUE_CPM_PREVIEW_MODELS.clear();
        EMBEDDED_CPM_PREVIEW_POSES.clear();
        previewModel = null;
        previewEntityModelsIdentity = null;
        EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.remove();
        CURRENT_EMBEDDED_CPM_PREVIEW_POSE.remove();
    }

    public static void unregisterLegacySkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return;
        LegacyRef ref = REF_BY_ID.remove(skinId);
        if (ref == null) return;
        ID_BY_PACK_ORDINAL.remove(new PackOrdinalKey(ref.packId(), ref.ordinal()));
    }

    public static boolean isDefaultPack(ResourceLocation packId) {
        return LEGACY_DEFAULT_PACK.equals(packId);
    }

    public static boolean isFavoritesPack(ResourceLocation packId) {
        return LEGACY_FAVORITES_PACK.equals(packId);
    }

    public static int runtimeOrdinal(ResourceLocation packId, int rawOrdinal) {
        return isDefaultPack(packId) ? rawOrdinal + 1 : rawOrdinal;
    }

    public static String syntheticSkinId(ResourceLocation packId, int runtimeOrdinal) {
        if (packId == null) return null;
        return PREFIX + packId.getNamespace() + ":" + packId.getPath() + "#" + runtimeOrdinal;
    }

    public static void registerLegacySkin(String skinId, ResourceLocation packId, int runtimeOrdinal) {
        if (skinId == null || skinId.isBlank() || packId == null || runtimeOrdinal < 0) return;
        LegacyRef ref = new LegacyRef(packId, runtimeOrdinal);
        REF_BY_ID.put(skinId, ref);
        ID_BY_PACK_ORDINAL.put(new PackOrdinalKey(packId, runtimeOrdinal), skinId);
    }

    public static boolean isSkinUsable(ResourceLocation packId, int runtimeOrdinal) {
        if (!resolveCore()) return false;
        Object legacySkin = resolveLegacySkin(packId, runtimeOrdinal);
        if (legacySkin == null) return true;
        try {
            Object usable = legacySkinCanBeUsedMethod.invoke(legacySkin);
            return !(usable instanceof Boolean b) || b;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean shouldSkipPack(ResourceLocation packId, String type) {
        if (packId == null) return true;
        if (isFavoritesPack(packId)) return true;
        if (type == null || !type.trim().equalsIgnoreCase("dev")) return false;

        Boolean showDev = getShowDevPacks();
        if (showDev == null || showDev) return false;

        ResourceLocation current = getCurrentSelectedPack();
        return current == null || !current.equals(packId);
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

    public static String getCurrentSelectedSkinId() {
        if (!resolveCore()) return null;
        try {
            Object config = lazyInstanceMethod.invoke(null);
            if (config == null) return null;
            Object skinsConfig = configGetActiveSkinsConfigMethod.invoke(config);
            if (skinsConfig == null) return null;
            Object opt = skinsConfigGetCurrentSkinMethod.invoke(skinsConfig);
            if (!(opt instanceof Optional<?> optional) || optional.isEmpty()) return null;
            Object ref = optional.get();
            ResourceLocation pack = invokePack(ref);
            Integer ordinal = invokeOrdinal(ref);
            if (pack == null || ordinal == null) return null;
            String resolvedId = ID_BY_PACK_ORDINAL.get(new PackOrdinalKey(pack, ordinal));
            return resolvedId != null ? resolvedId : syntheticSkinId(pack, ordinal);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean canResolveSelectedSkinId(String skinId) {
        if (skinId == null || skinId.isBlank() || !isLegacySkinId(skinId)) return false;
        if (REF_BY_ID.containsKey(skinId)) return true;
        String selectedId = getCurrentSelectedSkinId();
        return skinId.equals(selectedId);
    }

    public static ResourceLocation getCurrentSelectedPack() {
        if (!resolveCore()) return null;
        try {
            Object config = lazyInstanceMethod.invoke(null);
            if (config == null) return null;
            Object skinsConfig = configGetActiveSkinsConfigMethod.invoke(config);
            if (skinsConfig == null) return null;
            Object opt = skinsConfigGetCurrentSkinMethod.invoke(skinsConfig);
            if (!(opt instanceof Optional<?> optional) || optional.isEmpty()) return null;
            Object ref = optional.get();
            return invokePack(ref);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean applySelectedSkin(String skinId) {
        if (!resolveCore()) return false;
        LegacyRef ref = REF_BY_ID.get(skinId);
        if (ref == null) return false;
        try {
            Object config = lazyInstanceMethod.invoke(null);
            if (config == null) return false;
            Object refObj = skinReferenceCtor.newInstance(ref.packId(), ref.ordinal());
            configSetSkinMethod.invoke(config, refObj);
            if (configSaveMethod != null) configSaveMethod.invoke(config);
            return true;
        } catch (Throwable t) {
            DebugLog.warn("LegacySkinsCompat failed to apply {}: {}", skinId, t.toString());
            return false;
        }
    }

    public static void clearSelectedSkin() {
        if (!resolveCore()) return;
        try {
            Object config = lazyInstanceMethod.invoke(null);
            if (config == null) return;
            configSetSkinMethod.invoke(config, new Object[]{null});
            if (configSaveMethod != null) configSaveMethod.invoke(config);
        } catch (Throwable t) {
            DebugLog.warn("LegacySkinsCompat failed to clear selection: {}", t.toString());
        }
    }

    public static boolean canRenderPreview(String skinId) {
        LegacyRef ref = REF_BY_ID.get(skinId);
        return ref != null && (resolvePreviewWidget() || resolvePreview()) && resolveLegacySkin(ref.packId(), ref.ordinal()) != null;
    }

    public static void markEmbeddedCpmPreviewModel(Object model) {
        if (model == null) return;
        OPAQUE_CPM_PREVIEW_MODELS.add(model);
    }

    public static boolean isEmbeddedCpmPreviewModel(Object model) {
        return model != null && OPAQUE_CPM_PREVIEW_MODELS.contains(model);
    }

    public static void beginEmbeddedCpmPreviewRender(Object model) {
        if (!isEmbeddedCpmPreviewModel(model)) return;
        EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.set(EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.get() + 1);
        CURRENT_EMBEDDED_CPM_PREVIEW_POSE.set(EMBEDDED_CPM_PREVIEW_POSES.get(model));
    }

    public static void endEmbeddedCpmPreviewRender(Object model) {
        if (!isEmbeddedCpmPreviewModel(model)) return;
        int depth = EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.get();
        if (depth <= 1) {
            EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.remove();
            CURRENT_EMBEDDED_CPM_PREVIEW_POSE.remove();
            return;
        }
        EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.set(depth - 1);
    }

    public static boolean isRenderingEmbeddedCpmPreview() {
        return EMBEDDED_CPM_PREVIEW_RENDER_DEPTH.get() > 0;
    }

    public static void rememberEmbeddedCpmPreviewPose(Object model, String skinId, boolean crouching, boolean punchLoop) {
        if (!isEmbeddedCpmPreviewModel(model)) return;
        EMBEDDED_CPM_PREVIEW_POSES.put(model, new EmbeddedPreviewPose(skinId, crouching, punchLoop));
    }

    public static void prepareEmbeddedCpmPreviewPose(Object model, Object safeWidget, PlayerModel playerModel) {
        if (!isRenderingEmbeddedCpmPreview() || model == null || safeWidget == null || playerModel == null) return;
        try {
            if (widgetModelSetupAnimMethod == null && resolvePreview()) {
                widgetModelSetupAnimMethod = widgetModelClass.getMethod("setupAnim", widgetSafeWidgetClass, PlayerModel.class);
            }
            if (widgetModelSetupAnimMethod != null) {
                widgetModelSetupAnimMethod.invoke(model, safeWidget, playerModel);
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isEmbeddedCpmPreviewPunchState(Object safeWidget) {
        EmbeddedPreviewPose pose = CURRENT_EMBEDDED_CPM_PREVIEW_POSE.get();
        if (pose != null) return pose.punchLoop();
        return safeWidget != null && previewState(safeWidget) == statePunch;
    }

    public static boolean isEmbeddedCpmPreviewSneakingState(Object safeWidget) {
        EmbeddedPreviewPose pose = CURRENT_EMBEDDED_CPM_PREVIEW_POSE.get();
        if (pose != null) return pose.crouching();
        return safeWidget != null && previewState(safeWidget) == stateSneaking;
    }

    public static boolean isEmbeddedCpmPreviewPunching() {
        EmbeddedPreviewPose pose = CURRENT_EMBEDDED_CPM_PREVIEW_POSE.get();
        return pose != null && pose.punchLoop();
    }

    public static boolean isEmbeddedCpmPreviewSneaking() {
        EmbeddedPreviewPose pose = CURRENT_EMBEDDED_CPM_PREVIEW_POSE.get();
        return pose != null && pose.crouching();
    }

    public static String getEmbeddedCpmPreviewSkinId() {
        EmbeddedPreviewPose pose = CURRENT_EMBEDDED_CPM_PREVIEW_POSE.get();
        return pose != null ? pose.skinId() : null;
    }

    public static RenderType opaqueEmbeddedCpmRenderType(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
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
        if (renderPreviewLowLevel(guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom)) {
            return true;
        }
        return renderPreviewWithWidget(owner, guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom);
    }

    public static boolean renderPreview(GuiGraphics guiGraphics,
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
        return renderPreview(null, guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom);
    }

    private static boolean renderPreviewWithWidget(PlayerSkinWidget owner,
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
        if (owner == null || guiGraphics == null) return false;
        LegacyRef ref = REF_BY_ID.get(skinId);
        if (ref == null) return false;
        if (!resolvePreviewWidget()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        EntityModelSet entityModels = mc.getEntityModels();
        if (entityModels == null) return false;

        PreviewRect previewRect = toPreviewRect(left, top, right, bottom);
        float effectiveRotationX = 0.0F;
        float effectiveRotationY = -rotationY;

        try {
            PreviewWidgetHolder holder = PREVIEW_WIDGETS.get(owner);
            if (holder == null || holder.widget() == null || holder.entityModelsIdentity() != entityModels) {
                holder = createPreviewWidgetHolder(entityModels);
                if (holder == null) return false;
                PREVIEW_WIDGETS.put(owner, holder);
            }

            holder.supplier().packId = ref.packId();
            holder.supplier().ordinal = ref.ordinal();

            AbstractWidget widget = (AbstractWidget) holder.widget();
            widget.active = true;
            widget.visible = true;
            widget.setAlpha(1.0F);
            widget.setX(previewRect.left());
            widget.setY(previewRect.top());
            widget.setWidth(previewRect.width());
            widget.setHeight(previewRect.height());

            previewWidgetRotationXField.setFloat(holder.widget(), effectiveRotationX);
            previewWidgetRotationYField.setFloat(holder.widget(), effectiveRotationY);
            previewWidgetStateField.set(holder.widget(), punchLoop ? statePunch : (crouching ? stateSneaking : stateNormal));
            rememberEmbeddedCpmPreviewPose(holder.previewModel(), skinId, crouching, punchLoop);
            previewWidgetVisibleMethod.invoke(holder.widget());
            widget.render(guiGraphics, 0, 0, partialTick);
            return true;
        } catch (Throwable t) {
            PreviewWidgetHolder holder = PREVIEW_WIDGETS.remove(owner);
            cleanupPreviewWidget(holder);
            return false;
        }
    }

    private static PreviewWidgetHolder createPreviewWidgetHolder(EntityModelSet entityModels) {
        try {
            PreviewRefSupplier supplier = new PreviewRefSupplier();
            Object widget = legacyPlayerSkinWidgetCtor.newInstance(PREVIEW_BASE_W, PREVIEW_BASE_H, entityModels, supplier);
            if (!(widget instanceof AbstractWidget abstractWidget)) return null;
            abstractWidget.active = true;
            abstractWidget.visible = true;
            abstractWidget.setAlpha(1.0F);
            abstractWidget.setWidth(PREVIEW_BASE_W);
            abstractWidget.setHeight(PREVIEW_BASE_H);
            Object previewModel = previewWidgetModelField.get(widget);
            markEmbeddedCpmPreviewModel(previewModel);
            previewWidgetVisibleMethod.invoke(widget);
            return new PreviewWidgetHolder(widget, supplier, entityModels, previewModel);
        } catch (Throwable ignored) {
            return null;
        }
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
        if (holder == null) return;
        Object previewWidget = holder.widget();
        if (previewWidget != null && previewWidgetCleanupMethod != null) {
            try {
                previewWidgetCleanupMethod.invoke(previewWidget);
            } catch (Throwable ignored) {
            }
        }
        Object holderPreviewModel = holder.previewModel();
        if (holderPreviewModel != null) {
            OPAQUE_CPM_PREVIEW_MODELS.remove(holderPreviewModel);
            EMBEDDED_CPM_PREVIEW_POSES.remove(holderPreviewModel);
        }
    }

    private static PreviewRect toPreviewRect(int left, int top, int right, int bottom) {
        int fullWidth = Math.max(1, right - left);
        int fullHeight = Math.max(1, bottom - top);

        int previewWidth = Math.max(1, Math.round(fullWidth * PREVIEW_RECT_SCALE));
        int previewHeight = Math.max(1, Math.round(fullHeight * PREVIEW_RECT_SCALE));

        int insetX = Math.max(0, (fullWidth - previewWidth) / 2);
        int insetY = Math.max(0, (fullHeight - previewHeight) / 2);

        int previewLeft = left + insetX;
        int previewTop = top + insetY;
        return new PreviewRect(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight);
    }

    private static boolean renderPreviewLowLevel(GuiGraphics guiGraphics,
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
        LegacyRef ref = REF_BY_ID.get(skinId);
        if (ref == null || guiGraphics == null) return false;
        if (!resolvePreview()) return false;

        Object legacySkin = resolveLegacySkin(ref.packId(), ref.ordinal());
        if (legacySkin == null) return false;

        PreviewRect previewRect = toPreviewRect(left, top, right, bottom);
        float effectiveRotationX = 0.0F;
        float effectiveRotationY = -rotationY;

        try {
            Object model = getOrBakePreviewModel();
            if (model == null) return false;
            rememberEmbeddedCpmPreviewPose(model, skinId, crouching, punchLoop);

            Object state = punchLoop ? statePunch : (crouching ? stateSneaking : stateNormal);
            Object safeWidget = widgetSafeWidgetCtor.newInstance(state, legacySkin);

            int x0 = previewRect.left() - EXTEND_BY;
            int y0 = previewRect.top() - EXTEND_BY;
            int x1 = previewRect.right() + EXTEND_BY;
            int y1 = previewRect.bottom() + EXTEND_BY;
            float scale = previewRect.height() / MODEL_HEIGHT;

            ScreenRectangle scissor = guiGraphics.scissorStack.peek();
            ScreenRectangle bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissor);

            Object renderState = guiCpmSkinRenderStateCtor.newInstance(
                    safeWidget,
                    model,
                    effectiveRotationX,
                    effectiveRotationY,
                    PIVOT_Y,
                    x0,
                    y0,
                    x1,
                    y1,
                    scale,
                    scissor,
                    bounds,
                    partialTick
            );

            guiGraphics.guiRenderState.submitPicturesInPictureState((PictureInPictureRenderState) renderState);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object resolveLegacySkin(ResourceLocation packId, int runtimeOrdinal) {
        if (!resolveCore() || packId == null || runtimeOrdinal < 0) return null;
        try {
            Object mapObj = legacySkinPackListField.get(null);
            if (!(mapObj instanceof Map<?, ?> packs)) return null;
            Object pack = packs.get(packId);
            if (pack == null) return null;
            Method skinsMethod = pack.getClass().getMethod("skins");
            Object listObj = skinsMethod.invoke(pack);
            if (!(listObj instanceof java.util.List<?> list)) return null;
            if (runtimeOrdinal >= list.size()) return null;
            Object skin = list.get(runtimeOrdinal);
            if (skin == null) return null;
            return skin;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ResourceLocation invokePack(Object skinReference) {
        if (skinReference == null) return null;
        try {
            Object pack = skinReference.getClass().getMethod("pack").invoke(skinReference);
            return pack instanceof ResourceLocation rl ? rl : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer invokeOrdinal(Object skinReference) {
        if (skinReference == null) return null;
        try {
            Object ordinal = skinReference.getClass().getMethod("ordinal").invoke(skinReference);
            return ordinal instanceof Integer i ? i : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Boolean getShowDevPacks() {
        if (!resolveCore()) return null;
        try {
            Object config = lazyInstanceMethod.invoke(null);
            if (config == null) return null;
            Object value = configShowDevPacksMethod.invoke(config);
            return value instanceof Boolean b ? b : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getOrBakePreviewModel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        Object entityModels = mc.getEntityModels();
        if (entityModels == null) return null;
        if (previewModel != null && previewEntityModelsIdentity == entityModels) return previewModel;
        try {
            previewModel = widgetModelBakeMethod.invoke(null, entityModels);
            previewEntityModelsIdentity = entityModels;
            markEmbeddedCpmPreviewModel(previewModel);
            return previewModel;
        } catch (Throwable ignored) {
            previewModel = null;
            previewEntityModelsIdentity = null;
            return null;
        }
    }

    private static boolean resolveCore() {
        if (resolvedCore) return coreAvailable;
        synchronized (LegacySkinsCompat.class) {
            if (resolvedCore) return coreAvailable;
            resolvedCore = true;
            try {
                legacySkinsClass = Class.forName("io.github.redrain0o0.legacyskins.LegacySkins");
                legacyConfigClass = Class.forName("io.github.redrain0o0.legacyskins.LegacySkinsConfig");
                legacySkinsConfigClass = Class.forName("io.github.redrain0o0.legacyskins.LegacySkinsConfig$SkinsConfig");
                skinReferenceClass = Class.forName("io.github.redrain0o0.legacyskins.SkinReference");
                legacySkinPackClass = Class.forName("io.github.redrain0o0.legacyskins.client.LegacySkinPack");
                legacySkinClass = Class.forName("io.github.redrain0o0.legacyskins.client.LegacySkin");

                lazyInstanceMethod = legacySkinsClass.getMethod("lazyInstance");
                configSetSkinMethod = legacyConfigClass.getMethod("setSkin", skinReferenceClass);
                configSaveMethod = legacyConfigClass.getMethod("save");
                configGetActiveSkinsConfigMethod = legacyConfigClass.getMethod("getActiveSkinsConfig");
                configShowDevPacksMethod = legacyConfigClass.getMethod("showDevPacks");
                skinsConfigGetCurrentSkinMethod = legacySkinsConfigClass.getMethod("getCurrentSkin");
                legacySkinCanBeUsedMethod = legacySkinClass.getMethod("canBeUsed");
                legacySkinPackListField = legacySkinPackClass.getField("list");

                skinReferenceCtor = skinReferenceClass.getConstructor(ResourceLocation.class, int.class);
                coreAvailable = true;
            } catch (Throwable ignored) {
                coreAvailable = false;
            }
            return coreAvailable;
        }
    }

    private static boolean resolvePreview() {
        if (resolvedPreview) return previewAvailable;
        synchronized (LegacySkinsCompat.class) {
            if (resolvedPreview) return previewAvailable;
            resolvedPreview = true;
            if (!resolveCore()) {
                previewAvailable = false;
                return false;
            }
            try {
                guiCpmSkinRenderStateClass = Class.forName("io.github.redrain0o0.legacyskins.client.screen.GuiCpmSkinRenderState");
                ensurePreviewStatesResolved();
                widgetSafeWidgetClass = Class.forName("io.github.redrain0o0.legacyskins.client.screen.PlayerSkinWidget$SafeWidget");
                widgetModelClass = Class.forName("io.github.redrain0o0.legacyskins.client.screen.PlayerSkinWidget$Model");

                guiCpmSkinRenderStateCtor = guiCpmSkinRenderStateClass.getConstructor(
                        widgetSafeWidgetClass,
                        widgetModelClass,
                        float.class,
                        float.class,
                        float.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class,
                        ScreenRectangle.class,
                        ScreenRectangle.class,
                        float.class
                );
                widgetSafeWidgetCtor = widgetSafeWidgetClass.getDeclaredConstructor(widgetStateClass, legacySkinClass);
                widgetSafeWidgetCtor.setAccessible(true);
                widgetSafeWidgetStateMethod = widgetSafeWidgetClass.getMethod("statf");
                widgetModelBakeMethod = widgetModelClass.getMethod("bake", EntityModelSet.class);
                widgetModelSetupAnimMethod = widgetModelClass.getMethod("setupAnim", widgetSafeWidgetClass, PlayerModel.class);

                previewAvailable = true;
            } catch (Throwable t) {
                previewAvailable = false;
            }
            return previewAvailable;
        }
    }

    private static void ensurePreviewStatesResolved() throws Exception {
        if (widgetStateClass != null && stateNormal != null && stateSneaking != null && statePunch != null) return;
        widgetStateClass = Class.forName("io.github.redrain0o0.legacyskins.client.screen.PlayerSkinWidget$State");
        Object[] states = widgetStateClass.getEnumConstants();
        if (states == null || states.length == 0) throw new IllegalStateException("No LegacySkins widget states");
        stateNormal = enumConstant(widgetStateClass, "NORMKFL", states[0]);
        stateSneaking = enumConstant(widgetStateClass, "STEAKING", stateNormal);
        statePunch = enumConstant(widgetStateClass, "PCFVUCING", stateNormal);
    }

    private static boolean resolvePreviewWidget() {
        if (resolvedPreviewWidget) return previewWidgetAvailable;
        synchronized (LegacySkinsCompat.class) {
            if (resolvedPreviewWidget) return previewWidgetAvailable;
            resolvedPreviewWidget = true;
            if (!resolveCore()) {
                previewWidgetAvailable = false;
                return false;
            }
            try {
                ensurePreviewStatesResolved();
                legacyPlayerSkinWidgetClass = Class.forName("io.github.redrain0o0.legacyskins.client.screen.PlayerSkinWidget");
                legacyPlayerSkinWidgetCtor = legacyPlayerSkinWidgetClass.getConstructor(int.class, int.class, EntityModelSet.class, Supplier.class);
                previewWidgetRotationXField = legacyPlayerSkinWidgetClass.getDeclaredField("rotationX");
                previewWidgetRotationXField.setAccessible(true);
                previewWidgetRotationYField = legacyPlayerSkinWidgetClass.getDeclaredField("rotationY");
                previewWidgetRotationYField.setAccessible(true);
                previewWidgetStateField = legacyPlayerSkinWidgetClass.getDeclaredField("statf");
                previewWidgetStateField.setAccessible(true);
                previewWidgetModelField = legacyPlayerSkinWidgetClass.getDeclaredField("model");
                previewWidgetModelField.setAccessible(true);
                previewWidgetVisibleMethod = legacyPlayerSkinWidgetClass.getMethod("visible");
                try {
                    previewWidgetCleanupMethod = legacyPlayerSkinWidgetClass.getMethod("cleanup");
                } catch (Throwable ignored) {
                    previewWidgetCleanupMethod = null;
                }
                previewWidgetAvailable = true;
            } catch (Throwable t) {
                previewWidgetAvailable = false;
            }
            return previewWidgetAvailable;
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

    private static Object previewState(Object safeWidget) {
        if (safeWidget == null) return null;
        try {
            if (widgetSafeWidgetStateMethod == null && widgetSafeWidgetClass != null) {
                widgetSafeWidgetStateMethod = widgetSafeWidgetClass.getMethod("statf");
            }
            return widgetSafeWidgetStateMethod != null ? widgetSafeWidgetStateMethod.invoke(safeWidget) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class PreviewRefSupplier implements Supplier<Object> {
        private volatile ResourceLocation packId = LEGACY_DEFAULT_PACK;
        private volatile int ordinal;

        @Override
        public Object get() {
            try {
                return skinReferenceCtor.newInstance(packId, ordinal);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private record PreviewWidgetHolder(Object widget, PreviewRefSupplier supplier, Object entityModelsIdentity, Object previewModel) {
    }

    private record EmbeddedPreviewPose(String skinId, boolean crouching, boolean punchLoop) {
    }

    private record PreviewRect(int left, int top, int right, int bottom) {
        private int width() {
            return Math.max(1, right - left);
        }

        private int height() {
            return Math.max(1, bottom - top);
        }
    }

    private record LegacyRef(ResourceLocation packId, int ordinal) {
    }

    private record PackOrdinalKey(ResourceLocation packId, int ordinal) {
    }
}
