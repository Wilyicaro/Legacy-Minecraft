package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerTrustPermissions;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.world.PlayerTrustAdmin;

import java.util.Objects;
import java.util.UUID;

public record PlayerTrustUpdatePayload(UUID player, PlayerTrustPermissions permissions, boolean moderator) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<PlayerTrustUpdatePayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_trust_update_c2s"), PlayerTrustUpdatePayload::new);

    public PlayerTrustUpdatePayload(CommonNetwork.PlayBuf buf) {
        this(buf.get().readUUID(), PlayerTrustPermissions.decode(buf), buf.get().readBoolean());
    }

    public static PlayerTrustUpdatePayload forPlayer(GameProfile profile, PlayerTrustPermissions permissions, boolean moderator) {
        return new PlayerTrustUpdatePayload(profile.id(), permissions, moderator);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeUUID(player);
        permissions.encode(buf);
        buf.get().writeBoolean(moderator);
    }

    @Override
    public void apply(Context context) {
        if (!(context.player() instanceof ServerPlayer actor)) return;
        MinecraftServer server = actor.level().getServer();
        ServerPlayer target = server.getPlayerList().getPlayer(player);
        if (target == null) return;
        LegacyPlayerInfo targetInfo = LegacyPlayerInfo.of(target);
        PlayerTrustPolicy.Management management = PlayerTrustPolicy.management(actor, target);
        boolean changed = false;
        if (management.canManageTrust() && !Objects.equals(targetInfo.getTrustPermissions(), permissions)) {
            targetInfo.setTrustPermissions(permissions);
            targetInfo.setTrustPermissionsInitialized(true);
            changed = true;
        }
        if (management.canSetModerator() && targetInfo.isModerator() != moderator) {
            targetInfo.setModerator(moderator);
            changed = true;
        }
        if (changed) PlayerTrustAdmin.enforceCurrentRestrictions(target);
        PlayerInfoSync.All authoritative = PlayerInfoSync.All.fromPlayer(target);
        if (changed) CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), authoritative);
        else CommonNetwork.sendToPlayer(actor, authoritative);
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
