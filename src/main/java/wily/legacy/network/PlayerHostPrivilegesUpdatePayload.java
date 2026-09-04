package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerHostPrivileges;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.world.PlayerTrustAdmin;

import java.util.Objects;
import java.util.UUID;

public record PlayerHostPrivilegesUpdatePayload(UUID player, PlayerHostPrivileges privileges) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<PlayerHostPrivilegesUpdatePayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_host_privileges_update_c2s"), PlayerHostPrivilegesUpdatePayload::new);

    public PlayerHostPrivilegesUpdatePayload(CommonNetwork.PlayBuf buf) {
        this(buf.get().readUUID(), PlayerHostPrivileges.decode(buf));
    }

    public static PlayerHostPrivilegesUpdatePayload forPlayer(GameProfile profile, PlayerHostPrivileges privileges) {
        return new PlayerHostPrivilegesUpdatePayload(profile.id(), privileges);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeUUID(player);
        privileges.encode(buf);
    }

    @Override
    public void apply(Context context) {
        if (!(context.player() instanceof ServerPlayer actor)) return;
        ServerPlayer target = actor.level().getServer().getPlayerList().getPlayer(player);
        if (target == null) return;
        LegacyPlayerInfo targetInfo = LegacyPlayerInfo.of(target);
        PlayerHostPrivileges previous = targetInfo.getHostPrivileges();
        if (!PlayerTrustPolicy.canManageHostPrivileges(actor, target) || Objects.equals(previous, privileges)) {
            CommonNetwork.sendToPlayer(actor, PlayerInfoSync.All.fromPlayer(target));
            return;
        }
        targetInfo.setHostPrivileges(privileges);
        notifyChanges(target, previous, privileges);
        PlayerTrustAdmin.enforceCurrentRestrictions(target);
        CommonNetwork.sendToPlayers(actor.level().getServer().getPlayerList().getPlayers(), PlayerInfoSync.All.fromPlayer(target));
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    static void notifyChanges(ServerPlayer player, PlayerHostPrivileges previous, PlayerHostPrivileges current) {
        notifyChange(player, "canBecomeInvisible", previous.canBecomeInvisible(), current.canBecomeInvisible());
        notifyChange(player, "canFly", previous.canFly(), current.canFly());
        notifyChange(player, "canDisableExhaustion", previous.canDisableExhaustion(), current.canDisableExhaustion());
        notifyChange(player, "canTeleport", previous.canTeleport(), current.canTeleport());
    }

    private static void notifyChange(ServerPlayer player, String privilege, boolean previous, boolean current) {
        if (previous != current)
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player." + privilege + "." + (current ? "enabled" : "disabled")), false);
    }
}
