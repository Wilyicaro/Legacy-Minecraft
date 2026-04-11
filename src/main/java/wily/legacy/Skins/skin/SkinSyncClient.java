package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Skins.client.util.ViewBobbingSkinOverride;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSyncClient {
    private static final State STATE = new State();

    private SkinSyncClient() {
    }

    public static void initClient() {
        UUID userId = getUserId(Minecraft.getInstance());
        if (userId != null) loadStoredSelection(userId);
    }

    public static void postTick(Minecraft client) {
        syncLocalPlayerCache(client);
        refreshKnownPlayerNames(client);
        boolean connected = isConnected(client);
        if (connected && STATE.pendingUpload) {
            STATE.pendingUpload = false;
            onRequestSkinUpload();
        }
        if (connected && tickSnapshotRequest()) {
            if (SkinCloudSyncClient.isActive(client)) SkinCloudSyncClient.requestSnapshot(client, true);
            else CommonNetwork.sendToServer(new SkinSync.RequestSnapshotC2S());
        }
        if (connected && !STATE.sessionAnnounced) onRequestSkinUpload();
        if (!connected) return;
        String skinId = takePendingSkinId();
        if (skinId != null) requestSetSkin(client, skinId);
    }

    public static void onClientJoin() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        STATE.snapshotDelay = 1;
        String skinId = takePendingSkinId();
        if (skinId != null) {
            requestSetSkin(client, skinId);
            return;
        }
        if (client.player == null) return;
        skinId = resolveSelectedSkinId(client);
        if (!SkinIdUtil.hasSkin(skinId)) return;
        if (isConnected(client)) onRequestSkinUpload();
        else STATE.pendingUpload = true;
    }

    public static void onClientDisconnect() {
        Minecraft client = Minecraft.getInstance();
        UUID userId = getUserId(client);
        String keepSkinId = userId == null ? "" : SkinDataStore.getSelectedSkin(userId);
        ClientSkinCache.clear();
        ClientSkinAssets.clear();
        STATE.reset();
        if (userId != null && SkinIdUtil.hasSkin(keepSkinId)) ClientSkinCache.set(userId, keepSkinId);
        ViewBobbingSkinOverride.reset(client);
    }

    public static void requestSetSkin(Minecraft client, String skinId) {
        if (client == null) return;
        String id = SkinIdUtil.normalize(skinId);
        UUID userId = getUserId(client);
        if (userId != null) SkinDataStore.setSelectedSkin(userId, id);
        cacheLocalSelection(client, id);
        if (!isConnected(client)) {
            STATE.pendingSkinId = id;
            return;
        }
        sendSelection(client, id);
    }

    public static void onRequestSkinUpload() {
        Minecraft client = Minecraft.getInstance();
        if (!isConnected(client)) {
            STATE.pendingUpload = true;
            return;
        }
        if (SkinCloudSyncClient.isActive(client)) {
            sendSelection(client, resolveSelectedSkinId(client));
            return;
        }
        sendSelection(client, resolveSelectedSkinId(client));
    }

    public static void onSyncAssetChunk(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        if (!SkinIdUtil.hasSkin(skinId) || total <= 0) return;
        String key = uuid + "|" + skinId + "|" + assetType;
        SkinSync.SkinChunkAccumulator accumulator = STATE.assetChunks.computeIfAbsent(key, ignored -> new SkinSync.SkinChunkAccumulator(total));
        accumulator.put(index, data);
        if (!accumulator.isComplete()) return;
        STATE.assetChunks.remove(key);
        if (assetType == SkinSync.ASSET_TEXTURE) ClientSkinAssets.putTexture(skinId, accumulator.assemble());
        else if (assetType == SkinSync.ASSET_MODEL) ClientSkinAssets.putModel(skinId, accumulator.assemble());
        ClientSkinCache.set(uuid, skinId);
    }

    public static void onSyncSkin(UUID uuid, String skinId) { ClientSkinCache.set(uuid, skinId); }

    static void onCloudSnapshot(Map<UUID, String> skins) {
        if (skins == null || skins.isEmpty()) return;
        skins.forEach(ClientSkinCache::set);
    }

    private static UUID getUserId(Minecraft client) { return client == null || client.getUser() == null ? null : client.getUser().getProfileId(); }

    private static boolean isConnected(Minecraft client) { return client != null && client.player != null && client.getConnection() != null; }

    private static String loadStoredSelection(UUID userId) {
        String skinId = SkinDataStore.getSelectedSkin(userId);
        if (SkinIdUtil.hasSkin(skinId)) ClientSkinCache.set(userId, skinId);
        return skinId;
    }

    private static String resolveSelectedSkinId(Minecraft client) {
        if (client == null) return "";
        if (client.player != null) {
            String skinId = ClientSkinCache.get(client.player.getUUID());
            if (SkinIdUtil.hasSkin(skinId)) return skinId;
        }
        UUID userId = getUserId(client);
        if (userId == null) return "";
        String skinId = ClientSkinCache.get(userId);
        if (!SkinIdUtil.hasSkin(skinId)) skinId = loadStoredSelection(userId);
        if (SkinIdUtil.hasSkin(skinId) && client.player != null) ClientSkinCache.set(client.player.getUUID(), skinId);
        return SkinIdUtil.hasSkin(skinId) ? skinId : "";
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
        if (SkinIdUtil.hasSkin(skinId)) ClientSkinCache.set(client.player.getUUID(), skinId);
    }

    private static void refreshKnownPlayerNames(Minecraft client) {
        if (client == null || client.level == null || ++STATE.scanTick < State.SCAN_INTERVAL) return;
        STATE.scanTick = 0;
        boolean missingSkin = false;
        for (var player : client.level.players()) {
            if (player == null) continue;
            String skinId = ClientSkinCache.get(player.getUUID());
            if (!SkinIdUtil.hasSkin(skinId)) {
                missingSkin = true;
                continue;
            }
            if (skinId.equals(STATE.lastApplied.put(player.getUUID(), skinId))) continue;
            ClientSkinCache.setName(player.getScoreboardName(), skinId);
        }
        if (missingSkin && SkinCloudSyncClient.isActive(client)) SkinCloudSyncClient.requestSnapshot(client, false);
    }

    private static void sendSelection(Minecraft client, String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        STATE.sessionAnnounced = true;
        if (SkinCloudSyncClient.isActive(client)) {
            SkinCloudSyncClient.submitSelection(client, id);
            return;
        }
        CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(id));
        sendAssets(client, id);
    }

    private static void sendAssets(Minecraft client, String skinId) {
        if (!SkinIdUtil.hasSkin(skinId) || STATE.sentAssets.putIfAbsent(skinId, Boolean.TRUE) != null) return;
        ClientSkinAssets.AssetData assets = ClientSkinAssets.resolveAssetData(client, skinId);
        sendAssetChunks(skinId, SkinSync.ASSET_TEXTURE, assets.texture());
        sendAssetChunks(skinId, SkinSync.ASSET_MODEL, assets.model());
    }

    private static void sendAssetChunks(String skinId, int assetType, byte[] bytes) {
        if (!SkinIdUtil.hasSkin(skinId)) return;
        SkinSync.forEachChunk(bytes, SkinSync.UploadAssetChunkC2S.MAX_CHUNK, (index, total, chunk) ->
                CommonNetwork.sendToServer(new SkinSync.UploadAssetChunkC2S(skinId, assetType, index, total, chunk))
        );
    }

    private static String takePendingSkinId() {
        String skinId = STATE.pendingSkinId;
        STATE.pendingSkinId = null;
        return skinId;
    }

    private static boolean tickSnapshotRequest() {
        if (STATE.snapshotDelay < 0) return false;
        if (--STATE.snapshotDelay > 0) return false;
        STATE.snapshotDelay = -1;
        return true;
    }

    private static final class State {
        private static final int SCAN_INTERVAL = 20;
        private String pendingSkinId;
        private boolean pendingUpload;
        private boolean sessionAnnounced;
        private int snapshotDelay = -1;
        private int scanTick;
        private final ConcurrentHashMap<String, Boolean> sentAssets = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, SkinSync.SkinChunkAccumulator> assetChunks = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, String> lastApplied = new ConcurrentHashMap<>();

        private void reset() {
            pendingSkinId = null;
            pendingUpload = false;
            sessionAnnounced = false;
            snapshotDelay = -1;
            scanTick = 0;
            sentAssets.clear();
            assetChunks.clear();
            lastApplied.clear();
        }
    }
}
