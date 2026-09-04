package wily.legacy.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyTags;
import wily.legacy.world.LegacyWorldSettings;

import java.util.List;
import java.util.UUID;

public record PlayerTrustPolicy(UUID playerId, PlayerTrustPermissions permissions, boolean unrestricted,
                                @Nullable MinecraftServer server, @Nullable Player onlinePlayer) {
    public static final List<Identifier> MODERATOR_GAME_RULES = List.of(
            LegacyGameRules.FIRE_SPREADS.getId(), LegacyGameRules.getTntExplodes().getIdentifier(),
            GameRules.ADVANCE_TIME.getIdentifier(), GameRules.KEEP_INVENTORY.getIdentifier(), GameRules.SPAWN_MOBS.getIdentifier(),
            GameRules.MOB_GRIEFING.getIdentifier(), LegacyGameRules.GLOBAL_MAP_PLAYER_ICON.getId(), LegacyGameRules.LEGACY_SWIMMING.getId(),
            LegacyGameRules.LEGACY_FLIGHT.getId(), LegacyGameRules.LEGACY_SHIELD_CONTROLS.getId(), LegacyGameRules.LEGACY_OFFHAND_LIMITS.getId(),
            GameRules.ADVANCE_WEATHER.getIdentifier(), GameRules.MOB_DROPS.getIdentifier(), GameRules.BLOCK_DROPS.getIdentifier(),
            GameRules.NATURAL_HEALTH_REGENERATION.getIdentifier(), GameRules.IMMEDIATE_RESPAWN.getIdentifier()
    );

    public record Management(boolean canManageTrust, boolean canSetModerator, boolean canKick) {
    }

    public static PlayerTrustPolicy of(ServerPlayer player) {
        LegacyPlayerInfo.initializeTrustPermissions(player);
        LegacyPlayerInfo info = LegacyPlayerInfo.of(player);
        MinecraftServer server = player.level().getServer();
        return new PlayerTrustPolicy(player.getUUID(), info.getTrustPermissions(), isTrustEnabled(server) || info.hasFullTrustAuthority(), server, player);
    }

    public static PlayerTrustPolicy of(Player player, LegacyPlayerInfo info, boolean trustPlayers) {
        return new PlayerTrustPolicy(player.getUUID(), info.getTrustPermissions(), trustPlayers || info.hasFullTrustAuthority(), null, player);
    }

    public boolean canBuildAndMine() {
        return unrestricted || permissions.canBuildAndMine();
    }

    public boolean canUseDoorsAndSwitches() {
        return unrestricted || permissions.canUseDoorsAndSwitches();
    }

    public boolean canOpenContainers() {
        return unrestricted || permissions.canOpenContainers();
    }

    public boolean canInteractWithBlock(BlockState state) {
        if (unrestricted) return true;
        if (state.is(LegacyTags.CONTAINERS)) return permissions.canOpenContainers();
        if (state.is(LegacyTags.DOORS_AND_SWITCHES)) return permissions.canUseDoorsAndSwitches();
        return permissions.canBuildAndMine();
    }

    public boolean canUseItem(ItemStack stack) {
        return canBuildAndMine() || !requiresBuildAndMine(stack);
    }

    public boolean canTriggerBlock(BlockState state) {
        return state.is(LegacyTags.DOORS_AND_SWITCHES) ? canUseDoorsAndSwitches() : canBuildAndMine();
    }

    public boolean canInteractWithEntity(Entity target, boolean secondaryUseActive, ItemStack stack) {
        if (unrestricted) return true;
        boolean containerInteraction = isContainerInteraction(target, secondaryUseActive);
        if (!canUseItem(stack) && !(containerInteraction && stack.getItem() instanceof BlockItem)) return false;
        return containerInteraction ? permissions.canOpenContainers() : canDestroyEntity(target);
    }

    public boolean canDestroyEntity(Entity target) {
        return !target.getType().builtInRegistryHolder().is(LegacyTags.BUILD_INTERACTION_ENTITIES) || canBuildAndMine();
    }

    public boolean canDamageByTrust(Entity target) {
        return canDestroyEntity(target) && hasAttackPermission(target);
    }

    public boolean canDamage(Entity target) {
        if (server != null && playerId.equals(target.getUUID())) return true;
        return canDamageByTrust(target) && canAttackUnderVanillaRules(target);
    }

    private boolean hasAttackPermission(Entity target) {
        if (unrestricted) return true;
        if (requiresAttackPlayersPermission(target)) return permissions.canAttackPlayers();
        return !target.getType().builtInRegistryHolder().is(LegacyTags.ANIMALS) || permissions.canAttackAnimals();
    }

    private boolean canAttackUnderVanillaRules(Entity target) {
        if (server == null) {
            if (onlinePlayer == null) return false;
            if (playerId.equals(target.getUUID())) return true;
            boolean pvpAllowed = LegacyGameRules.getSidedBooleanGamerule(onlinePlayer, LegacyGameRules.getPvp());
            if (target instanceof Player victim) return pvpAllowed && victim.canHarmPlayer(onlinePlayer);
            if (pvpAllowed) return true;
            return canAttackWithPvpDisabled(playerId, target);
        }
        if (target instanceof ServerPlayer victim) {
            if (onlinePlayer instanceof ServerPlayer player) return victim.canHarmPlayer(player);
            if (!victim.level().isPvpAllowed()) return false;
            NameAndId profile = server.services().nameToIdCache().get(playerId).orElse(null);
            if (profile == null) return false;
            Team attackerTeam = server.getScoreboard().getPlayersTeam(profile.name());
            Team victimTeam = victim.getTeam();
            return attackerTeam == null || !attackerTeam.isAlliedTo(victimTeam) || attackerTeam.isAllowFriendlyFire();
        }
        return server.overworld().isPvpAllowed() || !(target instanceof Player) && canAttackWithPvpDisabled(playerId, target);
    }

    public static boolean isTrustEnabled(MinecraftServer server) {
        return LegacyWorldSettings.of(server.getWorldData()).trustPlayers();
    }

    public static boolean isFullAuthority(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.isSingleplayerOwner(player.nameAndId()) || server.isDedicatedServer() && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static Management management(ServerPlayer actor, ServerPlayer target) {
        return management(isTrustEnabled(actor.level().getServer()), actor == target, isFullAuthority(actor), LegacyPlayerInfo.of(actor).isModerator(), isFullAuthority(target));
    }

    public static Management management(boolean trustPlayers, boolean samePlayer, boolean actorFullAuthority, boolean actorModerator, boolean targetFullAuthority) {
        boolean canManageTarget = !samePlayer && !targetFullAuthority && (actorFullAuthority || actorModerator);
        return new Management(!trustPlayers && canManageTarget, canManageTarget && actorFullAuthority, canManageTarget);
    }

    public static boolean canChangeGameRule(ServerPlayer player, Identifier gameRule) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || LegacyPlayerInfo.of(player).isModerator() && MODERATOR_GAME_RULES.contains(gameRule);
    }

    public static boolean canManageHostOptions(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || LegacyPlayerInfo.of(player).isModerator();
    }

    public static boolean canManagePlayerOptions(ServerPlayer actor, ServerPlayer target) {
        if (actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        LegacyPlayerInfo actorInfo = LegacyPlayerInfo.of(actor);
        if (actor == target) return actorInfo.isModerator() || actorInfo.getHostPrivileges().any();
        return actorInfo.isModerator() && !isFullAuthority(target);
    }

    public static boolean canManageHostPrivileges(ServerPlayer actor, ServerPlayer target) {
        return actor != target && !isFullAuthority(target) && (actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || LegacyPlayerInfo.of(actor).isModerator());
    }

    public static boolean canBecomeInvisible(LegacyPlayerInfo info) {
        return info.getHostPrivileges().canBecomeInvisible();
    }

    public static boolean canFly(LegacyPlayerInfo info) {
        return info.getHostPrivileges().canFly();
    }

    public static boolean canDisableExhaustion(LegacyPlayerInfo info) {
        return info.getHostPrivileges().canDisableExhaustion();
    }

    public static boolean canTeleport(LegacyPlayerInfo info) {
        return info.getHostPrivileges().canTeleport();
    }

    private static boolean canAttackWithPvpDisabled(UUID playerId, Entity target) {
        return !(target instanceof OwnableEntity ownable && ownable.getOwner() != null && playerId.equals(ownable.getOwner().getUUID()))
                && !(target instanceof IronGolem golem && golem.isPlayerCreated())
                && !(target instanceof CopperGolem)
                && !(target instanceof SnowGolem);
    }

    private static boolean requiresAttackPlayersPermission(Entity target) {
        return target instanceof Player || target instanceof SnowGolem || target instanceof CopperGolem || target instanceof IronGolem golem && golem.isPlayerCreated();
    }

    private static boolean requiresBuildAndMine(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof SpawnEggItem || stack.is(LegacyTags.REQUIRES_BUILD_AND_MINE);
    }

    private static boolean isContainerInteraction(Entity target, boolean secondaryUseActive) {
        if (target.getType().builtInRegistryHolder().is(LegacyTags.CONTAINER_ENTITIES)) return true;
        if (!secondaryUseActive) return false;
        if (target instanceof AbstractHorse horse) return horse.isTamed() && !horse.isBaby() && !horse.isVehicle();
        return target instanceof AbstractNautilus nautilus && nautilus.isTame() && !nautilus.isBaby();
    }
}
