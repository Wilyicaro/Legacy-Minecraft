package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSync {
    public static final String MODID = Legacy4J.MOD_ID;
    public static final String ASSET_NS = Legacy4J.MOD_ID;

    private static final int MAX_SKIN_ID_LEN = 256;

    private static final Map<UUID, String> SERVER_SKINS = new ConcurrentHashMap<>();

    private static final Map<String, byte[]> SERVER_ASSETS = new ConcurrentHashMap<>();
    private static final Map<String, ServerChunkAccumulator> SERVER_ACC = new ConcurrentHashMap<>();

    private SkinSync() {
    }

    public static void remove(UUID uuid) {
        if (uuid == null) return;
        SERVER_SKINS.remove(uuid);
    }

    public static Map<UUID, String> snapshot() {
        return new LinkedHashMap<>(SERVER_SKINS);
    }

    public static String getServerSkinId(UUID uuid) {
        return uuid == null ? null : SERVER_SKINS.get(uuid);
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

    private static void writeChunk(CommonNetwork.PlayBuf buf, byte[] data) {
        byte[] b = data == null ? new byte[0] : data;
        buf.get().writeVarInt(b.length);
        if (b.length > 0) buf.get().writeBytes(b);
    }

    private static byte[] readChunk(CommonNetwork.PlayBuf buf) {
        int len = buf.get().readVarInt();
        if (len <= 0) return new byte[0];
        byte[] out = new byte[len];
        buf.get().readBytes(out);
        return out;
    }

    private static final class ServerChunkAccumulator {
        private final int total;
        private final byte[][] parts;
        private int received;

        private ServerChunkAccumulator(int total) {
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

    private static String assetKey(String skinId, int assetType) {
        return normalizeSkinId(skinId) + "|" + assetType;
    }

    private static void cacheChunk(UUID owner, String skinId, int assetType, int index, int total, byte[] data) {
        if (owner == null) return;
        if (skinId == null || skinId.isBlank()) return;
        if (total <= 0) return;

        String baseKey = assetKey(skinId, assetType);
        String k = owner + "|" + baseKey;

        ServerChunkAccumulator acc = SERVER_ACC.computeIfAbsent(k, kk -> new ServerChunkAccumulator(total));
        acc.put(index, data);

        if (!acc.complete()) return;

        SERVER_ACC.remove(k);
        SERVER_ASSETS.put(baseKey, acc.assemble());
    }

        public static boolean hasServerAsset(String skinId, int assetType) {
        if (skinId == null || skinId.isBlank()) return false;
        return SERVER_ASSETS.containsKey(assetKey(skinId, assetType));
    }

public static void sendCachedAssetsTo(ServerPlayer to, UUID owner, String skinId) {
        if (to == null || owner == null) return;
        if (skinId == null || skinId.isBlank()) return;

        for (int assetType = 0; assetType <= 1; assetType++) {
            byte[] bytes = SERVER_ASSETS.get(assetKey(skinId, assetType));
            if (bytes == null || bytes.length == 0) continue;

            int max = UploadAssetChunkC2S.MAX_CHUNK;
            int total = (bytes.length + max - 1) / max;

            for (int i = 0; i < total; i++) {
                int start = i * max;
                int end = Math.min(bytes.length, start + max);
                int len = end - start;
                byte[] chunk = new byte[len];
                System.arraycopy(bytes, start, chunk, 0, len);
                CommonNetwork.sendToPlayer(to, new SyncAssetChunkS2C(owner, skinId, assetType, i, total, chunk));
            }
        }
    }

    public record SetSkinC2S(String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SetSkinC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("set_skin_c2s"), SetSkinC2S::new);

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

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                CommonNetwork.sendToPlayer(other, new SyncSkinS2C(player.getUUID(), id));
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }

    public record RequestSnapshotC2S() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSnapshotC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("request_snapshot_c2s"), (CommonNetwork.PlayBuf b) -> new RequestSnapshotC2S(b));

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

            for (Map.Entry<UUID, String> e : snapshot().entrySet()) {
                UUID who = e.getKey();
                if (who == null) continue;
                String id = e.getValue();
                CommonNetwork.sendToPlayer(requester, new SyncSkinS2C(who, id));
                sendCachedAssetsTo(requester, who, id);
                if (id != null && !id.isBlank()) {
                    if (!SERVER_ASSETS.containsKey(assetKey(id, 0)) || !SERVER_ASSETS.containsKey(assetKey(id, 1))) {
                        ServerPlayer p = server.getPlayerList().getPlayer(who);
                        if (p != null) CommonNetwork.sendToPlayer(p, new RequestSkinS2C());
                    }
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
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("sync_skin_s2c"), SyncSkinS2C::new);

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

    public record UploadAssetChunkC2S(String skinId, int assetType, int index, int total, byte[] data) implements CommonNetwork.Payload {
        public static final int MAX_CHUNK = 30 * 1024;
        public static final CommonNetwork.Identifier<UploadAssetChunkC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("upload_skin_asset_chunk_c2s"), UploadAssetChunkC2S::new);

        public UploadAssetChunkC2S(CommonNetwork.PlayBuf buf) {
            this(readSkinId(buf), buf.get().readVarInt(), buf.get().readVarInt(), buf.get().readVarInt(), readChunk(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            writeSkinId(buf, skinId);
            buf.get().writeVarInt(assetType);
            buf.get().writeVarInt(index);
            buf.get().writeVarInt(total);
            writeChunk(buf, clampChunk(data));
        }

        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
            if (server == null) return;

            String id = normalizeSkinId(skinId);
            byte[] chunk = clampChunk(data);

            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                CommonNetwork.sendToPlayer(other, new SyncAssetChunkS2C(player.getUUID(), id, assetType, index, total, chunk));
            }

            cacheChunk(player.getUUID(), id, assetType, index, total, chunk);
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }

        private static byte[] clampChunk(byte[] b) {
            if (b == null) return new byte[0];
            if (b.length <= MAX_CHUNK) return b;
            byte[] out = new byte[MAX_CHUNK];
            System.arraycopy(b, 0, out, 0, MAX_CHUNK);
            return out;
        }
    }

    public record SyncAssetChunkS2C(UUID uuid, String skinId, int assetType, int index, int total, byte[] data) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SyncAssetChunkS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("sync_skin_asset_chunk_s2c"), SyncAssetChunkS2C::new);

        public SyncAssetChunkS2C(CommonNetwork.PlayBuf buf) {
            this(buf.get().readUUID(), readSkinId(buf), buf.get().readVarInt(), buf.get().readVarInt(), buf.get().readVarInt(), readChunk(buf));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeUUID(uuid);
            writeSkinId(buf, skinId);
            buf.get().writeVarInt(assetType);
            buf.get().writeVarInt(index);
            buf.get().writeVarInt(total);
            writeChunk(buf, data);
        }

        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            byte[] chunk = data;
            mc.execute(() -> SkinSyncClient.onSyncAssetChunk(uuid, skinId, assetType, index, total, chunk));
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }

    public record RequestSkinS2C() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSkinS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("request_skin_s2c"), (CommonNetwork.PlayBuf b) -> new RequestSkinS2C(b));

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
}
