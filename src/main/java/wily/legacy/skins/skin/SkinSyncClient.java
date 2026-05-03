package wily.legacy.skins.skin;

import net.minecraft.client.Minecraft;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.skins.client.util.ViewBobbingSkinOverride;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSyncClient {
    private static final State STATE = new State();

    private SkinSyncClient() {
    }

    public static void init() {
        UUID userId = getUserId(Minecraft.getInstance());
        if (userId != null) loadStoredSelection(userId);
    }

    public static void postTick(Minecraft client) {
        syncLocalPlayerCache(client);
        refreshKnownPlayerNames(client);
        boolean connected = isConnected(client);
        if (connected && STATE.tickUploadRetry()) {
            onRequestSkinUpload();
        }
        Boolean snapshotForce = connected ? STATE.tickSnapshotRequest() : null;
        if (snapshotForce != null) requestSnapshot(client, snapshotForce);
        if (connected && !STATE.sessionAnnounced) onRequestSkinUpload();
        if (!connected) return;
        PendingSelection pendingSelection = takePendingSelection();
        if (pendingSelection != null) requestSetSkin(client, pendingSelection.packId(), pendingSelection.skinId());
    }

    public static void onClientJoin() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        STATE.sentAssets.clear();
        STATE.requestSnapshot(1, true);
        PendingSelection pendingSelection = takePendingSelection();
        if (pendingSelection != null) {
            requestSetSkin(client, pendingSelection.packId(), pendingSelection.skinId());
            return;
        }
        if (client.player == null) return;
        String skinId = resolveSelectedSkinId(client);
        if (!SkinIdUtil.hasSkin(skinId)) return;
        if (isConnected(client)) onRequestSkinUpload();
        else STATE.requestUpload();
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
        requestSetSkin(client, null, skinId);
    }

    public static void requestSetSkin(Minecraft client, String packId, String skinId) {
        if (client == null) return;
        String id = SkinIdUtil.normalize(skinId);
        String selectedPackId = SkinIdUtil.trimToNull(packId);
        UUID userId = getUserId(client);
        if (userId != null) SkinDataStore.setSelectedSkin(userId, id, selectedPackId);
        cacheLocalSelection(client, id);
        if (!isConnected(client)) {
            STATE.pendingSkinId = id;
            STATE.pendingPackId = selectedPackId;
            return;
        }
        sendSelection(client, id);
    }

    public static void onRequestSkinUpload() {
        Minecraft client = Minecraft.getInstance();
        if (!isConnected(client)) {
            STATE.requestUpload();
            return;
        }
        sendSelection(client, resolveSelectedSkinId(client), true);
    }

    public static void onSkinAssetsReloaded(Minecraft client) {
        if (client == null) return;
        STATE.sentAssets.clear();
        if (!isConnected(client)) return;
        String skinId = resolveSelectedSkinId(client);
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        sendSelection(client, skinId);
        requestSnapshot(client, true);
    }

    public static void onSyncAssetChunk(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        if (!SkinIdUtil.hasSkin(skinId) || total <= 0) return;
        String key = uuid + "|" + skinId + "|" + assetType;
        SkinSync.SkinChunkAccumulator accumulator = STATE.assetChunks.computeIfAbsent(key, ignored -> new SkinSync.SkinChunkAccumulator(total));
        accumulator.put(index, data);
        if (!accumulator.isComplete()) return;
        STATE.assetChunks.remove(key);
        byte[] bytes = accumulator.assemble();
        if (assetType == SkinSync.ASSET_TEXTURE && bytes.length == 0) return;
        String assetKey = ClientSkinAssets.runtimeAssetKey(uuid, skinId);
        if (assetType == SkinSync.ASSET_TEXTURE) ClientSkinAssets.putTexture(assetKey, bytes);
        else if (assetType == SkinSync.ASSET_MODEL) ClientSkinAssets.putModel(assetKey, bytes);
        else if (assetType == SkinSync.ASSET_METADATA) ClientSkinAssets.putMetadata(skinId, bytes);
        else if (assetType == SkinSync.ASSET_CAPE) ClientSkinAssets.putCape(assetKey, bytes);
        ClientSkinCache.set(uuid, skinId);
    }

    public static void onSyncSkin(UUID uuid, String skinId) {
        ClientSkinCache.set(uuid, skinId);
    }

    private static UUID getUserId(Minecraft client) {
        return client == null || client.getUser() == null ? null : client.getUser().getProfileId();
    }

    private static boolean isConnected(Minecraft client) {
        return client != null && client.player != null && client.getConnection() != null;
    }

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
        if (missingSkin) STATE.requestSnapshotRetry();
    }

    private static void requestSnapshot(Minecraft client, boolean force) {
        if (!Legacy4JClient.hasModOnServer()) return;
        CommonNetwork.sendToServer(new SkinSync.RequestSnapshotC2S());
    }

    private static void sendSelection(Minecraft client, String skinId) {
        sendSelection(client, skinId, false);
    }

    private static void sendSelection(Minecraft client, String skinId, boolean forceAssets) {
        String id = SkinFairness.effectiveSkinId(client, skinId);
        STATE.sessionAnnounced = true;
        if (!Legacy4JClient.hasModOnServer()) return;
        CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(id));
        sendAssets(client, id, forceAssets);
    }

    private static void sendAssets(Minecraft client, String skinId, boolean force) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (!force && STATE.sentAssets.containsKey(skinId)) return;
        ClientSkinAssets.AssetData assets = ClientSkinAssets.resolveAssetData(client, skinId);
        if (!assets.hasTexture()) {
            STATE.requestUploadRetry();
            return;
        }
        if (force) STATE.sentAssets.put(skinId, Boolean.TRUE);
        else if (STATE.sentAssets.putIfAbsent(skinId, Boolean.TRUE) != null) return;
        sendAssetChunks(skinId, SkinSync.ASSET_TEXTURE, assets.texture());
        sendAssetChunks(skinId, SkinSync.ASSET_MODEL, assets.model());
        sendAssetChunks(skinId, SkinSync.ASSET_METADATA, assets.metadata());
        sendAssetChunks(skinId, SkinSync.ASSET_CAPE, assets.cape());
    }

    private static void sendAssetChunks(String skinId, int assetType, byte[] bytes) {
        if (!SkinIdUtil.hasSkin(skinId)) return;
        SkinSync.forEachChunk(bytes, SkinSync.UploadAssetChunkC2S.MAX_CHUNK, (index, total, chunk) ->
                CommonNetwork.sendToServer(new SkinSync.UploadAssetChunkC2S(skinId, assetType, index, total, chunk))
        );
    }

    private static PendingSelection takePendingSelection() {
        String skinId = STATE.pendingSkinId;
        String packId = STATE.pendingPackId;
        STATE.pendingSkinId = null;
        STATE.pendingPackId = null;
        return skinId == null ? null : new PendingSelection(packId, skinId);
    }

    private static final class State {
        private static final int SCAN_INTERVAL = 20;
        private static final int UPLOAD_RETRY_INTERVAL = 20;
        private final ConcurrentHashMap<String, Boolean> sentAssets = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, SkinSync.SkinChunkAccumulator> assetChunks = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, String> lastApplied = new ConcurrentHashMap<>();
        private String pendingSkinId;
        private String pendingPackId;
        private boolean pendingUpload;
        private boolean sessionAnnounced;
        private boolean snapshotForce;
        private int snapshotDelay = -1;
        private int uploadRetryDelay;
        private int scanTick;

        private void requestUpload() {
            pendingUpload = true;
            uploadRetryDelay = 0;
        }

        private void requestUploadRetry() {
            pendingUpload = true;
            if (uploadRetryDelay <= 0) uploadRetryDelay = UPLOAD_RETRY_INTERVAL;
        }

        private void requestSnapshotRetry() {
            requestSnapshot(SCAN_INTERVAL, false);
        }

        private void requestSnapshot(int delay, boolean force) {
            if (!force && snapshotDelay >= 0) return;
            snapshotDelay = delay;
            snapshotForce = force;
        }

        private boolean tickUploadRetry() {
            if (!pendingUpload) return false;
            if (uploadRetryDelay > 0 && --uploadRetryDelay > 0) return false;
            pendingUpload = false;
            uploadRetryDelay = 0;
            return true;
        }

        private Boolean tickSnapshotRequest() {
            if (snapshotDelay < 0) return null;
            if (--snapshotDelay > 0) return null;
            boolean force = snapshotForce;
            snapshotDelay = -1;
            snapshotForce = false;
            return force;
        }

        private void reset() {
            pendingSkinId = null;
            pendingPackId = null;
            pendingUpload = false;
            sessionAnnounced = false;
            snapshotForce = false;
            snapshotDelay = -1;
            uploadRetryDelay = 0;
            scanTick = 0;
            sentAssets.clear();
            assetChunks.clear();
            lastApplied.clear();
        }
    }

    private record PendingSelection(String packId, String skinId) {
    }
}
