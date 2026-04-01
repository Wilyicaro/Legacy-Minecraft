package wily.legacy.Skins.skin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ClientSkinSyncState {
    static final int SCAN_INTERVAL = 20;
    String pendingSkinId;
    boolean pendingUpload;
    boolean sessionAnnounced;
    int snapshotDelay = -1;
    int scanTick;
    final ConcurrentHashMap<String, Boolean> sentAssets = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, SkinChunkAccumulator> assetChunks = new ConcurrentHashMap<>();
    final ConcurrentHashMap<UUID, String> lastApplied = new ConcurrentHashMap<>();

    void reset() {
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
