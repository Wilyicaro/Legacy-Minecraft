package wily.legacy.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyChunkLoading;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyGeneratedChunks {
    private static final long TTL_MILLIS = 120000;
    private static final long PRUNE_MILLIS = 10000;
    private static final Map<ResourceKey<Level>, Map<Long, Long>> chunks = new ConcurrentHashMap<>();
    private static long lastPrune;

    private LegacyGeneratedChunks() {
    }

    public static void mark(ServerLevel level, ChunkPos pos) {
        chunks.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>()).put(pos.toLong(), Util.getMillis());
    }

    public static void sync(ServerPlayer player, LevelChunk chunk) {
        Map<Long, Long> levelChunks = chunks.get(player.level().dimension());
        if (levelChunks == null) {
            return;
        }

        long now = Util.getMillis();
        prune(levelChunks, now);

        Long markedAt = levelChunks.get(chunk.getPos().toLong());
        if (markedAt == null || now - markedAt > TTL_MILLIS) {
            return;
        }

        ChunkPos pos = chunk.getPos();
        CommonNetwork.sendToPlayer(player, new Payload(pos.x, pos.z));
    }

    private static void prune(Map<Long, Long> levelChunks, long now) {
        if (now - lastPrune < PRUNE_MILLIS) {
            return;
        }

        lastPrune = now;
        levelChunks.entrySet().removeIf(entry -> now - entry.getValue() > TTL_MILLIS);
    }

    public record Payload(int x, int z) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<Payload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_fresh_chunk"), Payload::new);

        public Payload(CommonNetwork.PlayBuf buf) {
            this(buf.get().readInt(), buf.get().readInt());
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeInt(x);
            buf.get().writeInt(z);
        }

        @Override
        public void apply(Context context) {
            if (FactoryAPIPlatform.isClient()) {
                LegacyChunkLoading.markFreshChunk(x, z);
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }
}
