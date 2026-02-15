package wily.legacy.Skins.client.gui;

import wily.legacy.Skins.client.cpm.CpmModelManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class GuiCpmPreviewCache {
    private static final Executor CPM_PREWARM_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)), r -> {
                Thread t = new Thread(r, "ConsoleSkins-CPM-Prewarm");
                t.setDaemon(true);
                return t;
            }
    );
    private static final Map<String, CompletableFuture<?>> CPM_PREWARM_TASKS = new ConcurrentHashMap<>();
    private static final Map<String, GameProfile> CPM_PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CPM_LAST_ATTEMPT_MS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> CPM_READY = new ConcurrentHashMap<>();

    private GuiCpmPreviewCache() {
    }

    public static void clearCaches() {
        CPM_PREWARM_TASKS.clear();
        CPM_PROFILE_CACHE.clear();
        CPM_LAST_ATTEMPT_MS.clear();
        CPM_READY.clear();
    }

    public static void prewarmMenuPreview(String selectionId, ResourceLocation skinTexture) {
        if (selectionId == null || skinTexture == null) return;
        if (!selectionId.startsWith("cpm:")) return;
        if (CustomPlayerModelsClient.mc == null) return;
        String key = selectionId + "|" + skinTexture;
        if (Boolean.TRUE.equals(CPM_READY.get(key))) return;
        long now = System.currentTimeMillis();
        Long last = CPM_LAST_ATTEMPT_MS.get(key);
        if (last != null && now - last < 750L) return;
        if (CPM_PREWARM_TASKS.containsKey(key)) return;
        CPM_LAST_ATTEMPT_MS.put(key, now);
        CompletableFuture<?> fut = CompletableFuture.runAsync(() -> warmupCpmProfile(selectionId, skinTexture), CPM_PREWARM_EXECUTOR).whenComplete((v, t) -> CPM_PREWARM_TASKS.remove(key));
        CPM_PREWARM_TASKS.put(key, fut);
    }

    static GameProfile getOrCreateCpmProfile(String selectionId, ResourceLocation skinTexture) {
        String key = (selectionId == null ? "" : selectionId) + "|" + skinTexture;
        GameProfile gp = CPM_PROFILE_CACHE.get(key);
        if (gp != null) return gp;
        GameProfile created = new GameProfile(ProfilePropertyUtil.stablePreviewUUID(key), "ConsoleSkinsPreview");
        gp = CPM_PROFILE_CACHE.putIfAbsent(key, created);
        return gp == null ? created : gp;
    }

    public static boolean isReady(String selectionId, ResourceLocation skinTexture) {
        if (selectionId == null || skinTexture == null) return false;
        return Boolean.TRUE.equals(CPM_READY.get(selectionId + "|" + skinTexture));
    }

    public static boolean isResolved(String selectionId, ResourceLocation skinTexture) {
        if (selectionId == null || skinTexture == null) return false;
        if (!selectionId.startsWith("cpm:")) return false;
        if (!isReady(selectionId, skinTexture)) return false;
        try {
            if (CustomPlayerModelsClient.mc == null) return false;
            GameProfile gp = getOrCreateCpmProfile(selectionId, skinTexture);
            Player<?> pl = CustomPlayerModelsClient.mc.getDefinitionLoader().loadPlayer(gp, ModelDefinitionLoader.PLAYER_UNIQUE);
            return pl != null && pl.getModelDefinition() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void warmupCpmProfile(String selectionId, ResourceLocation skinTexture) {
        if (selectionId == null || skinTexture == null) return;
        if (!selectionId.startsWith("cpm:")) return;
        String key = selectionId + "|" + skinTexture;
        GameProfile gp = getOrCreateCpmProfile(selectionId, skinTexture);

        try {
            ensureTexturesProperty(gp, skinTexture);
        } catch (Throwable ignored) {
        }
        if (CustomPlayerModelsClient.mc == null) {
            CPM_READY.put(key, Boolean.TRUE);
            return;
        }
        try {
            CpmModelManager.applyToProfile(gp, selectionId);
        } catch (Throwable ignored) {
        }
        CPM_READY.put(key, Boolean.TRUE);
    }

    private static boolean ensureTexturesProperty(GameProfile gp, ResourceLocation skinTexture) {
        if (gp == null || skinTexture == null) return false;
        try {
            if (ProfilePropertyUtil.hasTexturesProperty(gp)) return true;
            Path outFile = exportSkinToTempFile(skinTexture);
            if (outFile == null) return false;
            String url = outFile.toUri().toString();
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\",\"metadata\":{\"model\":\"default\"}}}}";
            String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            ProfilePropertyUtil.putProfileProperty(gp, "textures", new Property("textures", b64));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Path exportSkinToTempFile(ResourceLocation skinTexture) {
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("cache/consoleskins_cpm");
            Files.createDirectories(dir);
            String safe = skinTexture.toString().replace(':', '_').replace('/', '_');
            Path out = dir.resolve(safe + ".png");
            if (Files.exists(out) && Files.size(out) > 0) return out;
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(skinTexture)) {
                Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }
}
