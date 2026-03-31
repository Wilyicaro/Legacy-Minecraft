package wily.legacy.Skins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Skins.skin.SkinSync;

import java.util.UUID;

public final class SkinsBootstrap {
    private SkinsBootstrap() {
    }

    private static final java.util.concurrent.ConcurrentHashMap<UUID, Long> LAST_JOIN_SYNC_MS =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void initCommon() {
        FactoryEvent.PlayerEvent.JOIN_EVENT.register(SkinsBootstrap::onServerPlayerJoin);
    }

    private static void runLater(MinecraftServer server, int ticks, Runnable task) {
        if (server == null || task == null) return;
        if (ticks <= 0) {
            server.execute(task);
        } else {
            server.execute(() -> runLater(server, ticks - 1, task));
        }
    }

    private static void onServerPlayerJoin(ServerPlayer joining) {
        handleServerPlayerJoin(joining);
    }

    public static void handleServerPlayerJoin(ServerPlayer joining) {
        MinecraftServer server = FactoryAPIPlatform.getEntityServer(joining);
        if (server == null) return;

        long now = System.currentTimeMillis();
        long last = LAST_JOIN_SYNC_MS.getOrDefault(joining.getUUID(), 0L);
        if (now - last < 1_000L) return;
        LAST_JOIN_SYNC_MS.put(joining.getUUID(), now);

        int delayTicks = FactoryAPI.getLoader().isForgeLike() ? 20 : 2;
        runLater(server, delayTicks, () -> {
            SkinSync.sendSnapshotTo(joining, server);
            SkinSync.requestSkin(joining);

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p == null) continue;
                if (SkinSync.getServerSkinId(p.getUUID()) == null) {
                    SkinSync.requestSkin(p);
                }
            }
        });
    }
}
