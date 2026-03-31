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
    public static final String ASSET_NS = Legacy4J.MOD_ID;
    public static final int ASSET_TEXTURE = 0;
    public static final int ASSET_MODEL = 1;
    private static final int MAX_SKIN_ID_LEN = 256;
    private static final Map<UUID, String> SERVER_SKINS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> SERVER_ASSETS = new ConcurrentHashMap<>();
    private static final Map<String, SkinChunkAccumulator> SERVER_ACC = new ConcurrentHashMap<>();
    @FunctionalInterface
    interface ChunkConsumer { void accept(int index, int total, byte[] chunk); }
    private SkinSync() { }
    public static Map<UUID, String> snapshot() { return new LinkedHashMap<>(SERVER_SKINS); }
    public static String getServerSkinId(UUID uuid) { return uuid == null ? null : SERVER_SKINS.get(uuid); }
    private static String normalizeSkinId(String skinId) {
        String id = skinId == null ? "" : skinId;
        if (id.length() > MAX_SKIN_ID_LEN) id = id.substring(0, MAX_SKIN_ID_LEN);
        return id;
    }
    private static void writeSkinId(CommonNetwork.PlayBuf buf, String skinId) { buf.get().writeUtf(normalizeSkinId(skinId), MAX_SKIN_ID_LEN); }
    private static String readSkinId(CommonNetwork.PlayBuf buf) { return buf.get().readUtf(MAX_SKIN_ID_LEN); }
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
    private static String assetKey(String skinId, int assetType) { return normalizeSkinId(skinId) + "|" + assetType; }
    private static void cacheChunk(UUID owner, String skinId, int assetType, int index, int total, byte[] data) {
        if (owner == null) return;
        if (skinId == null || skinId.isBlank()) return;
        if (total <= 0) return;
        String baseKey = assetKey(skinId, assetType);
        String k = owner + "|" + baseKey;
        SkinChunkAccumulator acc = SERVER_ACC.computeIfAbsent(k, kk -> new SkinChunkAccumulator(total));
        acc.put(index, data);
        if (!acc.isComplete()) return;
        SERVER_ACC.remove(k);
        SERVER_ASSETS.put(baseKey, acc.assemble());
    }
    public static boolean hasServerAssets(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        return SERVER_ASSETS.containsKey(assetKey(skinId, ASSET_TEXTURE))
                && SERVER_ASSETS.containsKey(assetKey(skinId, ASSET_MODEL));
    }
    public static void requestSkin(ServerPlayer player) {
        if (player != null) { CommonNetwork.sendToPlayer(player, new RequestSkinS2C()); }
    }
    public static void sendSnapshotTo(ServerPlayer requester, MinecraftServer server) {
        if (requester == null || server == null) return;
        for (Map.Entry<UUID, String> entry : snapshot().entrySet()) { sendSnapshotEntry(requester, server, entry.getKey(), entry.getValue()); }
    }
    static void forEachChunk(byte[] bytes, int maxChunk, ChunkConsumer consumer) {
        if (consumer == null || maxChunk <= 0) return;
        byte[] data = bytes == null ? new byte[0] : bytes;
        int total = data.length == 0 ? 1 : (data.length + maxChunk - 1) / maxChunk;
        for (int i = 0; i < total; i++) {
            int start = i * maxChunk;
            int end = Math.min(data.length, start + maxChunk);
            byte[] chunk = new byte[Math.max(0, end - start)];
            if (chunk.length > 0) { System.arraycopy(data, start, chunk, 0, chunk.length); }
            consumer.accept(i, total, chunk);
        }
    }
    public static void sendCachedAssetsTo(ServerPlayer to, UUID owner, String skinId) {
        if (to == null || owner == null) return;
        if (skinId == null || skinId.isBlank()) return;
        for (int assetType = ASSET_TEXTURE; assetType <= ASSET_MODEL; assetType++) {
            byte[] bytes = SERVER_ASSETS.get(assetKey(skinId, assetType));
            if (bytes == null || bytes.length == 0) continue;
            int type = assetType;
            forEachChunk(bytes, UploadAssetChunkC2S.MAX_CHUNK, (index, total, chunk) ->
                    CommonNetwork.sendToPlayer(to, new SyncAssetChunkS2C(owner, skinId, type, index, total, chunk))
            );
        }
    }
    private static void sendSnapshotEntry(ServerPlayer requester, MinecraftServer server, UUID who, String skinId) {
        if (requester == null || server == null || who == null) return;
        CommonNetwork.sendToPlayer(requester, new SyncSkinS2C(who, skinId));
        sendCachedAssetsTo(requester, who, skinId);
        if (skinId != null && !skinId.isBlank() && !hasServerAssets(skinId)) { requestSkin(server.getPlayerList().getPlayer(who)); }
    }
    public record SetSkinC2S(String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SetSkinC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("set_skin_c2s"), SetSkinC2S::new);
        public SetSkinC2S(CommonNetwork.PlayBuf buf) { this(readSkinId(buf)); }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) { writeSkinId(buf, skinId); }
        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
            if (server == null) return;
            String id = normalizeSkinId(skinId);
            SERVER_SKINS.put(player.getUUID(), id);
            for (ServerPlayer other : server.getPlayerList().getPlayers()) { CommonNetwork.sendToPlayer(other, new SyncSkinS2C(player.getUUID(), id)); }
        }
        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
    }
    public record RequestSnapshotC2S() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSnapshotC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("request_snapshot_c2s"), (CommonNetwork.PlayBuf b) -> new RequestSnapshotC2S(b));
        public RequestSnapshotC2S(CommonNetwork.PlayBuf buf) { this(); }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) { }
        @Override
        public void apply(Context context) {
            if (!(context.player() instanceof ServerPlayer requester)) return;
            MinecraftServer server = FactoryAPIPlatform.getEntityServer(requester);
            if (server == null) return;
            sendSnapshotTo(requester, server);
        }
        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
    }
    public record SyncSkinS2C(UUID uuid, String skinId) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<SyncSkinS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("sync_skin_s2c"), SyncSkinS2C::new);
        public SyncSkinS2C(CommonNetwork.PlayBuf buf) { this(buf.get().readUUID(), readSkinId(buf)); }
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
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
    }
    public record UploadAssetChunkC2S(String skinId, int assetType, int index, int total, byte[] data) implements CommonNetwork.Payload {
        public static final int MAX_CHUNK = 30 * 1024;
        public static final CommonNetwork.Identifier<UploadAssetChunkC2S> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("upload_skin_asset_chunk_c2s"), UploadAssetChunkC2S::new);
        public UploadAssetChunkC2S(CommonNetwork.PlayBuf buf) { this(readSkinId(buf), buf.get().readVarInt(), buf.get().readVarInt(), buf.get().readVarInt(), readChunk(buf)); }
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
            for (ServerPlayer other : server.getPlayerList().getPlayers()) { CommonNetwork.sendToPlayer(other, new SyncAssetChunkS2C(player.getUUID(), id, assetType, index, total, chunk)); }
            cacheChunk(player.getUUID(), id, assetType, index, total, chunk);
        }
        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
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
        public SyncAssetChunkS2C(CommonNetwork.PlayBuf buf) { this(buf.get().readUUID(), readSkinId(buf), buf.get().readVarInt(), buf.get().readVarInt(), buf.get().readVarInt(), readChunk(buf)); }
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
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
    }
    public record RequestSkinS2C() implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<RequestSkinS2C> ID =
                CommonNetwork.Identifier.create(Legacy4J.createModLocation("request_skin_s2c"), (CommonNetwork.PlayBuf b) -> new RequestSkinS2C(b));
        public RequestSkinS2C(CommonNetwork.PlayBuf buf) { this(); }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) { }
        @Override
        public void apply(Context context) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(SkinSyncClient::onRequestSkinUpload);
        }
        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() { return ID; }
    }
}
