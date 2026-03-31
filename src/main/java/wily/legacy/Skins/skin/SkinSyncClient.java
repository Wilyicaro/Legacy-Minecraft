package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import wily.factoryapi.base.network.CommonNetwork;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSyncClient {
    private static String pendingSkinId;
    private static volatile boolean pendingUploadRequest;
    private static volatile boolean announcedThisSession;
    private static volatile int snapshotRequestDelayTicks = -1;
    private static final ConcurrentHashMap<String, Boolean> SENT_ASSETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SkinChunkAccumulator> ASSET_CHUNKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> LAST_APPLIED = new ConcurrentHashMap<>();
    private static final int SCAN_INTERVAL = 20;
    private static int scanTickCounter;
    private record SkinAssetData(byte[] texture, byte[] model) { }
    private SkinSyncClient() { }
    public static void initClient() {
        UUID userId = getUserId(Minecraft.getInstance());
        if (userId != null) { loadPersistedSelection(userId); }
    }
    public static void ensureInitialSkinLoaded(Minecraft client) {
        UUID userId = getUserId(client);
        if (userId == null || SkinIdUtil.hasSkin(ClientSkinCache.get(userId))) return;
        loadPersistedSelection(userId);
    }
    public static void postTick(Minecraft client) {
        syncLocalPlayerCache(client);
        refreshKnownPlayerNames(client);
        if (pendingUploadRequest && isConnected(client)) {
            pendingUploadRequest = false;
            onRequestSkinUpload();
        }
        if (snapshotRequestDelayTicks >= 0 && isConnected(client)) {
            snapshotRequestDelayTicks--;
            if (snapshotRequestDelayTicks <= 0) {
                snapshotRequestDelayTicks = -1;
                CommonNetwork.sendToServer(new SkinSync.RequestSnapshotC2S());
            }
        }
        if (!announcedThisSession && isConnected(client)) { onRequestSkinUpload(); }
        if (pendingSkinId == null || !isConnected(client)) return;
        String skinId = pendingSkinId;
        pendingSkinId = null;
        requestSetSkin(client, skinId);
    }
    public static void onClientJoin() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        snapshotRequestDelayTicks = 1;
        if (pendingSkinId != null) {
            String skinId = pendingSkinId;
            pendingSkinId = null;
            requestSetSkin(client, skinId);
            return;
        }
        if (client.player == null) return;
        String skinId = resolveSelectedSkinId(client);
        if (!SkinIdUtil.hasSkin(skinId)) return;
        if (isConnected(client)) {
            onRequestSkinUpload();
        } else { pendingUploadRequest = true; }
    }
    public static void onClientDisconnect() {
        pendingSkinId = null;
        pendingUploadRequest = false;
        announcedThisSession = false;
        snapshotRequestDelayTicks = -1;
        LAST_APPLIED.clear();
        scanTickCounter = 0;
        Minecraft client = Minecraft.getInstance();
        UUID keep = getUserId(client);
        String keepVal = keep == null ? null : ClientSkinCache.get(keep);
        ClientSkinCache.clear();
        ClientSkinAssets.clear();
        SENT_ASSETS.clear();
        ASSET_CHUNKS.clear();
        if (keep != null && SkinIdUtil.hasSkin(keepVal)) { ClientSkinCache.set(keep, keepVal); }
    }
    public static void requestSetSkin(Minecraft client, String skinId) {
        if (client == null) return;
        String id = SkinIdUtil.normalize(skinId);
        saveSelection(client, id);
        cacheLocalSelection(client, id);
        if (!isConnected(client)) {
            pendingSkinId = id;
            return;
        }
        sendSelection(client, id);
    }
    public static void onRequestSkinUpload() {
        Minecraft client = Minecraft.getInstance();
        if (!isConnected(client)) {
            pendingUploadRequest = true;
            return;
        }
        sendSelection(client, resolveSelectedSkinId(client));
    }
    public static void onSyncAssetChunk(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        if (!SkinIdUtil.hasSkin(skinId) || total <= 0) return;
        String key = uuid + "|" + skinId + "|" + assetType;
        SkinChunkAccumulator acc = ASSET_CHUNKS.computeIfAbsent(key, ignored -> new SkinChunkAccumulator(total));
        acc.put(index, data);
        if (!acc.isComplete()) return;
        ASSET_CHUNKS.remove(key);
        applySyncedAsset(skinId, assetType, acc.assemble());
        ClientSkinCache.set(uuid, skinId);
    }
    public static void onSyncSkin(UUID uuid, String skinId) { ClientSkinCache.set(uuid, skinId); }
    private static UUID getUserId(Minecraft client) { return client == null || client.getUser() == null ? null : client.getUser().getProfileId(); }
    private static boolean isConnected(Minecraft client) { return client != null && client.player != null && client.getConnection() != null; }
    private static String loadPersistedSelection(UUID userId) {
        String skinId = ClientSkinPersistence.load(userId);
        if (SkinIdUtil.hasSkin(skinId)) { ClientSkinCache.set(userId, skinId); }
        return skinId;
    }
    private static void saveSelection(Minecraft client, String skinId) {
        UUID userId = getUserId(client);
        if (userId != null) { ClientSkinPersistence.save(userId, skinId); }
    }
    private static String resolveSelectedSkinId(Minecraft client) {
        if (client == null || client.player == null) return "";
        String skinId = ClientSkinCache.get(client.player.getUUID());
        if (SkinIdUtil.hasSkin(skinId)) return skinId;
        UUID userId = getUserId(client);
        if (userId == null) return "";
        skinId = ClientSkinCache.get(userId);
        if (!SkinIdUtil.hasSkin(skinId)) { skinId = loadPersistedSelection(userId); }
        if (SkinIdUtil.hasSkin(skinId)) {
            ClientSkinCache.set(client.player.getUUID(), skinId);
            return skinId;
        }
        return "";
    }
    private static void cacheLocalSelection(Minecraft client, String skinId) {
        if (client == null) return;
        if (client.player != null) ClientSkinCache.set(client.player.getUUID(), skinId);
        if (client.getUser() != null) ClientSkinCache.set(client.getUser().getProfileId(), skinId);
    }
    private static void syncLocalPlayerCache(Minecraft client) {
        if (client == null || client.player == null) return;
        if (SkinIdUtil.hasSkin(ClientSkinCache.get(client.player.getUUID()))) return;
        UUID userId = getUserId(client);
        if (userId == null) return;
        String skinId = ClientSkinCache.get(userId);
        if (SkinIdUtil.hasSkin(skinId)) { ClientSkinCache.set(client.player.getUUID(), skinId); }
    }
    private static void refreshKnownPlayerNames(Minecraft client) {
        if (client == null || client.level == null) return;
        if (++scanTickCounter < SCAN_INTERVAL) return;
        scanTickCounter = 0;
        for (var player : client.level.players()) {
            if (player == null) continue;
            String skinId = ClientSkinCache.get(player.getUUID());
            if (!SkinIdUtil.hasSkin(skinId)) continue;
            String last = LAST_APPLIED.get(player.getUUID());
            if (Objects.equals(last, skinId)) continue;
            LAST_APPLIED.put(player.getUUID(), skinId);
            ClientSkinCache.setName(player.getScoreboardName(), skinId);
        }
    }
    private static void sendSelection(Minecraft client, String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        announcedThisSession = true;
        CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(id));
        sendAssets(client, id);
    }
    private static void sendAssets(Minecraft client, String skinId) {
        if (!SkinIdUtil.hasSkin(skinId) || SENT_ASSETS.putIfAbsent(skinId, true) != null) return;
        SkinAssetData assets = resolveAssets(client, skinId);
        sendChunks(skinId, SkinSync.ASSET_TEXTURE, assets.texture());
        sendChunks(skinId, SkinSync.ASSET_MODEL, assets.model());
    }
    private static SkinAssetData resolveAssets(Minecraft client, String skinId) {
        byte[] texture = ClientSkinAssets.getTextureBytes(skinId);
        byte[] model = ClientSkinAssets.getModelBytes(skinId);
        if (texture != null && model != null) { return new SkinAssetData(texture, model); }
        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        if (texture == null && entry != null && entry.texture() != null) { texture = loadBytes(client, entry.texture()); }
        if (model == null) { model = loadBytes(client, resolveModelLocation(client, skinId, entry)); }
        return new SkinAssetData(texture, model);
    }
    private static ResourceLocation resolveModelLocation(Minecraft client, String skinId, SkinEntry entry) {
        if (client == null || !SkinIdUtil.hasSkin(skinId)) return null;
        if (entry != null && entry.texture() != null) {
            ResourceLocation packModel = resolvePackModelLocation(entry.texture(), skinId);
            if (client.getResourceManager().getResource(packModel).isPresent()) return packModel;
            ResourceLocation localModel = ResourceLocation.fromNamespaceAndPath(entry.texture().getNamespace(), "box_models/" + skinId + ".json");
            if (client.getResourceManager().getResource(localModel).isPresent()) return localModel;
        }
        return ResourceLocation.fromNamespaceAndPath("legacy", "box_models/" + skinId + ".json");
    }
    private static ResourceLocation resolvePackModelLocation(ResourceLocation texture, String skinId) {
        String path = texture.getPath();
        int index = path.indexOf("skinpacks/");
        if (index < 0) return null;
        String after = path.substring(index + "skinpacks/".length());
        int slash = after.indexOf('/');
        if (slash <= 0) return null;
        String folder = after.substring(0, slash);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "skinpacks/" + folder + "/box_models/" + skinId + ".json");
    }
    private static void sendChunks(String skinId, int assetType, byte[] bytes) {
        if (!SkinIdUtil.hasSkin(skinId)) return;
        SkinSync.forEachChunk(bytes, SkinSync.UploadAssetChunkC2S.MAX_CHUNK, (index, total, chunk) ->
                CommonNetwork.sendToServer(new SkinSync.UploadAssetChunkC2S(skinId, assetType, index, total, chunk))
        );
    }
    private static byte[] loadBytes(Minecraft client, ResourceLocation id) {
        if (client == null || id == null) return new byte[0];
        Resource res = client.getResourceManager().getResource(id).orElse(null);
        if (res == null) return new byte[0];
        try (var in = res.open()) {
            return in.readAllBytes();
        } catch (IOException ignored) { return new byte[0]; }
    }
    private static void applySyncedAsset(String skinId, int assetType, byte[] data) {
        if (assetType == SkinSync.ASSET_TEXTURE) {
            ClientSkinAssets.putTexture(skinId, data);
        } else if (assetType == SkinSync.ASSET_MODEL) { ClientSkinAssets.putModel(skinId, data); }
    }
}
