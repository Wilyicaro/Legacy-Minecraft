package wily.legacy.Skins.skin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;
import wily.legacy.Skins.client.gui.PreviewPlayer;

import java.util.Objects;
import java.util.UUID;

public final class SkinSyncClient {
    private static String pendingSkinId;
    private static volatile boolean pendingUploadRequest;
    private static volatile boolean announcedThisSession;

    private static volatile int snapshotRequestDelayTicks = -1;
    private static volatile boolean receivedAnySyncSinceJoin;


    private static final java.util.concurrent.ConcurrentHashMap<UUID, String> LAST_APPLIED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static volatile long lastAnnounceMs = 0L;

    private static final java.util.concurrent.ConcurrentHashMap<String, CpmChunkAssembly> IN_CPM_CHUNKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private record CpmChunkAssembly(String skinId, int transferId, int totalChunks, byte[][] chunks,
                                    java.util.concurrent.atomic.AtomicInteger received,
                                    java.util.concurrent.atomic.AtomicLong lastMs) {
    }

    private SkinSyncClient() {
    }


    public static void initClient() {

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                UUID uid = mc.getUser().getProfileId();
                String persisted = ClientSkinPersistence.load(uid);
                if (persisted != null && !persisted.isBlank()) {
                    ClientSkinCache.set(uid, persisted);
                }
            }
        } catch (Throwable ignored) {
        }
    }


    public static void ensureInitialSkinLoaded(Minecraft mc) {
        if (mc == null || mc.getUser() == null) return;
        UUID uid = mc.getUser().getProfileId();
        String s = ClientSkinCache.get(uid);
        if (s == null || s.isBlank()) {
            s = ClientSkinPersistence.load(uid);
            if (s != null && !s.isBlank()) ClientSkinCache.set(uid, s);
        }
    }


    public static void postTick(Minecraft client) {

        try {
            if (client != null && client.player != null && client.getUser() != null) {
                UUID playerId = client.player.getUUID();
                if (ClientSkinCache.get(playerId) == null) {
                    UUID userId = client.getUser().getProfileId();
                    String s = ClientSkinCache.get(userId);
                    if (s != null && !s.isBlank()) ClientSkinCache.set(playerId, s);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            if (client != null && client.level != null) {
                for (var p : client.level.players()) {
                    if (p == null) continue;
                    String s = ClientSkinCache.get(p.getUUID());
                    if (s != null && !s.isBlank()) ClientSkinCache.setName(p.getScoreboardName(), s);

                    if (s != null) {
                        String last = LAST_APPLIED.get(p.getUUID());
                        if (!Objects.equals(last, s)) {
                            LAST_APPLIED.put(p.getUUID(), s);
                            try {
                                CpmModelManager.applyToProfile(p.getGameProfile(), s);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        if (pendingUploadRequest) {
            if (client != null && client.player != null && client.getConnection() != null) {
                pendingUploadRequest = false;
                announcedThisSession = false;
                onRequestSkinUpload();
            }
        }

        if (snapshotRequestDelayTicks >= 0) {
            if (FactoryAPI.getLoader().isForgeLike() && receivedAnySyncSinceJoin) {

                snapshotRequestDelayTicks = -1;
            } else if (client != null && client.player != null && client.getConnection() != null) {
                snapshotRequestDelayTicks--;
                if (snapshotRequestDelayTicks <= 0) {
                    snapshotRequestDelayTicks = -1;
                    try {
                        CommonNetwork.sendToServer(new SkinSync.RequestSnapshotC2S());
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        if (!announcedThisSession) {
            if (client != null && client.player != null && client.getConnection() != null) {
                announcedThisSession = true;
                onRequestSkinUpload();
            }
        }

        if (pendingSkinId != null) {
            if (client == null || client.getConnection() == null || client.player == null) return;
            String skinId = pendingSkinId;
            pendingSkinId = null;
            requestSetSkin(client, skinId);
        }

    }


    public static void onClientJoin() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        receivedAnySyncSinceJoin = false;
        snapshotRequestDelayTicks = 40;

        if (pendingSkinId != null) {
            String skinId = pendingSkinId;
            pendingSkinId = null;
            requestSetSkin(client, skinId);
            return;
        }

        try {
            if (client.getUser() != null && client.player != null) {
                UUID uid = client.getUser().getProfileId();
                String persisted = ClientSkinCache.get(uid);
                if (persisted == null || persisted.isBlank()) {
                    persisted = ClientSkinPersistence.load(uid);
                    if (persisted != null && !persisted.isBlank()) ClientSkinCache.set(uid, persisted);
                }
                if (persisted != null && !persisted.isBlank()) {

                    ClientSkinCache.set(client.player.getUUID(), persisted);
                    try {
                        CpmModelManager.applyToProfile(client.player.getGameProfile(), persisted);
                    } catch (Throwable ignored2) {
                    }

                    if (client.getConnection() != null) {
                        announcedThisSession = true;
                        onRequestSkinUpload();
                    } else {
                        pendingUploadRequest = true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }


    public static void onClientDisconnect() {
        pendingSkinId = null;
        pendingUploadRequest = false;
        announcedThisSession = false;
        snapshotRequestDelayTicks = -1;
        receivedAnySyncSinceJoin = false;
        lastAnnounceMs = 0L;
        LAST_APPLIED.clear();
        IN_CPM_CHUNKS.clear();

        String keepVal = null;
        UUID keep = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                keep = mc.getUser().getProfileId();
                keepVal = ClientSkinCache.get(keep);
            }
        } catch (Throwable ignored) {
        }

        ClientSkinCache.clear();
        if (keep != null && keepVal != null && !keepVal.isBlank()) {
            ClientSkinCache.set(keep, keepVal);
        }
        GuiCpmPreviewCache.clearCaches();
        PreviewPlayer.clear();
        CpmModelManager.invalidateAll();
    }


    public static void requestSetSkin(Minecraft minecraft, String skinId) {
        if (minecraft != null && minecraft.getConnection() != null && minecraft.player != null) {
            try {
                UUID uid = minecraft.getUser() != null ? minecraft.getUser().getProfileId() : minecraft.player.getUUID();
                ClientSkinPersistence.save(uid, skinId);
                if (uid != null) ClientSkinCache.set(uid, skinId);
            } catch (Throwable ignored) {
            }

            try {
                CpmModelManager.applyToProfile(minecraft.player.getGameProfile(), skinId == null ? "" : skinId);
            } catch (Throwable ignored) {
            }

            if (SkinIdUtil.isCpm(skinId) && skinId.length() > SkinIds.CPM_PREFIX.length()) {
                try {
                    ClientSkinCache.set(minecraft.player.getUUID(), skinId);
                } catch (Throwable ignored) {
                }

                byte[] modelFile = CpmModelManager.readModelFileBytesForSelection(skinId);
                if (modelFile != null && modelFile.length > 0) {
                    try {
                        CpmModelManager.cacheNetworkModelFile(minecraft.player.getUUID(), skinId, modelFile);
                    } catch (Throwable ignored) {
                    }

                    if (modelFile.length <= SkinSync.MAX_CPM_MODEL_FILE_BYTES) {
                        CommonNetwork.sendToServer(new SkinSync.SetCpmModelC2S(skinId, modelFile));
                        return;
                    }

                    if (modelFile.length <= SkinSync.MAX_CPM_MODEL_TOTAL_BYTES) {
                        CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(skinId));
                        sendCpmChunksToServer(skinId, modelFile);
                        return;
                    }
                }
                CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(skinId));
                return;
            }

            CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(skinId));
        } else {

            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getUser() != null) {
                    UUID uid = mc.getUser().getProfileId();
                    ClientSkinPersistence.save(uid, skinId);
                    ClientSkinCache.set(uid, skinId);
                }
            } catch (Throwable ignored) {
            }
            pendingSkinId = skinId;
        }
    }

    public static void onSyncSkin(UUID uuid, String skinId) {
        receivedAnySyncSinceJoin = true;
        ClientSkinCache.set(uuid, skinId);
        applyToAnyKnownProfile(uuid, skinId);
    }

    public static void onSyncCpmModel(UUID uuid, String skinId, byte[] modelFileBytes) {
        receivedAnySyncSinceJoin = true;
        ClientSkinCache.set(uuid, skinId);
        CpmModelManager.cacheNetworkModelFile(uuid, skinId, modelFileBytes);
        applyToAnyKnownProfile(uuid, skinId);
    }

    public static void onRequestCpmUpload(String requestedSkinId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (!SkinIdUtil.isCpm(requestedSkinId)) return;

            String current = ClientSkinCache.get(mc.player.getUUID());
            if (current == null || !current.equals(requestedSkinId)) return;

            byte[] modelFile = CpmModelManager.readModelFileBytesForSelection(requestedSkinId);
            if (modelFile == null || modelFile.length == 0) return;

            if (modelFile.length <= SkinSync.MAX_CPM_MODEL_FILE_BYTES) {
                CommonNetwork.sendToServer(new SkinSync.SetCpmModelC2S(requestedSkinId, modelFile));
            } else if (modelFile.length <= SkinSync.MAX_CPM_MODEL_TOTAL_BYTES) {
                sendCpmChunksToServer(requestedSkinId, modelFile);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onRequestSkinUpload() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.getConnection() == null) {
                pendingUploadRequest = true;
                return;
            }

            UUID pid = mc.player.getUUID();
            String skinId = ClientSkinCache.get(pid);
            UUID uid = null;
            if (mc.getUser() != null) uid = mc.getUser().getProfileId();

            if (skinId == null || skinId.isBlank()) {
                if (uid != null) skinId = ClientSkinCache.get(uid);
            }
            if (skinId == null || skinId.isBlank()) {
                if (uid != null) {
                    String persisted = ClientSkinPersistence.load(uid);
                    if (persisted != null && !persisted.isBlank()) {
                        skinId = persisted;

                        ClientSkinCache.set(uid, persisted);
                        ClientSkinCache.set(pid, persisted);
                    }
                }
            }
            if (skinId == null) skinId = "";

            CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(skinId));
        } catch (Throwable ignored) {
        }
    }


    public static void onSyncCpmModelChunk(UUID uuid, String skinId, int transferId, int totalChunks, int chunkIndex, byte[] chunkBytes) {
        if (uuid == null || skinId == null) return;
        if (totalChunks <= 0 || totalChunks > 10_000) return;
        if (chunkIndex < 0 || chunkIndex >= totalChunks) return;

        String key = uuid + "|" + transferId;
        long now = System.currentTimeMillis();

        CpmChunkAssembly asm = IN_CPM_CHUNKS.compute(key, (k, existing) -> {
            if (existing == null || existing.totalChunks != totalChunks || existing.transferId != transferId || !Objects.equals(existing.skinId, skinId)) {
                return new CpmChunkAssembly(skinId, transferId, totalChunks, new byte[totalChunks][], new java.util.concurrent.atomic.AtomicInteger(0), new java.util.concurrent.atomic.AtomicLong(now));
            }
            existing.lastMs.set(now);
            return existing;
        });

        if (now - asm.lastMs.get() > 20_000L) {
            IN_CPM_CHUNKS.remove(key);
            return;
        }

        if (asm.chunks[chunkIndex] == null) {
            asm.chunks[chunkIndex] = chunkBytes == null ? new byte[0] : chunkBytes;
            asm.received.incrementAndGet();
        }

        if (asm.received.get() >= totalChunks) {
            try {
                int total = 0;
                for (byte[] c : asm.chunks) total += (c == null ? 0 : c.length);
                if (total <= 0 || total > SkinSync.MAX_CPM_MODEL_TOTAL_BYTES) {
                    IN_CPM_CHUNKS.remove(key);
                    return;
                }
                byte[] full = new byte[total];
                int pos = 0;
                for (byte[] c : asm.chunks) {
                    if (c == null || c.length == 0) continue;
                    System.arraycopy(c, 0, full, pos, c.length);
                    pos += c.length;
                }
                IN_CPM_CHUNKS.remove(key);
                onSyncCpmModel(uuid, skinId, full);
            } catch (Throwable ignored) {
                IN_CPM_CHUNKS.remove(key);
            }
        }
    }

    private static void sendCpmChunksToServer(String skinId, byte[] modelFile) {
        try {
            if (skinId == null || modelFile == null) return;
            int chunkSize = SkinSync.MAX_CPM_CHUNK_BYTES;
            if (chunkSize <= 0) return;
            int totalChunks = (modelFile.length + chunkSize - 1) / chunkSize;
            int transferId = java.util.concurrent.ThreadLocalRandom.current().nextInt();

            for (int i = 0; i < totalChunks; i++) {
                int off = i * chunkSize;
                int len = Math.min(chunkSize, modelFile.length - off);
                byte[] chunk = new byte[len];
                System.arraycopy(modelFile, off, chunk, 0, len);
                CommonNetwork.sendToServer(new SkinSync.SetCpmModelChunkC2S(skinId, transferId, totalChunks, i, chunk));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyToAnyKnownProfile(UUID uuid, String skinId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            GameProfile gp = null;
            if (mc.player != null && mc.player.getUUID().equals(uuid)) {
                gp = mc.player.getGameProfile();
            } else if (mc.level != null) {
                var p = mc.level.getPlayerByUUID(uuid);
                if (p != null) gp = p.getGameProfile();
            }
            if (gp == null && SkinIdUtil.isCpm(skinId)) {

                gp = new GameProfile(uuid, "synced");
            }
            if (gp != null) {
                CpmModelManager.applyToProfile(gp, skinId == null ? "" : skinId);
            }
        } catch (Throwable ignored) {
        }
    }
}
