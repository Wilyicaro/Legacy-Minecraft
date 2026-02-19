package wily.legacy.Skins.client.cpm;

import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinSync;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import com.mojang.authlib.GameProfile;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftClientAccess;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.io.ModelFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CpmModelManager {
    public static final String PREFIX = "cpm:";
    private static final long RETRY_MS = 2000L;
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Map<UUID, String> LAST_APPLIED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();
    private static final Map<String, CachedModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISSING_KEYS = ConcurrentHashMap.newKeySet();
    private static final int MAX_NET_MODELS = 64;
    @SuppressWarnings("serial")
    private static final Map<String, byte[]> NET_MODEL_FILES = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                                                                                               @Override
                                                                                               protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                                                                                                   return size() > MAX_NET_MODELS;
                                                                                               }
                                                                                           }
    );
    private static volatile boolean initialized;

    private CpmModelManager() {
    }

    public static void refreshUpsideDownFlags() {
        boolean anim;
        try {
            anim = wily.legacy.Skins.client.util.ConsoleSkinsClientSettings.isSkinAnimations();
        } catch (Throwable t) {
            anim = true;
        }

        for (var e : LAST_APPLIED.entrySet()) {
            UUID uuid = e.getKey();
            String selectionId = e.getValue();
            refreshUpsideDownFor(uuid, selectionId, anim);
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                for (var p : mc.level.players()) {
                    if (!(p instanceof AbstractClientPlayer ap)) continue;
                    UUID uuid = ap.getUUID();
                    String selectionId = ClientSkinCache.get(uuid);
                    refreshUpsideDownFor(uuid, selectionId, anim);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void refreshUpsideDownFor(UUID uuid, String selectionId, boolean animationsEnabled) {
        if (uuid == null) {
            return;
        }
        boolean flip = false;
        if (animationsEnabled && isCpmSelection(selectionId)) {
            String key = selectionId.substring(PREFIX.length());
            flip = isLegacyUpsideDownModel(key);
        }
        try {
            wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.setFlipped(uuid, flip);
        } catch (Throwable ignored) {
        }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[CPM_DEBUG] Initializing CpmModelManager");
    }

    public static void invalidateAll() {
        LAST_APPLIED.clear();
        LAST_ATTEMPT.clear();
        MODEL_CACHE.clear();
        MISSING_KEYS.clear();
        NET_MODEL_FILES.clear();
    }

    public static void applyToProfile(GameProfile profile, String selectionId) {
        if (profile == null || selectionId == null) return;
        UUID uuid = profile.id();
        if (uuid == null) return;
        String prev = LAST_APPLIED.get(uuid);
        boolean cpm = isCpmSelection(selectionId);
        if (Objects.equals(prev, selectionId)) {
            if (cpm && !isModelLoadedForProfile(profile)) {
                if (applyModel(profile, selectionId, uuid)) {
                    LAST_APPLIED.put(uuid, selectionId);
                }
            }
            return;
        }
        if (cpm) {
            if (applyModel(profile, selectionId, uuid)) {
                LAST_APPLIED.put(uuid, selectionId);
            }
            return;
        }

        if (isModelLoadedForProfile(profile)) {
            clearModel(profile);
            try {
                wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.setFlipped(uuid, false);
            } catch (Throwable ignored) {
            }
            LAST_ATTEMPT.remove(uuid);
        } else if (prev != null && isCpmSelection(prev)) {
            clearModel(profile);
            LAST_ATTEMPT.remove(uuid);
        }
        LAST_APPLIED.put(uuid, selectionId);
    }

    public static void tick(Minecraft client) {
        if (client == null || client.level == null) return;
        for (var p : client.level.players()) {
            if (!(p instanceof AbstractClientPlayer player)) continue;
            String selectionId = ClientSkinCache.get(player.getUUID());
            if (selectionId == null) continue;
            UUID uuid = player.getUUID();
            String prev = LAST_APPLIED.get(uuid);
            boolean cpm = isCpmSelection(selectionId);
            if (Objects.equals(prev, selectionId)) {
                if (cpm && !isModelLoadedForProfile(player.getGameProfile())) {
                    applyModel(player.getGameProfile(), selectionId, uuid);
                }
                continue;
            }
            if (cpm) {
                if (applyModel(player.getGameProfile(), selectionId, uuid)) {
                    LAST_APPLIED.put(uuid, selectionId);
                }
                continue;
            }

            if (isModelLoadedForProfile(player.getGameProfile())) {
                clearModel(player.getGameProfile());
                try {
                    wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.setFlipped(uuid, false);
                } catch (Throwable ignored) {
                }
                LAST_ATTEMPT.remove(uuid);
            } else if (prev != null && isCpmSelection(prev)) {
                clearModel(player.getGameProfile());
                LAST_ATTEMPT.remove(uuid);
            }
            LAST_APPLIED.put(uuid, selectionId);
        }
    }

    private static boolean isModelLoadedForProfile(GameProfile profile) {
        try {
            if (profile == null) return false;
            return CustomPlayerModelsClient.mc.getDefinitionLoader().getLoadedPlayer(profile) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isModelLoaded(UUID uuid) {
        try {
            if (uuid == null) return false;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return false;
            var p = mc.level.getPlayerByUUID(uuid);
            if (p == null) return false;
            if (!(p instanceof AbstractClientPlayer ap)) return false;
            return isModelLoadedForProfile(ap.getGameProfile());
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isCpmSelection(String selectionId) {
        return selectionId.startsWith(PREFIX) && selectionId.length() > PREFIX.length();
    }

    private static boolean isLegacyUpsideDownModel(String key) {
        if (key == null) return false;

        
        if ("legacy_skinpacks:skinpacks/birthday_3/nathan.cpmmodel".equals(key) ||
            "legacy_skinpacks:skinpacks/birthday_3/erik.cpmmodel".equals(key)) {
            return true;
        }

        
        try {
            return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, "cpm:" + key);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean applyModel(GameProfile profile, String selectionId, UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = LAST_ATTEMPT.get(uuid);
        if (last != null && now - last < RETRY_MS) return false;
        LOGGER.info("[CPM_DEBUG] Applying model to {}: {}", profile.name(), selectionId);
        MinecraftClientAccess acc = MinecraftClientAccess.get();
        if (acc == null) {
            LOGGER.error("[CPM_DEBUG] MinecraftClientAccess is null");
            return false;
        }
        ModelDefinitionLoader loader = acc.getDefinitionLoader();
        if (loader == null) {
            LOGGER.error("[CPM_DEBUG] ModelDefinitionLoader is null");
            return false;
        }
        String key = selectionId.substring(PREFIX.length());
        boolean needsFlip = isLegacyUpsideDownModel(key);
        boolean animEnabled;
        try {
            animEnabled = wily.legacy.Skins.client.util.ConsoleSkinsClientSettings.isSkinAnimations();
        } catch (Throwable t) {
            animEnabled = true;
        }
        wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.setFlipped(uuid, needsFlip && animEnabled);
        CachedModel model = MODEL_CACHE.computeIfAbsent(key, k -> {
                    LOGGER.info("[CPM_DEBUG] Loading model from cache/disk: {}", k);
                    CachedModel m = loadModelFromResources(k);
                    if (m == null) m = loadModelFromNetwork(k);
                    return m;
                }
        );
        if (model == null || model.dataBlock == null) {
            LOGGER.warn("[CPM_DEBUG] Failed to load model data for: {}", key);
            LAST_ATTEMPT.put(uuid, now);
            return false;
        }
        try {
            if (model.file != null) model.file.registerLocalCache(loader);
        } catch (Throwable t) {
            LOGGER.debug("Failed registering CPM local cache for {}: {}", key, t.toString());
        }
        try {
            loader.setModel(profile, model.dataBlock, true);
            LOGGER.info("[CPM_DEBUG] Successfully set model for: {}", profile.name());
            LAST_ATTEMPT.remove(uuid);
            return true;
        } catch (Throwable t) {
            LAST_ATTEMPT.put(uuid, now);
            LOGGER.warn("[CPM_DEBUG] Failed applying CPM model {}: {}", key, t.toString());
            return false;
        }
    }

    public static byte[] readModelFileBytesForSelection(String selectionId) {
        try {
            if (selectionId == null || !selectionId.startsWith(PREFIX) || selectionId.length() <= PREFIX.length())
                return null;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            String key = selectionId.substring(PREFIX.length());
            ResourceLocation rl = resolveModelResource(key);
            if (rl == null) return null;
            Resource res = mc.getResourceManager().getResource(rl).orElse(null);
            if (res == null) {
                LOGGER.warn("[CPM_DEBUG] Resource not found for selection: {}", rl);
                return null;
            }
            LOGGER.info("[CPM_DEBUG] Found model resource: {}", rl);
            try (InputStream is = res.open()) {
                return is.readAllBytes();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static void cacheNetworkModelFile(UUID playerUuid, String selectionId, byte[] modelFileBytes) {
        if (selectionId == null || !selectionId.startsWith(PREFIX) || selectionId.length() <= PREFIX.length()) return;
        if (modelFileBytes == null || modelFileBytes.length == 0) return;
        String key = selectionId.substring(PREFIX.length());
        NET_MODEL_FILES.put(key, modelFileBytes);
        MODEL_CACHE.remove(key);
        MISSING_KEYS.remove(key);
        if (playerUuid != null) {
            LAST_ATTEMPT.remove(playerUuid);
        }
    }

    private static ResourceLocation resolveModelResource(String key) {
        if (key == null || key.isEmpty()) return null;
        ResourceLocation base;
        try {
            if (key.indexOf(':') > 0) base = ResourceLocation.parse(key);
            else base = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, key);
        } catch (Throwable t) {
            return null;
        }
        String ns = base.getNamespace();
        String path = base.getPath();
        if (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("cpm_models/") || path.startsWith("skinpacks/")) {
        } else {
            path = "cpm_models/" + path;
        }
        if (!path.endsWith(".cpmmodel")) path = path + ".cpmmodel";
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static void clearModel(GameProfile profile) {
        MinecraftClientAccess acc = MinecraftClientAccess.get();
        if (acc == null) return;
        ModelDefinitionLoader loader = acc.getDefinitionLoader();
        if (loader == null) return;
        try {
            loader.setModel(profile, null, false);
        } catch (Throwable ignored) {
        }
    }

    private static CachedModel loadModelFromResources(String key) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            ResourceLocation rl = resolveModelResource(key);
            if (rl == null) return null;
            Resource res = mc.getResourceManager().getResource(rl).orElse(null);
            if (res == null) {
                if (MISSING_KEYS.add(key)) LOGGER.warn("[CPM_DEBUG] CPM model resource not found: {}", rl);
                return null;
            }
            LOGGER.info("[CPM_DEBUG] Loading model from resource: {}", rl);
            try (InputStream is = res.open()) {
                ModelFile mf = ModelFile.load(key, is);
                return new CachedModel(mf, mf.getDataBlock());
            }
        } catch (Throwable t) {
            if (MISSING_KEYS.add(key)) LOGGER.warn("Failed to load CPM model {}: {}", key, t.toString());
            return null;
        }
    }

    private static CachedModel loadModelFromNetwork(String key) {
        byte[] raw = NET_MODEL_FILES.get(key);
        if (raw == null || raw.length == 0) return null;
        try (InputStream is = new ByteArrayInputStream(raw)) {
            ModelFile mf = ModelFile.load(key, is);
            return new CachedModel(mf, mf.getDataBlock());
        } catch (Throwable t) {
            NET_MODEL_FILES.remove(key);
            String warnKey = "net:" + key;
            if (MISSING_KEYS.add(warnKey)) {
                LOGGER.warn("Failed to load CPM model from network cache {}: {}", key, t.toString());
            }
            return null;
        }
    }

    private static final class CachedModel {
        private final ModelFile file;
        private final byte[] dataBlock;

        private CachedModel(ModelFile file, byte[] dataBlock) {
            this.file = file;
            this.dataBlock = dataBlock;
        }
    }
}
