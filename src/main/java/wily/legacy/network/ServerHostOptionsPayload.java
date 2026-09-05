package wily.legacy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.world.PlayerTrustAdmin;

import java.util.UUID;

public record ServerHostOptionsPayload(Action action, String value, UUID player) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ServerHostOptionsPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("server_host_options"), ServerHostOptionsPayload::new);
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public ServerHostOptionsPayload(CommonNetwork.PlayBuf buf) {
        this(buf.get().readEnum(Action.class), buf.get().readUtf(), buf.get().readUUID());
    }

    public static ServerHostOptionsPayload command(String command) {
        return new ServerHostOptionsPayload(Action.COMMAND, command, EMPTY_UUID);
    }

    public static ServerHostOptionsPayload time(String time) {
        return new ServerHostOptionsPayload(Action.TIME, time, EMPTY_UUID);
    }

    public static ServerHostOptionsPayload weather(String weather) {
        return new ServerHostOptionsPayload(Action.WEATHER, weather, EMPTY_UUID);
    }

    public static ServerHostOptionsPayload difficulty(Difficulty difficulty) {
        return new ServerHostOptionsPayload(Action.DIFFICULTY, difficulty.getSerializedName(), EMPTY_UUID);
    }

    public static ServerHostOptionsPayload defaultGameMode(GameType gameType) {
        return new ServerHostOptionsPayload(Action.DEFAULT_GAME_MODE, gameType.getName(), EMPTY_UUID);
    }

    public static ServerHostOptionsPayload gameMode(GameType gameType, UUID player) {
        return new ServerHostOptionsPayload(Action.GAME_MODE, gameType.getName(), player);
    }

    public static ServerHostOptionsPayload worldSpawn() {
        return new ServerHostOptionsPayload(Action.WORLD_SPAWN, "", EMPTY_UUID);
    }

    public static ServerHostOptionsPayload playerSpawn(UUID player) {
        return new ServerHostOptionsPayload(Action.PLAYER_SPAWN, "", player);
    }

    public static ServerHostOptionsPayload trustPlayers(boolean trustPlayers) {
        return new ServerHostOptionsPayload(Action.TRUST_PLAYERS, Boolean.toString(trustPlayers), EMPTY_UUID);
    }

    public static ServerHostOptionsPayload kick(UUID player) {
        return new ServerHostOptionsPayload(Action.KICK, "", player);
    }

    public static ServerHostOptionsPayload teleportToPlayer(UUID player) {
        return new ServerHostOptionsPayload(Action.TELEPORT_TO_PLAYER, "", player);
    }

    public static ServerHostOptionsPayload teleportToMe(UUID player) {
        return new ServerHostOptionsPayload(Action.TELEPORT_TO_ME, "", player);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeEnum(action);
        buf.get().writeUtf(value);
        buf.get().writeUUID(player);
    }

    @Override
    public void apply(Context context) {
        if (!(context.player() instanceof ServerPlayer sp)) return;
        var server = FactoryAPIPlatform.getEntityServer(sp);
        if (action == Action.TRUST_PLAYERS) {
            if (!PlayerTrustPolicy.isFullAuthority(sp) || !(value.equals("true") || value.equals("false"))) return;
            PlayerTrustAdmin.setTrustPlayers(server, Boolean.parseBoolean(value));
            return;
        }
        if (action == Action.KICK) {
            ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
            if (affectPlayer != null && PlayerTrustPolicy.management(sp, affectPlayer).canKick()) PlayerTrustAdmin.kick(sp, affectPlayer);
            return;
        }
        if (action == Action.TELEPORT_TO_PLAYER || action == Action.TELEPORT_TO_ME) {
            if (!PlayerTrustPolicy.canTeleport(sp)) return;
            ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
            if (affectPlayer == null || affectPlayer == sp) return;
            var source = server.createCommandSourceStack().withSuppressedOutput().withCallback((success, result) -> {
                if (success) affectPlayer.sendSystemMessage(Component.translatable(action == Action.TELEPORT_TO_PLAYER ? "legacy.menu.host_options.message.teleport.to_you" : "legacy.menu.host_options.message.teleport.to_them", sp.getDisplayName()), false);
            });
            if (action == Action.TELEPORT_TO_PLAYER)
                server.getCommands().performPrefixedCommand(source, "tp %s %s".formatted(sp.getGameProfile().name(), affectPlayer.getGameProfile().name()));
            else
                server.getCommands().performPrefixedCommand(source, "tp %s %s".formatted(affectPlayer.getGameProfile().name(), sp.getGameProfile().name()));
            return;
        }
        if (!PlayerTrustPolicy.canManageHostOptions(sp)) return;
        if (action == Action.TIME && !value.equals("day") && !value.equals("night")) return;
        if (action == Action.WEATHER && !value.equals("clear") && !value.equals("rain") && !value.equals("thunder")) return;
        if (action == Action.DIFFICULTY && !value.equals("peaceful") && !value.equals("easy") && !value.equals("normal") && !value.equals("hard")) return;
        if ((action == Action.DEFAULT_GAME_MODE || action == Action.GAME_MODE) && !isGameTypeValue(value)) return;
        var source = sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) ? sp.createCommandSourceStack().withSuppressedOutput() : server.createCommandSourceStack().withEntity(sp).withPosition(sp.position()).withRotation(sp.getRotationVector()).withLevel(sp.level()).withSuppressedOutput();
        BlockPos pos = sp.blockPosition();
        switch (action) {
            case COMMAND -> {
                if (!sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
                server.getCommands().performPrefixedCommand(source, value);
                CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), PlayerInfoSync.All.fromPlayerList(server));
            }
            case TIME -> {
                server.getCommands().performPrefixedCommand(source, "time set " + value);
                sp.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.set_" + value), false);
            }
            case WEATHER -> {
                setWeather(server, sp, value);
                sp.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.weather." + value), false);
            }
            case DIFFICULTY -> {
                server.getCommands().performPrefixedCommand(source, "difficulty " + value);
                sp.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.difficulty", switch (value) {
                    case "peaceful" -> Difficulty.PEACEFUL.getDisplayName();
                    case "easy" -> Difficulty.EASY.getDisplayName();
                    case "hard" -> Difficulty.HARD.getDisplayName();
                    default -> Difficulty.NORMAL.getDisplayName();
                }), false);
            }
            case DEFAULT_GAME_MODE -> {
                GameType gameType = gameTypeFromValue(value);
                server.getCommands().performPrefixedCommand(source, "defaultgamemode " + gameType.getName());
                CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), PlayerInfoSync.All.fromPlayerList(server));
                sp.sendSystemMessage(Component.translatable("commands.defaultgamemode.success", gameType.getLongDisplayName()), false);
            }
            case GAME_MODE -> {
                ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
                if (affectPlayer == null || !PlayerTrustPolicy.canManagePlayerOptions(sp, affectPlayer)) return;
                GameType gameType = gameTypeFromValue(value);
                if (affectPlayer.setGameMode(gameType)) {
                    affectPlayer.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.game_mode_changed"), false);
                    if (sp == affectPlayer) sp.sendSystemMessage(Component.translatable("commands.gamemode.success.self", gameType.getLongDisplayName()), false);
                    else sp.sendSystemMessage(Component.translatable("commands.gamemode.success.other", affectPlayer.getDisplayName(), gameType.getLongDisplayName()), false);
                }
            }
            case WORLD_SPAWN -> {
                server.getCommands().performPrefixedCommand(source, "setworldspawn");
                sp.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.world_spawn", pos.getX(), pos.getY(), pos.getZ()), false);
            }
            case PLAYER_SPAWN -> {
                ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
                if (affectPlayer != null && PlayerTrustPolicy.canManagePlayerOptions(sp, affectPlayer)) {
                    server.getCommands().performPrefixedCommand(source, "spawnpoint %s ~ ~ ~".formatted(affectPlayer.getGameProfile().name()));
                    sp.sendSystemMessage(Component.translatable("legacy.menu.host_options.message.player_spawn", affectPlayer.getGameProfile().name(), pos.getX(), pos.getY(), pos.getZ()), false);
                }
            }
            case TRUST_PLAYERS, KICK, TELEPORT_TO_PLAYER, TELEPORT_TO_ME -> {
            }
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    private static GameType gameTypeFromValue(String value) {
        return switch (value) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };
    }

    private static boolean isGameTypeValue(String value) {
        return value.equals("survival") || value.equals("creative") || value.equals("adventure") || value.equals("spectator");
    }

    private static boolean setWeather(MinecraftServer server, ServerPlayer player, String value) {
        switch (value) {
            case "clear" -> setClearWeather(server, player);
            case "rain" -> setWeather(server, duration(player, ServerLevel.RAIN_DURATION), true, false);
            case "thunder" -> setWeather(server, duration(player, ServerLevel.THUNDER_DURATION), true, true);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void setClearWeather(MinecraftServer server, ServerPlayer player) {
        server.setWeatherParameters(duration(player, ServerLevel.RAIN_DELAY), 0, false, false);
        applyWeather(server, 0.0f, 0.0f);
    }

    private static void setWeather(MinecraftServer server, int duration, boolean raining, boolean thundering) {
        server.setWeatherParameters(0, duration, raining, thundering);
        applyWeather(server, raining ? 1.0f : 0.0f, thundering ? 1.0f : 0.0f);
    }

    private static int duration(ServerPlayer player, IntProvider provider) {
        return provider.sample(player.level().getRandom());
    }

    private static void applyWeather(MinecraftServer server, float rain, float thunder) {
        boolean raining = rain > 0.0f;
        server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(raining ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0.0f));
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.canHaveWeather()) continue;
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
            server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain), level.dimension());
            server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder), level.dimension());
        }
    }

    public enum Action {
        COMMAND,
        TIME,
        WEATHER,
        DIFFICULTY,
        DEFAULT_GAME_MODE,
        GAME_MODE,
        WORLD_SPAWN,
        PLAYER_SPAWN,
        TRUST_PLAYERS,
        KICK,
        TELEPORT_TO_PLAYER,
        TELEPORT_TO_ME
    }
}
