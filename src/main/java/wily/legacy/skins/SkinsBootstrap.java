package wily.legacy.skins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.skins.skin.SkinSync;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinsBootstrap {
    private static final Map<UUID, Integer> PENDING_JOIN_SYNC = new ConcurrentHashMap<>();

    private SkinsBootstrap() {
    }

    public static void initCommon() {
        FactoryEvent.PlayerEvent.JOIN_EVENT.register(SkinsBootstrap::onServerPlayerJoin);
        FactoryEvent.afterServerTick(SkinsBootstrap::processPendingJoinSync);
        FactoryEvent.serverStopping(server -> {
            PENDING_JOIN_SYNC.clear();
            SkinSync.clearAll();
        });
    }

    private static void onServerPlayerJoin(ServerPlayer joining) {
        MinecraftServer server = FactoryAPIPlatform.getEntityServer(joining);
        if (server == null) return;
        PENDING_JOIN_SYNC.put(joining.getUUID(), FactoryAPI.getLoader().isForgeLike() ? 20 : 2);
    }

    private static void processPendingJoinSync(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_JOIN_SYNC.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks > 0) {
                entry.setValue(ticks);
                continue;
            }
            iterator.remove();
            ServerPlayer joining = server.getPlayerList().getPlayer(entry.getKey());
            if (joining != null) syncJoin(server, joining);
        }
    }

    private static void syncJoin(MinecraftServer server, ServerPlayer joining) {
        SkinSync.sendSnapshotTo(joining, server);
        SkinSync.requestSkin(joining);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == null || SkinSync.getServerSkinId(player.getUUID()) != null) continue;
            SkinSync.requestSkin(player);
        }
    }
}
