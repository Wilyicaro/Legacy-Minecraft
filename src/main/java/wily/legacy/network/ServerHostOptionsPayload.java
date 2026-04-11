package wily.legacy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

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
        return new ServerHostOptionsPayload(Action.DIFFICULTY, difficulty.getKey(), EMPTY_UUID);
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

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeEnum(action);
        buf.get().writeUtf(value);
        buf.get().writeUUID(player);
    }

    @Override
    public void apply(Context context) {
        if (!(context.player() instanceof ServerPlayer sp) || !sp.hasPermissions(2)) return;
        var server = FactoryAPIPlatform.getEntityServer(sp);
        var source = sp.createCommandSourceStack().withSuppressedOutput();
        BlockPos pos = sp.blockPosition();
        switch (action) {
            case COMMAND -> {
                server.getCommands().performPrefixedCommand(source, value);
                CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), PlayerInfoSync.All.fromPlayerList(server));
            }
            case TIME -> {
                server.getAllLevels().forEach(level -> level.setDayTime("day".equals(value) ? 1000L : 14000L));
                sp.displayClientMessage(Component.translatable("legacy.menu.host_options.message.set_" + value), false);
            }
            case WEATHER -> {
                server.getCommands().performPrefixedCommand(source, "weather " + value);
                sp.displayClientMessage(Component.translatable("legacy.menu.host_options.message.weather." + value), false);
            }
            case DIFFICULTY -> {
                server.getCommands().performPrefixedCommand(source, "difficulty " + value);
                sp.displayClientMessage(Component.translatable("legacy.menu.host_options.message.difficulty", switch (value) {
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
                sp.displayClientMessage(Component.translatable("commands.defaultgamemode.success", gameType.getLongDisplayName()), false);
            }
            case GAME_MODE -> {
                ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
                GameType gameType = gameTypeFromValue(value);
                if (affectPlayer != null && affectPlayer.setGameMode(gameType)) {
                    affectPlayer.displayClientMessage(Component.translatable("legacy.menu.host_options.message.game_mode_changed"), false);
                    if (sp == affectPlayer) sp.displayClientMessage(Component.translatable("commands.gamemode.success.self", gameType.getLongDisplayName()), false);
                    else sp.displayClientMessage(Component.translatable("commands.gamemode.success.other", affectPlayer.getDisplayName(), gameType.getLongDisplayName()), false);
                }
            }
            case WORLD_SPAWN -> {
                if (sp.level().dimension() != Level.OVERWORLD) {
                    sp.displayClientMessage(Component.translatable("commands.setworldspawn.failure.not_overworld"), false);
                    return;
                }
                server.getCommands().performPrefixedCommand(source, "setworldspawn");
                sp.displayClientMessage(Component.translatable("legacy.menu.host_options.message.world_spawn", pos.getX(), pos.getY(), pos.getZ()), false);
            }
            case PLAYER_SPAWN -> {
                ServerPlayer affectPlayer = server.getPlayerList().getPlayer(player);
                if (affectPlayer != null) {
                    server.getCommands().performPrefixedCommand(source, "spawnpoint %s ~ ~ ~".formatted(affectPlayer.getGameProfile().name()));
                    sp.displayClientMessage(Component.translatable("legacy.menu.host_options.message.player_spawn", affectPlayer.getGameProfile().name(), pos.getX(), pos.getY(), pos.getZ()), false);
                }
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

    public enum Action {
        COMMAND,
        TIME,
        WEATHER,
        DIFFICULTY,
        DEFAULT_GAME_MODE,
        GAME_MODE,
        WORLD_SPAWN,
        PLAYER_SPAWN
    }
}
