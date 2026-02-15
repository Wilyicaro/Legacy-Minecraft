package wily.legacy.Skins.skin;


/**
 * Console skins / CPM glue.
 */

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Skins.client.cpm.CpmModelManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSync {
    public static final String MODID = "consoleskins";
    public static final String CPM_PREFIX = "cpm:";

    private static final int MAX_SKIN_ID_LEN = 256;


    public static final int MAX_CPM_MODEL_FILE_BYTES = 30_000;


    public static final int MAX_CPM_MODEL_TOTAL_BYTES = 2 * 1024 * 1024;


    public static final int MAX_CPM_CHUNK_BYTES = 24_000;

    private static final Map<UUID, String> SERVER_SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, CpmEntry> SERVER_CPM = new ConcurrentHashMap<>();

    private record CpmEntry(String skinId, byte[] modelFileBytes) {
    }

    private static final ConcurrentHashMap<String, Long> LAST_CPM_REQUEST_MS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Long> LAST_SNAPSHOT_REQUEST_MS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Long> LAST_SNAPSHOT_SENT_MS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, CpmUploadAssembly> CPM_UPLOADS = new ConcurrentHashMap<>();

    private record CpmUploadAssembly(String skinId, int transferId, int totalChunks, byte[][] chunks,
                                     java.util.concurrent.atomic.AtomicInteger received,
                                     java.util.concurrent.atomic.AtomicLong lastMs) {
    }

    private SkinSync() {
    }

    public static boolean isCpm(String skinId) {
        return skinId != null && skinId.startsWith(CPM_PREFIX);
    }

    public static void remove(UUID uuid) {
        if (uuid == null) return;
        SERVER_SKINS.remove(uuid);
        SERVER_CPM.remove(uuid);
    }

    public static Map<UUID, String> snapshot() {
        return new LinkedHashMap<>(SERVER_SKINS);
    }

    public static String getServerSkinId(UUID uuid) {
        return uuid == null ? null : SERVER_SKINS.get(uuid);
    }

    public static byte[] getServerCpmModelFile(UUID uuid, String skinId) {
        CpmEntry e = SERVER_CPM.get(uuid);
        if (e == null) return null;
        if (!Objects.equals(e.skinId, skinId)) return null;
        return e.modelFileBytes;
    }


    public static void sendCpmModelToPlayer(ServerPlayer target, UUID ownerUuid, String skinId, byte[] modelBytes) {
        if (target == null || ownerUuid == null || modelBytes == null || modelBytes.length == 0) return;

        if (modelBytes.length <= MAX_CPM_MODEL_FILE_BYTES) {
            CommonNetwork.sendToPlayer(target, new SyncCpmModelS2C(ownerUuid, skinId, modelBytes));
            return;
        }

        if (modelBytes.length > MAX_CPM_MODEL_TOTAL_BYTES) return;
        int chunkSize = MAX_CPM_CHUNK_BYTES;
        if (chunkSize <= 0) return;
        int totalChunks = (modelBytes.length + chunkSize - 1) / chunkSize;
        int transferId = java.util.concurrent.ThreadLocalRandom.current().nextInt();

        for (int i = 0; i < totalChunks; i++) {
            int off = i * chunkSize;
            int len = Math.min(chunkSize, modelBytes.length - off);
            byte[] chunk = new byte[len];
            System.arraycopy(modelBytes, off, chunk, 0, len);
            CommonNetwork.sendToPlayer(target, new SyncCpmModelChunkS2C(ownerUuid, skinId, transferId, totalChunks, i, chunk));
        }
    }

    public static void requestCpmModelFrom(ServerPlayer owner, String skinId) {
        if (owner == null || !isCpm(skinId)) return;

        long now = System.currentTimeMillis();
        String key = owner.getUUID() + "|" + skinId;
        long last = LAST_CPM_REQUEST_MS.getOrDefault(key, 0L);
        if (now - last < 5_000L) return;
        LAST_CPM_REQUEST_MS.put(key, now);

        CommonNetwork.sendToPlayer(owner, new RequestCpmModelS2C(skinId));
    }


    public record SetSkinC2S(String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SetSkinC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_set_skin_c2s"), SetSkinC2S::new);

        public SetSkinC2S(CommonNetwork.PlayBuf buf) {
            this(readSkinId(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            writeSkinId(buf, skinId);
        }

        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
            if (server == null) return;

            String id = normalizeSkinId(skinId);
            SERVER_SKINS.put(player.getUUID(), id);

            boolean isCpm = isCpm(id);
            if (!isCpm) {
                SERVER_CPM.remove(player.getUUID());
            } else {
                CpmEntry e = SERVER_CPM.get(player.getUUID());
                if (e != null && !Objects.equals(e.skinId, id)) SERVER_CPM.remove(player.getUUID());
            }

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                CommonNetwork.sendToPlayer(other, new SyncSkinS2C(player.getUUID(), id));
            }

            boolean recentlyJoined = player.tickCount < 200;
            long now = System.currentTimeMillis();
            long lastSnap = LAST_SNAPSHOT_SENT_MS.getOrDefault(player.getUUID(), 0L);
            boolean allowSnapshot = recentlyJoined && (now - lastSnap > 2_000L);
            if (allowSnapshot) {
                LAST_SNAPSHOT_SENT_MS.put(player.getUUID(), now);
                for (Map.Entry<UUID, String> e : snapshot().entrySet()) {
                    UUID who = e.getKey();
                    if (who == null || who.equals(player.getUUID())) continue;
                    String otherSkin = e.getValue();
                    CommonNetwork.sendToPlayer(player, new SyncSkinS2C(who, otherSkin));
                    byte[] cpmFile = getServerCpmModelFile(who, otherSkin);
                    if (cpmFile != null && cpmFile.length > 0) {
                        sendCpmModelToPlayer(player, who, otherSkin, cpmFile);
                    } else if (isCpm(otherSkin)) {
                        ServerPlayer owner = server.getPlayerList().getPlayer(who);
                        if (owner != null) requestCpmModelFrom(owner, otherSkin);
                    }
                }
            }

            if (isCpm && getServerCpmModelFile(player.getUUID(), id) == null) {
                requestCpmModelFrom(player, id);
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record RequestSnapshotC2S() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSnapshotC2S> ID =

                CommonNetwork.Identifier.create(
                        Legacy4J.createModLocation("skins_request_snapshot_c2s"),
                        (CommonNetwork.PlayBuf buf) -> new RequestSnapshotC2S(buf)
                );

        public RequestSnapshotC2S(CommonNetwork.PlayBuf buf) {
            this();
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {

        }

        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer requester)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(requester);
            if (server == null) return;

            long now = System.currentTimeMillis();
            Long last = LAST_SNAPSHOT_REQUEST_MS.get(requester.getUUID());
            if (last != null && now - last < 2_000L) return;
            LAST_SNAPSHOT_REQUEST_MS.put(requester.getUUID(), now);

            for (Map.Entry<UUID, String> e : snapshot().entrySet()) {
                UUID who = e.getKey();
                if (who == null) continue;
                String skinId = e.getValue();
                CommonNetwork.sendToPlayer(requester, new SyncSkinS2C(who, skinId));

                byte[] cpmFile = getServerCpmModelFile(who, skinId);
                if (cpmFile != null && cpmFile.length > 0) {
                    sendCpmModelToPlayer(requester, who, skinId, cpmFile);
                } else if (isCpm(skinId)) {
                    ServerPlayer owner = server.getPlayerList().getPlayer(who);
                    if (owner != null) requestCpmModelFrom(owner, skinId);
                }
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record SetCpmModelC2S(String skinId, byte[] modelFileBytes) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SetCpmModelC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_set_cpm_model_c2s"), SetCpmModelC2S::new);

        public SetCpmModelC2S(CommonNetwork.PlayBuf buf) {
            this(readSkinId(buf), readByteArrayLimited(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            writeSkinId(buf, skinId);
            writeByteArrayLimited(buf, modelFileBytes);
        }

        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
            if (server == null) return;

            String id = normalizeSkinId(skinId);
            byte[] bytes = modelFileBytes == null ? new byte[0] : modelFileBytes;

            SERVER_SKINS.put(player.getUUID(), id);
            if (bytes.length > 0) SERVER_CPM.put(player.getUUID(), new CpmEntry(id, bytes));
            else SERVER_CPM.remove(player.getUUID());

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                CommonNetwork.sendToPlayer(other, new SyncSkinS2C(player.getUUID(), id));
                if (bytes.length > 0) {
                    sendCpmModelToPlayer(other, player.getUUID(), id, bytes);
                }
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record SetCpmModelChunkC2S(String skinId, int transferId, int totalChunks, int chunkIndex, byte[] chunkBytes)
            implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SetCpmModelChunkC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_set_cpm_model_chunk_c2s"), SetCpmModelChunkC2S::new);

        public SetCpmModelChunkC2S(CommonNetwork.PlayBuf buf) {
            this(readSkinId(buf), buf.get().readInt(), buf.get().readVarInt(), buf.get().readVarInt(), readByteArrayLimited(buf, MAX_CPM_CHUNK_BYTES));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            writeSkinId(buf, skinId);
            buf.get().writeInt(transferId);
            buf.get().writeVarInt(totalChunks);
            buf.get().writeVarInt(chunkIndex);
            writeByteArrayLimited(buf, chunkBytes, MAX_CPM_CHUNK_BYTES);
        }

        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
            if (server == null) return;

            String id = normalizeSkinId(skinId);
            if (!isCpm(id)) return;
            if (totalChunks <= 0 || totalChunks > 10_000) return;
            if (chunkIndex < 0 || chunkIndex >= totalChunks) return;

            long now = System.currentTimeMillis();
            CpmUploadAssembly asm = CPM_UPLOADS.compute(player.getUUID(), (k, existing) -> {
                if (existing == null || existing.transferId != transferId || existing.totalChunks != totalChunks || !Objects.equals(existing.skinId, id)) {
                    return new CpmUploadAssembly(id, transferId, totalChunks, new byte[totalChunks][], new java.util.concurrent.atomic.AtomicInteger(0), new java.util.concurrent.atomic.AtomicLong(now));
                }
                existing.lastMs.set(now);
                return existing;
            });

            if (now - asm.lastMs.get() > 20_000L) {
                CPM_UPLOADS.remove(player.getUUID());
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
                    if (total <= 0 || total > MAX_CPM_MODEL_TOTAL_BYTES) {
                        CPM_UPLOADS.remove(player.getUUID());
                        return;
                    }
                    byte[] full = new byte[total];
                    int pos = 0;
                    for (byte[] c : asm.chunks) {
                        if (c == null || c.length == 0) continue;
                        System.arraycopy(c, 0, full, pos, c.length);
                        pos += c.length;
                    }
                    CPM_UPLOADS.remove(player.getUUID());

                    SERVER_SKINS.put(player.getUUID(), id);
                    SERVER_CPM.put(player.getUUID(), new CpmEntry(id, full));

                    for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                        CommonNetwork.sendToPlayer(other, new SyncSkinS2C(player.getUUID(), id));
                        sendCpmModelToPlayer(other, player.getUUID(), id, full);
                    }
                } catch (Throwable ignored) {
                    CPM_UPLOADS.remove(player.getUUID());
                }
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record SyncSkinS2C(UUID uuid, String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SyncSkinS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_sync_skin_s2c"), SyncSkinS2C::new);

        public SyncSkinS2C(CommonNetwork.PlayBuf buf) {
            this(buf.get().readUUID(), readSkinId(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeUUID(uuid);
            writeSkinId(buf, skinId);
        }

        @Override
        public void apply(Context context) {

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(() -> SkinSyncClient.onSyncSkin(uuid, skinId));
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record SyncCpmModelS2C(UUID uuid, String skinId, byte[] modelFileBytes) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SyncCpmModelS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_sync_cpm_model_s2c"), SyncCpmModelS2C::new);

        public SyncCpmModelS2C(CommonNetwork.PlayBuf buf) {
            this(buf.get().readUUID(), readSkinId(buf), readByteArrayLimited(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeUUID(uuid);
            writeSkinId(buf, skinId);
            writeByteArrayLimited(buf, modelFileBytes);
        }

        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(() -> SkinSyncClient.onSyncCpmModel(uuid, skinId, modelFileBytes));
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record SyncCpmModelChunkS2C(UUID uuid, String skinId, int transferId, int totalChunks, int chunkIndex,
                                       byte[] chunkBytes)
            implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SyncCpmModelChunkS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_sync_cpm_model_chunk_s2c"), SyncCpmModelChunkS2C::new);

        public SyncCpmModelChunkS2C(CommonNetwork.PlayBuf buf) {
            this(buf.get().readUUID(), readSkinId(buf), buf.get().readInt(), buf.get().readVarInt(), buf.get().readVarInt(), readByteArrayLimited(buf, MAX_CPM_CHUNK_BYTES));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeUUID(uuid);
            writeSkinId(buf, skinId);
            buf.get().writeInt(transferId);
            buf.get().writeVarInt(totalChunks);
            buf.get().writeVarInt(chunkIndex);
            writeByteArrayLimited(buf, chunkBytes, MAX_CPM_CHUNK_BYTES);
        }

        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(() -> SkinSyncClient.onSyncCpmModelChunk(uuid, skinId, transferId, totalChunks, chunkIndex, chunkBytes));
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record RequestCpmModelS2C(String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestCpmModelS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("skins_request_cpm_model_s2c"), RequestCpmModelS2C::new);

        public RequestCpmModelS2C(CommonNetwork.PlayBuf buf) {
            this(readSkinId(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            writeSkinId(buf, skinId);
        }

        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(() -> SkinSyncClient.onRequestCpmUpload(skinId));
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }


    public record RequestSkinS2C() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSkinS2C> ID =

                CommonNetwork.Identifier.create(
                        Legacy4J.createModLocation("skins_request_skin_s2c"),
                        (java.util.function.Function<CommonNetwork.PlayBuf, RequestSkinS2C>) RequestSkinS2C::new
                );

        public RequestSkinS2C(CommonNetwork.PlayBuf buf) {
            this();
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {

        }

        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(SkinSyncClient::onRequestSkinUpload);
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }

    private static String normalizeSkinId(String skinId) {
        String id = skinId == null ? "" : skinId;
        if (id.length() > MAX_SKIN_ID_LEN) id = id.substring(0, MAX_SKIN_ID_LEN);
        return id;
    }

    private static void writeSkinId(CommonNetwork.PlayBuf buf, String skinId) {
        buf.get().writeUtf(normalizeSkinId(skinId), MAX_SKIN_ID_LEN);
    }

    private static String readSkinId(CommonNetwork.PlayBuf buf) {
        return buf.get().readUtf(MAX_SKIN_ID_LEN);
    }

    private static void writeByteArrayLimited(CommonNetwork.PlayBuf buf, byte[] data) {
        writeByteArrayLimited(buf, data, MAX_CPM_MODEL_FILE_BYTES);
    }

    private static byte[] readByteArrayLimited(CommonNetwork.PlayBuf buf) {
        return readByteArrayLimited(buf, MAX_CPM_MODEL_FILE_BYTES);
    }

    private static void writeByteArrayLimited(CommonNetwork.PlayBuf buf, byte[] data, int limit) {
        if (data == null || data.length == 0) {
            buf.get().writeVarInt(0);
            return;
        }
        int lim = Math.max(0, limit);

        if (data.length > lim) {
            buf.get().writeVarInt(0);
            return;
        }
        buf.get().writeVarInt(data.length);
        buf.get().writeBytes(data, 0, data.length);
    }

    private static byte[] readByteArrayLimited(CommonNetwork.PlayBuf buf, int limit) {
        int len;
        try {
            len = buf.get().readVarInt();
        } catch (Throwable t) {
            int rem = buf.get().readableBytes();
            if (rem > 0) buf.get().skipBytes(rem);
            return new byte[0];
        }
        if (len <= 0) return new byte[0];
        int rem = buf.get().readableBytes();
        if (len > rem) len = rem;
        int lim = Math.max(0, limit);
        if (len > lim) {
            if (rem > 0) buf.get().skipBytes(rem);
            return new byte[0];
        }
        byte[] out = new byte[len];
        buf.get().readBytes(out);
        return out;
    }
}
