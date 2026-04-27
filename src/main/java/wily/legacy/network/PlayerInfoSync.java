package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.*;
import java.util.stream.Collectors;

public record PlayerInfoSync(Sync sync, UUID player) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<PlayerInfoSync> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_c2s"), PlayerInfoSync::new);

    public PlayerInfoSync(CommonNetwork.PlayBuf buf) {
        this(Sync.byId(buf.get().readVarInt()), buf.get().readUUID());
    }

    public PlayerInfoSync(Sync sync, Player player) {
        this(sync, player.getUUID());
    }

    public PlayerInfoSync(Sync sync, GameProfile profile) {
        this(sync, profile.id());
    }

    public static PlayerInfoSync askAll(Player player) {
        return new PlayerInfoSync(Sync.ASK_ALL, player);
    }

    public static PlayerInfoSync classicCrafting(boolean classic, Player player) {
        return new PlayerInfoSync(classic ? Sync.CLASSIC_CRAFTING : Sync.LEGACY_CRAFTING, player);
    }

    public static PlayerInfoSync classicTrading(boolean classic, Player player) {
        return new PlayerInfoSync(classic ? Sync.CLASSIC_TRADING : Sync.LEGACY_TRADING, player);
    }

    public static PlayerInfoSync classicStonecutting(boolean classic, Player player) {
        return new PlayerInfoSync(classic ? Sync.CLASSIC_STONECUTTING : Sync.LEGACY_STONECUTTING, player);
    }

    public static PlayerInfoSync classicLoom(boolean classic, Player player) {
        return new PlayerInfoSync(classic ? Sync.CLASSIC_LOOM : Sync.LEGACY_LOOM, player);
    }

    public static PlayerInfoSync disableExhaustion(boolean disableExhaustion, GameProfile profile) {
        return new PlayerInfoSync(disableExhaustion ? Sync.DISABLE_EXHAUSTION : Sync.ENABLE_EXHAUSTION, profile);
    }

    public static PlayerInfoSync mayFlySurvival(boolean mayFlySurvival, GameProfile profile) {
        return new PlayerInfoSync(mayFlySurvival ? Sync.ENABLE_MAY_FLY_SURVIVAL : Sync.DISABLE_MAY_FLY_SURVIVAL, profile);
    }

    public static PlayerInfoSync invisibility(boolean invisible, GameProfile profile) {
        return new PlayerInfoSync(invisible ? Sync.ENABLE_INVISIBILITY : Sync.DISABLE_INVISIBILITY, profile);
    }

    public static Map<Identifier, Integer> getWritableGameRules(GameRules gameRules) {
        Map<Identifier, Integer> rules = new HashMap<>();
        gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> gameRule) {
                rules.put(gameRule.getIdentifier(), gameRule.getCommandResult(gameRules.get(gameRule)));
            }
        });
        return rules;
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeVarInt(sync.id());
        buf.get().writeUUID(player);
    }

    @Override
    public void apply(Context context) {
        if (context.player() instanceof ServerPlayer sp) {
            ServerPlayer affectPlayer;
            boolean shouldSyncPlayerInfo = false;
            if (sp.getUUID().equals(player)) {
                switch (sync) {
                    case ASK_ALL ->
                            CommonNetwork.sendToPlayer(sp, All.fromPlayerList(FactoryAPIPlatform.getEntityServer(sp)));
                    case CLASSIC_CRAFTING, LEGACY_CRAFTING ->
                            ((LegacyPlayer) sp).setCrafting(sync == Sync.CLASSIC_CRAFTING);
                    case CLASSIC_TRADING, LEGACY_TRADING ->
                            ((LegacyPlayer) sp).setTrading(sync == Sync.CLASSIC_TRADING);
                    case CLASSIC_STONECUTTING, LEGACY_STONECUTTING ->
                            ((LegacyPlayer) sp).setStonecutting(sync == Sync.CLASSIC_STONECUTTING);
                    case CLASSIC_LOOM, LEGACY_LOOM -> ((LegacyPlayer) sp).setLoom(sync == Sync.CLASSIC_LOOM);
                }
                affectPlayer = sp;
            } else affectPlayer = FactoryAPIPlatform.getEntityServer(sp).getPlayerList().getPlayer(player);
            if (affectPlayer == null) return;
            if (sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                switch (sync) {
                    case DISABLE_EXHAUSTION, ENABLE_EXHAUSTION -> {
                        boolean disableExhaustion = sync == Sync.DISABLE_EXHAUSTION;
                        LegacyPlayerInfo info = (LegacyPlayerInfo) affectPlayer;
                        if (info.isExhaustionDisabled() != disableExhaustion) {
                            info.setDisableExhaustion(disableExhaustion);
                            affectPlayer.sendSystemMessage(Component.translatable(disableExhaustion ? "legacy.menu.host_options.player.disableExhaustion.enabled" : "legacy.menu.host_options.player.disableExhaustion.disabled"), false);
                            shouldSyncPlayerInfo = true;
                        }
                    }
                    case ENABLE_MAY_FLY_SURVIVAL, DISABLE_MAY_FLY_SURVIVAL -> {
                        boolean mayFlySurvival = sync == Sync.ENABLE_MAY_FLY_SURVIVAL;
                        LegacyPlayerInfo info = (LegacyPlayerInfo) affectPlayer;
                        if (info.mayFlySurvival() != mayFlySurvival) {
                            LegacyPlayerInfo.setAndUpdateMayFlySurvival(affectPlayer, mayFlySurvival, true);
                            affectPlayer.sendSystemMessage(Component.translatable(mayFlySurvival ? "legacy.menu.host_options.player.mayFly.enabled" : "legacy.menu.host_options.player.mayFly.disabled"), false);
                            shouldSyncPlayerInfo = true;
                        }
                    }
                    case ENABLE_INVISIBILITY, DISABLE_INVISIBILITY -> {
                        boolean visible = sync == Sync.DISABLE_INVISIBILITY;
                        LegacyPlayerInfo info = (LegacyPlayerInfo) affectPlayer;
                        if (info.isVisible() != visible) {
                            info.setVisibility(visible);
                            affectPlayer.sendSystemMessage(Component.translatable(visible ? "legacy.menu.host_options.player.invisible.disabled" : "legacy.menu.host_options.player.invisible.enabled"), false);
                            affectPlayer.sendSystemMessage(Component.translatable(visible ? "legacy.menu.host_options.player.invulnerable.disabled" : "legacy.menu.host_options.player.invulnerable.enabled"), false);
                            shouldSyncPlayerInfo = true;
                        }
                    }
                }
            }
            if (shouldSyncPlayerInfo) syncPlayerInfo(affectPlayer);
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    public enum Sync {
        ASK_ALL(0),
        CLASSIC_CRAFTING(1),
        LEGACY_CRAFTING(2),
        DISABLE_EXHAUSTION(3),
        ENABLE_EXHAUSTION(4),
        ENABLE_MAY_FLY_SURVIVAL(5),
        DISABLE_MAY_FLY_SURVIVAL(6),
        CLASSIC_TRADING(7),
        LEGACY_TRADING(8),
        CLASSIC_STONECUTTING(9),
        LEGACY_STONECUTTING(10),
        CLASSIC_LOOM(11),
        LEGACY_LOOM(12),
        ENABLE_INVISIBILITY(13),
        DISABLE_INVISIBILITY(14);

        private final int id;

        Sync(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Sync byId(int id) {
            return switch (id) {
                case 0 -> ASK_ALL;
                case 1 -> CLASSIC_CRAFTING;
                case 2 -> LEGACY_CRAFTING;
                case 3 -> DISABLE_EXHAUSTION;
                case 4 -> ENABLE_EXHAUSTION;
                case 5 -> ENABLE_MAY_FLY_SURVIVAL;
                case 6 -> DISABLE_MAY_FLY_SURVIVAL;
                case 7 -> CLASSIC_TRADING;
                case 8 -> LEGACY_TRADING;
                case 9 -> CLASSIC_STONECUTTING;
                case 10 -> LEGACY_STONECUTTING;
                case 11 -> CLASSIC_LOOM;
                case 12 -> LEGACY_LOOM;
                case 13 -> ENABLE_INVISIBILITY;
                case 14 -> DISABLE_INVISIBILITY;
                default -> throw new IllegalArgumentException("Unknown player info sync id: " + id);
            };
        }
    }

    private static void syncPlayerInfo(ServerPlayer player) {
        MinecraftServer server = FactoryAPIPlatform.getEntityServer(player);
        if (server == null) return;
        CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers(), new All(Map.of(player.getUUID(), (LegacyPlayerInfo) player), Collections.emptyMap(), server.getDefaultGameType(), All.ID_S2C));
    }

    public record All(Map<UUID, LegacyPlayerInfo> players, Map<Identifier, Integer> gameRules, GameType defaultGameType,
                      CommonNetwork.Identifier<All> identifier) implements CommonNetwork.Payload {
        public static final List<GameRule<Boolean>> NON_OP_GAMERULES = new ArrayList<>(List.of(GameRules.FIRE_DAMAGE, LegacyGameRules.getTntExplodes(), GameRules.MOB_DROPS, GameRules.BLOCK_DROPS, GameRules.NATURAL_HEALTH_REGENERATION, LegacyGameRules.GLOBAL_MAP_PLAYER_ICON.get(), LegacyGameRules.LEGACY_SWIMMING.get(), GameRules.IMMEDIATE_RESPAWN));
        public static final CommonNetwork.Identifier<All> ID_C2S = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_all_c2s"), b -> new All(b, All.ID_C2S));
        public static final CommonNetwork.Identifier<All> ID_S2C = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_all_s2c"), b -> new All(b, All.ID_S2C));

        public All(Map<Identifier, Integer> gameRules, CommonNetwork.Identifier<All> identifier) {
            this(Collections.emptyMap(), gameRules, GameType.SURVIVAL, identifier);
        }

        public All(CommonNetwork.PlayBuf buf, CommonNetwork.Identifier<All> identifier) {
            this(buf.get().readMap(HashMap::new, b -> b.readUUID(), b -> LegacyPlayerInfo.decode(buf)), buf.get().readMap(HashMap::new, FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readVarInt), buf.get().readEnum(GameType.class), identifier);
        }

        public static <T> void syncGamerule(GameRule<T> key, T value, MinecraftServer server) {
            if (server != null) {
                All payload = new All(Collections.emptyMap(), Map.of(key.getIdentifier(), key.getCommandResult(value)), server.getDefaultGameType(), All.ID_S2C);
                server.getPlayerList().getPlayers().forEach(sp -> CommonNetwork.sendToPlayer(sp, payload));
            }
        }

        public static All fromPlayerList(MinecraftServer server) {
            return new All(server.getPlayerList().getPlayers().stream().collect(Collectors.toMap(e -> e.getGameProfile().id(), e -> (LegacyPlayerInfo) e)), getWritableGameRules(server.getGameRules()), server.getDefaultGameType(), All.ID_S2C);
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeMap(players, (b, u) -> b.writeUUID(u), (b, info) -> LegacyPlayerInfo.encode(buf, info));
            buf.get().writeMap(gameRules, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeVarInt);
            buf.get().writeEnum(defaultGameType);
        }

        @Override
        public void apply(Context context) {
            context.executor().executeWhen(() -> {
                if (context.isClient() && Legacy4JClient.hasModOnServer()) {
                    Legacy4JClient.defaultServerGameType = defaultGameType;
                    Legacy4JClient.updateLegacyPlayerInfos(players);
                    return true;
                }
                return false;
            });
            context.executor().execute(() -> {
                GameRules displayRules = context.player() instanceof ServerPlayer sp ? sp.level().getGameRules() : Legacy4JClient.gameRules;
                displayRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
                    @Override
                    public void visitBoolean(GameRule<Boolean> gameRule) {
                        Identifier id = gameRule.getIdentifier();
                        if (gameRules.containsKey(id) && (context.player().level().isClientSide() || NON_OP_GAMERULES.contains(gameRule) || context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))) {
                            displayRules.set(gameRule, gameRules.get(id) == 1, null);
                        }
                    }

                    @Override
                    public void visitInteger(GameRule<Integer> gameRule) {
                        Identifier id = gameRule.getIdentifier();
                        if (gameRules.containsKey(id) && (context.player().level().isClientSide() || NON_OP_GAMERULES.contains(gameRule) || context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))) {
                            displayRules.set(gameRule, gameRules.get(id), null);
                        }
                    }
                });
            });
        }
    }
}


