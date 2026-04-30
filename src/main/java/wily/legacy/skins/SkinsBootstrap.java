package wily.legacy.skins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.legacy.skins.skin.SkinSync;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinsBootstrap {
    private static final Map<UUID, PendingJoinSync> PENDING_JOIN_SYNC = new ConcurrentHashMap<>();

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
        PENDING_JOIN_SYNC.put(joining.getUUID(), new PendingJoinSync(FactoryAPI.getLoader().isForgeLike() ? 20 : 2, 0));
    }

    private static void processPendingJoinSync(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingJoinSync>> iterator = PENDING_JOIN_SYNC.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingJoinSync> entry = iterator.next();
            PendingJoinSync pending = entry.getValue();
            int ticks = pending.ticks() - 1;
            if (ticks > 0) {
                entry.setValue(new PendingJoinSync(ticks, pending.attempts()));
                continue;
            }
            ServerPlayer joining = server.getPlayerList().getPlayer(entry.getKey());
            if (joining == null || syncJoin(server, joining) || pending.attempts() >= 10) {
                iterator.remove();
                continue;
            }
            entry.setValue(new PendingJoinSync(20, pending.attempts() + 1));
        }
    }

    private static boolean syncJoin(MinecraftServer server, ServerPlayer joining) {
        SkinSync.sendSnapshotTo(joining, server);
        boolean done = hasCompleteSkin(joining);
        if (!done) SkinSync.requestSkin(joining);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == null || hasCompleteSkin(player)) continue;
            SkinSync.requestSkin(player);
        }
        return done;
    }

    private static boolean hasCompleteSkin(ServerPlayer player) {
        if (player == null) return true;
        String skinId = SkinSync.getServerSkinId(player.getUUID());
        if (skinId == null) return false;
        return !SkinIdUtil.hasSkin(skinId) || SkinSync.hasServerAssets(player.getUUID(), skinId);
    }

    private record PendingJoinSync(int ticks, int attempts) {
    }
}
