package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Skins.client.util.ViewBobbingSkinOverride;

import java.util.UUID;

public final class SkinSyncClient {
    private static final ClientSkinSyncState STATE = new ClientSkinSyncState();

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
            CommonNetwork.sendToServer(new SkinSync.RequestSnapshotC2S());
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
        saveSelection(client, id);
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
        sendSelection(client, resolveSelectedSkinId(client));
    }

    public static void onSyncAssetChunk(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        SkinAssetTransfer.acceptAssetChunk(STATE, uuid, skinId, assetType, index, total, data);
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

    private static void saveSelection(Minecraft client, String skinId) {
        UUID userId = getUserId(client);
        if (userId != null) SkinDataStore.setSelectedSkin(userId, skinId);
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
        if (client == null || client.level == null || ++STATE.scanTick < ClientSkinSyncState.SCAN_INTERVAL) return;
        STATE.scanTick = 0;
        for (var player : client.level.players()) {
            if (player == null) continue;
            String skinId = ClientSkinCache.get(player.getUUID());
            if (!SkinIdUtil.hasSkin(skinId) || skinId.equals(STATE.lastApplied.put(player.getUUID(), skinId))) continue;
            ClientSkinCache.setName(player.getScoreboardName(), skinId);
        }
    }

    private static void sendSelection(Minecraft client, String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        STATE.sessionAnnounced = true;
        CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(id));
        SkinAssetTransfer.sendAssets(client, STATE, id);
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
}
