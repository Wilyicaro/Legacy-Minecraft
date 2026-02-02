package wily.legacy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class LegacyBlockCaps {

    private static final int BANNER_CAP = 16;
    private static final int CHUNK_RADIUS = 8;

    private LegacyBlockCaps() {
    }

    public static int bannerCap() {
        return BANNER_CAP;
    }

    public static int countBannersInLegacyArea(ServerLevel level, ServerPlayer player) {
        ChunkPos center = player.chunkPosition();
        int count = 0;

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof BannerBlockEntity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static boolean isBannerCapped(ServerLevel level, ServerPlayer player) {
        return countBannersInLegacyArea(level, player) >= BANNER_CAP;
    }
}
