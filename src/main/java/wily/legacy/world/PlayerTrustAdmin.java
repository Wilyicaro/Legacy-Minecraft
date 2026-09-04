package wily.legacy.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerTrustOwnedSource;
import wily.legacy.entity.PlayerTrustPermissions;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.network.PlayerInfoSync;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class PlayerTrustAdmin {
    private static final Map<MinecraftServer, Set<UUID>> SESSION_KICKS = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Optional<PlayerTrustPermissions>>> OFFLINE_PERMISSIONS = new WeakHashMap<>();
    private static final ThreadLocal<PlayerTrustPolicy> RESPONSIBLE_PLAYER = new ThreadLocal<>();

    private PlayerTrustAdmin() {
    }

    public static void setTrustPlayers(MinecraftServer server, boolean trustPlayers) {
        LegacyWorldSettings.of(server.getWorldData()).setTrustPlayers(trustPlayers);
        server.getPlayerList().getPlayers().forEach(PlayerTrustAdmin::enforceCurrentRestrictions);
        CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), PlayerInfoSync.All.fromTrustPlayers(server));
    }

    public static void enforceCurrentRestrictions(ServerPlayer player) {
        PlayerTrustPolicy context = PlayerTrustPolicy.of(player);
        LegacyPlayerInfo info = LegacyPlayerInfo.of(player);
        if (!context.canBuildAndMine() || !context.canOpenContainers()) player.closeContainer();
        if (player.getVehicle() != null && !context.canInteractWithEntity(player.getVehicle(), player.isSecondaryUseActive(), ItemStack.EMPTY)) player.stopRiding();
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
        if (!PlayerTrustPolicy.canBecomeInvisible(info) && !info.isVisible()) {
            info.setVisibility(true);
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.invisible.disabled"), false);
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.invulnerable.disabled"), false);
        }
        if (!PlayerTrustPolicy.canDisableExhaustion(info) && info.isExhaustionDisabled()) {
            info.setDisableExhaustion(false);
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.disableExhaustion.disabled"), false);
        }
        if (!PlayerTrustPolicy.canFly(info) && info.mayFlySurvival()) {
            LegacyPlayerInfo.setAndUpdateMayFlySurvival(player, false, true);
            player.sendSystemMessage(Component.translatable("legacy.menu.host_options.player.mayFly.disabled"), false);
        }
    }

    public static <T> T withResponsiblePlayer(@Nullable PlayerTrustPolicy player, Supplier<T> action) {
        if (player == null) return action.get();
        PlayerTrustPolicy previous = RESPONSIBLE_PLAYER.get();
        RESPONSIBLE_PLAYER.set(player);
        try {
            return action.get();
        } finally {
            if (previous == null) RESPONSIBLE_PLAYER.remove();
            else RESPONSIBLE_PLAYER.set(previous);
        }
    }

    public static PlayerTrustPolicy getResponsiblePlayer(ServerLevel level, DamageSource source) {
        PlayerTrustPolicy player = getResponsiblePlayer(level, source.getEntity());
        if (player == null) player = getResponsiblePlayer(level, source.getDirectEntity());
        return player == null ? RESPONSIBLE_PLAYER.get() : player;
    }

    public static PlayerTrustPolicy getResponsiblePlayer(ServerLevel level, DamageSource damageSource, Entity source) {
        PlayerTrustPolicy player = getResponsiblePlayer(level, damageSource);
        if (player == null) player = getResponsiblePlayer(level, source);
        return player;
    }

    public static PlayerTrustPolicy getResponsiblePlayer(ServerLevel level, Entity source) {
        if (source instanceof ServerPlayer player) return PlayerTrustPolicy.of(player);
        if (!(source instanceof PlayerTrustOwnedSource ownedSource)) return null;
        UUID playerId = ownedSource.getResponsiblePlayer();
        return playerId == null ? null : getPlayerContext(level.getServer(), playerId);
    }

    public static PlayerTrustPolicy getScopedResponsiblePlayer() {
        return RESPONSIBLE_PLAYER.get();
    }

    public static synchronized void rememberPlayer(ServerPlayer player) {
        LegacyPlayerInfo.initializeTrustPermissions(player);
        OFFLINE_PERMISSIONS.computeIfAbsent(player.level().getServer(), server -> new java.util.HashMap<>()).put(player.getUUID(), Optional.of(LegacyPlayerInfo.of(player).getTrustPermissions()));
    }

    public static PlayerTrustPolicy getPlayerContext(MinecraftServer server, UUID playerId) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        if (onlinePlayer != null) return PlayerTrustPolicy.of(onlinePlayer);
        synchronized (PlayerTrustAdmin.class) {
            Map<UUID, Optional<PlayerTrustPermissions>> players = OFFLINE_PERMISSIONS.get(server);
            boolean knownPlayer = players != null && players.containsKey(playerId)
                    || server.services().nameToIdCache().get(playerId).isPresent()
                    || !server.isDedicatedServer() && playerId.equals(server.getWorldData().getSinglePlayerUUID());
            if (!knownPlayer) return null;
        }
        Optional<PlayerTrustPermissions> permissions = getOfflinePermissions(server, playerId);
        if (permissions.isEmpty()) return null;
        NameAndId profile = server.services().nameToIdCache().get(playerId).orElse(new NameAndId(playerId, "<legacy trust owner>"));
        boolean fullAuthority = !server.isDedicatedServer() && (server.getWorldData().getSinglePlayerUUID() != null && server.getWorldData().getSinglePlayerUUID().equals(playerId) || server.isSingleplayerOwner(profile))
                || server.isDedicatedServer() && server.getProfilePermissions(profile).hasPermission(Permissions.COMMANDS_GAMEMASTER);
        return new PlayerTrustPolicy(playerId, permissions.get(), fullAuthority || PlayerTrustPolicy.isTrustEnabled(server), server, null);
    }

    private static synchronized Optional<PlayerTrustPermissions> getOfflinePermissions(MinecraftServer server, UUID playerId) {
        Map<UUID, Optional<PlayerTrustPermissions>> players = OFFLINE_PERMISSIONS.computeIfAbsent(server, key -> new java.util.HashMap<>());
        if (players.containsKey(playerId)) return players.get(playerId);
        NameAndId profile = server.services().nameToIdCache().get(playerId).orElse(new NameAndId(playerId, "<legacy trust owner>"));
        Optional<CompoundTag> playerData = server.getPlayerList().loadPlayerData(profile);
        Optional<PlayerTrustPermissions> permissions = playerData.map(tag -> TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), tag))
                .map(input -> input.child("PlayerTrust").map(PlayerTrustPermissions::load).orElse(PlayerTrustPolicy.isTrustEnabled(server) ? PlayerTrustPermissions.TRUSTED : PlayerTrustPermissions.RESTRICTED));
        players.put(playerId, permissions);
        return permissions;
    }

    public static synchronized void kick(ServerPlayer actor, ServerPlayer target) {
        MinecraftServer server = actor.level().getServer();
        SESSION_KICKS.computeIfAbsent(server, s -> new HashSet<>()).add(target.getUUID());
        actor.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.player_kicked", target.getDisplayName()), false);
        target.connection.disconnect(Component.translatable("multiplayer.disconnect.kicked"));
    }

    public static synchronized boolean isSessionKicked(MinecraftServer server, UUID player) {
        Set<UUID> players = SESSION_KICKS.get(server);
        return players != null && players.contains(player);
    }
}
