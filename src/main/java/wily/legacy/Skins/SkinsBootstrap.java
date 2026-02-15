package wily.legacy.Skins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Skins.skin.SkinSync;

import java.util.Map;
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

            for (Map.Entry<UUID, String> e : SkinSync.snapshot().entrySet()) {
                UUID who = e.getKey();
                String skinId = e.getValue();

                CommonNetwork.sendToPlayer(joining, new SkinSync.SyncSkinS2C(who, skinId));

                byte[] cpmFile = SkinSync.getServerCpmModelFile(who, skinId);
                if (cpmFile != null && cpmFile.length > 0) {
                    SkinSync.sendCpmModelToPlayer(joining, who, skinId, cpmFile);
                } else if (SkinSync.isCpm(skinId)) {

                    ServerPlayer owner = server.getPlayerList().getPlayer(who);
                    if (owner != null) SkinSync.requestCpmModelFrom(owner, skinId);
                }
            }

            CommonNetwork.sendToPlayer(joining, new SkinSync.RequestSkinS2C());

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p == null) continue;
                if (SkinSync.getServerSkinId(p.getUUID()) == null) {
                    CommonNetwork.sendToPlayer(p, new SkinSync.RequestSkinS2C());
                }
            }
        });
    }
}
