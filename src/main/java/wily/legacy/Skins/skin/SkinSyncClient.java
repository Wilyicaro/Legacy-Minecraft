package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;

import java.util.Objects;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import java.util.UUID;

public final class SkinSyncClient {
    private static String pendingSkinId;
    private static volatile boolean pendingUploadRequest;
    private static volatile boolean announcedThisSession;

    private static volatile int snapshotRequestDelayTicks = -1;
    private static volatile boolean receivedAnySyncSinceJoin;

    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> SENT_ASSETS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.concurrent.ConcurrentHashMap<String, ChunkAccumulator> ASSET_CHUNKS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.concurrent.ConcurrentHashMap<UUID, String> LAST_APPLIED =
            new java.util.concurrent.ConcurrentHashMap<>();

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

        if (snapshotRequestDelayTicks >= 0) {        if (client != null && client.player != null && client.getConnection() != null) {
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
        snapshotRequestDelayTicks = 1;

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
        LAST_APPLIED.clear();

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
        ClientSkinAssets.clear();
        SENT_ASSETS.clear();
        ASSET_CHUNKS.clear();
        if (keep != null && keepVal != null && !keepVal.isBlank()) {
            ClientSkinCache.set(keep, keepVal);
        }
    }

    public static void requestSetSkin(Minecraft client, String skinId) {
        if (client == null) return;
        String id = skinId == null ? "" : skinId;

        try {
            UUID uid = client.getUser() != null ? client.getUser().getProfileId() : null;
            if (uid != null) ClientSkinPersistence.save(uid, id);
        } catch (Throwable ignored) {
        }

        try {
            if (client.player != null) ClientSkinCache.set(client.player.getUUID(), id);
            if (client.getUser() != null) ClientSkinCache.set(client.getUser().getProfileId(), id);
        } catch (Throwable ignored) {
        }

        if (client.getConnection() == null || client.player == null) {
            pendingSkinId = id;
            return;
        }

        try {
            CommonNetwork.sendToServer(new SkinSync.SetSkinC2S(id));

            try {
                if (!id.isBlank() && SENT_ASSETS.putIfAbsent(id, true) == null) {
                    byte[] texBytes = ClientSkinAssets.getTextureBytes(id);
                    byte[] modelBytes = ClientSkinAssets.getModelBytes(id);

                    SkinEntry e = null;
                    if (texBytes == null || modelBytes == null) {
                        try {
                            e = SkinPackLoader.getSkin(id);
                        } catch (Throwable ignored) {
                        }

                        if (texBytes == null) {
                            ResourceLocation texRl = e != null ? e.texture() : null;
                            if (texRl != null) texBytes = loadBytes(client, texRl);
                        }

                        if (modelBytes == null) {
                            ResourceLocation modelRl = resolveModelLocation(client, id, e);
                            modelBytes = loadBytes(client, modelRl);
                        }
                    }

                    sendChunks(id, 0, texBytes);
                    sendChunks(id, 1, modelBytes);
                }
            } catch (Throwable ignored) {
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

            try {
                if (!skinId.isBlank() && SENT_ASSETS.putIfAbsent(skinId, true) == null) {
                    byte[] texBytes = ClientSkinAssets.getTextureBytes(skinId);
                    byte[] modelBytes = ClientSkinAssets.getModelBytes(skinId);

                    SkinEntry e = null;
                    if (texBytes == null || modelBytes == null) {
                        try {
                            e = SkinPackLoader.getSkin(skinId);
                        } catch (Throwable ignored) {
                        }

                        if (texBytes == null) {
                            ResourceLocation texRl = e != null ? e.texture() : null;
                            if (texRl != null) texBytes = loadBytes(mc, texRl);
                        }

                        if (modelBytes == null) {
                            ResourceLocation modelRl = resolveModelLocation(mc, skinId, e);
                            modelBytes = loadBytes(mc, modelRl);
                        }
                    }

                    sendChunks(skinId, 0, texBytes);
                    sendChunks(skinId, 1, modelBytes);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private static ResourceLocation resolveModelLocation(Minecraft mc, String skinId, SkinEntry entry) {
        if (mc == null || skinId == null || skinId.isBlank()) return null;

        try {
            if (entry != null && entry.texture() != null) {
                ResourceLocation tex = entry.texture();
                String tp = tex.getPath();
                int idx = tp.indexOf("skinpacks/");
                if (idx >= 0) {
                    String after = tp.substring(idx + "skinpacks/".length());
                    int slash = after.indexOf('/');
                    if (slash > 0) {
                        String folder = after.substring(0, slash);
                        ResourceLocation cand = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), "skinpacks/" + folder + "/box_models/" + skinId + ".json");
                        if (mc.getResourceManager().getResource(cand).isPresent()) return cand;
                    }
                }

                ResourceLocation cand2 = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), "box_models/" + skinId + ".json");
                if (mc.getResourceManager().getResource(cand2).isPresent()) return cand2;
            }
        } catch (Throwable ignored) {
        }

        try {
            ResourceLocation cand3 = ResourceLocation.fromNamespaceAndPath("legacy", "box_models/" + skinId + ".json");
            if (mc.getResourceManager().getResource(cand3).isPresent()) return cand3;
        } catch (Throwable ignored) {
        }

        return ResourceLocation.fromNamespaceAndPath("legacy", "box_models/" + skinId + ".json");
    }

private static void sendChunks(String skinId, int type, byte[] bytes) {
        if (skinId == null || skinId.isBlank()) return;
        if (bytes == null) bytes = new byte[0];
        int max = SkinSync.UploadAssetChunkC2S.MAX_CHUNK;
        int total = bytes.length == 0 ? 1 : (bytes.length + max - 1) / max;

        for (int i = 0; i < total; i++) {
            int start = i * max;
            int end = Math.min(bytes.length, start + max);
            byte[] chunk;
            if (bytes.length == 0) {
                chunk = new byte[0];
            } else {
                int len = end - start;
                chunk = new byte[len];
                System.arraycopy(bytes, start, chunk, 0, len);
            }
            try {
                CommonNetwork.sendToServer(new SkinSync.UploadAssetChunkC2S(skinId, type, i, total, chunk));
            } catch (Throwable ignored) {
            }
        }
    }

private static byte[] loadBytes(Minecraft mc, ResourceLocation rl) {
        try {
            if (mc == null || rl == null) return new byte[0];
            Resource res = mc.getResourceManager().getResource(rl).orElse(null);
            if (res == null) return new byte[0];
            try (var in = res.open()) {
                return in.readAllBytes();
            }
        } catch (Throwable ignored) {
        }
        return new byte[0];
    }

    public static void onSyncAssets(UUID uuid, String skinId, byte[] texturePng, byte[] modelJson) {
        if (skinId == null || skinId.isBlank()) return;
        ClientSkinAssets.put(skinId, texturePng, modelJson);
        ClientSkinCache.set(uuid, skinId);
    }

    private static final class ChunkAccumulator {
        private final int total;
        private final byte[][] parts;
        private int received;

        private ChunkAccumulator(int total) {
            this.total = total;
            this.parts = new byte[total][];
            this.received = 0;
        }

        private void put(int index, byte[] data) {
            if (index < 0 || index >= total) return;
            if (parts[index] != null) return;
            parts[index] = data == null ? new byte[0] : data;
            received++;
        }

        private boolean complete() {
            return received >= total;
        }

        private byte[] assemble() {
            int len = 0;
            for (int i = 0; i < total; i++) {
                byte[] b = parts[i];
                if (b != null) len += b.length;
            }
            byte[] out = new byte[len];
            int off = 0;
            for (int i = 0; i < total; i++) {
                byte[] b = parts[i];
                if (b == null) continue;
                System.arraycopy(b, 0, out, off, b.length);
                off += b.length;
            }
            return out;
        }
    }

    public static void onSyncAssetChunk(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        if (skinId == null || skinId.isBlank()) return;
        if (total <= 0) return;

        String key = uuid + "|" + skinId + "|" + assetType;

        ChunkAccumulator acc = ASSET_CHUNKS.computeIfAbsent(key, k -> new ChunkAccumulator(total));
        acc.put(index, data);

        if (!acc.complete()) return;

        ASSET_CHUNKS.remove(key);

        byte[] full = acc.assemble();

        if (assetType == 0) {
            ClientSkinAssets.put(skinId, full, null);
        } else if (assetType == 1) {
            ClientSkinAssets.put(skinId, null, full);
        }

        ClientSkinCache.set(uuid, skinId);
    }

public static void onSyncSkin(UUID uuid, String skinId) {
        receivedAnySyncSinceJoin = true;
        ClientSkinCache.set(uuid, skinId);
    }
}
