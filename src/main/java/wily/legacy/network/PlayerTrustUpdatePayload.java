package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerHostPrivileges;
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
        PlayerTrustPermissions previousPermissions = targetInfo.getTrustPermissions();
        boolean previousModerator = targetInfo.isModerator();
        PlayerTrustPolicy.Management management = PlayerTrustPolicy.management(actor, target);
        boolean changed = false;
        if (management.canManageTrust() && !Objects.equals(targetInfo.getTrustPermissions(), permissions)) {
            targetInfo.setTrustPermissions(permissions);
            targetInfo.setTrustPermissionsInitialized(true);
            changed = true;
        }
        if (management.canSetModerator() && targetInfo.isModerator() != moderator) {
            PlayerHostPrivileges previousPrivileges = targetInfo.getHostPrivileges();
            targetInfo.setModerator(moderator);
            targetInfo.setHostPrivileges(moderator ? PlayerHostPrivileges.ALL : PlayerHostPrivileges.NONE);
            PlayerHostPrivilegesUpdatePayload.notifyChanges(target, previousPrivileges, targetInfo.getHostPrivileges());
            changed = true;
        }
        if (changed) {
            PlayerTrustAdmin.enforceCurrentRestrictions(target);
            notifyPermissionChanges(target, previousPermissions, targetInfo.getTrustPermissions());
            notifyChange(target, "moderator", previousModerator, targetInfo.isModerator());
        }
        PlayerInfoSync.All authoritative = PlayerInfoSync.All.fromPlayer(target);
        if (changed) CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), authoritative);
        else CommonNetwork.sendToPlayer(actor, authoritative);
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    public static void notifyPermissionChanges(ServerPlayer player, PlayerTrustPermissions previous, PlayerTrustPermissions current) {
        notifyBuildAndMineChange(player, previous.canBuildAndMine(), current.canBuildAndMine());
        notifyChange(player, "canUseDoorsAndSwitches", previous.canUseDoorsAndSwitches(), current.canUseDoorsAndSwitches());
        notifyChange(player, "canOpenContainers", previous.canOpenContainers(), current.canOpenContainers());
        notifyChange(player, "canAttackPlayers", previous.canAttackPlayers(), current.canAttackPlayers());
        notifyChange(player, "canAttackAnimals", previous.canAttackAnimals(), current.canAttackAnimals());
    }

    private static void notifyBuildAndMineChange(ServerPlayer player, boolean previous, boolean current) {
        if (previous == current) return;
        String state = current ? "enabled" : "disabled";
        player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.canBuildAndMine.placeBlocks." + state), false);
        player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.canBuildAndMine.mineAndUseItems." + state), false);
    }

    private static void notifyChange(ServerPlayer player, String permission, boolean previous, boolean current) {
        if (previous != current)
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player." + permission + "." + (current ? "enabled" : "disabled")), false);
    }
}
