package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record PlayerInfoSync(Sync sync, UUID player) implements CommonNetwork.Payload {
    public enum Sync {
        ASK_ALL,CLASSIC_CRAFTING,LEGACY_CRAFTING,DISABLE_EXHAUSTION,ENABLE_EXHAUSTION,ENABLE_MAY_FLY_SURVIVAL,DISABLE_MAY_FLY_SURVIVAL,CLASSIC_TRADING,LEGACY_TRADING,CLASSIC_STONECUTTING,LEGACY_STONECUTTING,CLASSIC_LOOM,LEGACY_LOOM;
    }

    public static final CommonNetwork.Identifier<PlayerInfoSync> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_c2s"),PlayerInfoSync::new);

    public PlayerInfoSync(CommonNetwork.PlayBuf buf){
        this(buf.get().readEnum(Sync.class),buf.get().readUUID());
    }

    public static PlayerInfoSync askAll(Player player){
        return new PlayerInfoSync(Sync.ASK_ALL, player);
    }

    public static PlayerInfoSync classicCrafting(boolean classic, Player player){
        return new PlayerInfoSync(classic ? Sync.CLASSIC_CRAFTING : Sync.LEGACY_CRAFTING, player);
    }

    public static PlayerInfoSync classicTrading(boolean classic, Player player){
        return new PlayerInfoSync(classic ? Sync.CLASSIC_TRADING : Sync.LEGACY_TRADING, player);
    }

    public static PlayerInfoSync classicStonecutting(boolean classic, Player player){
        return new PlayerInfoSync(classic ? Sync.CLASSIC_STONECUTTING : Sync.LEGACY_STONECUTTING, player);
    }

    public static PlayerInfoSync classicLoom(boolean classic, Player player){
        return new PlayerInfoSync(classic ? Sync.CLASSIC_LOOM : Sync.LEGACY_LOOM, player);
    }

    public static PlayerInfoSync disableExhaustion(boolean disableExhaustion, GameProfile profile){
        return new PlayerInfoSync(disableExhaustion ? Sync.DISABLE_EXHAUSTION : Sync.ENABLE_EXHAUSTION, profile);
    }

    public static PlayerInfoSync mayFlySurvival(boolean mayFlySurvival, GameProfile profile){
        return new PlayerInfoSync(mayFlySurvival ? Sync.ENABLE_MAY_FLY_SURVIVAL : Sync.DISABLE_MAY_FLY_SURVIVAL, profile);
    }

    public PlayerInfoSync(Sync sync, Player player){
        this(sync,player.getUUID());
    }

    public PlayerInfoSync(Sync sync, GameProfile profile){
        this(sync,profile.getId());
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeEnum(sync);
        buf.get().writeUUID(player);
    }

    public static Map<String,Object> getWritableGameRules(GameRules gameRules){
        Map<String,Object> rules = new HashMap<>();
        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                rules.put(key.getId(), gameRules.getRule(key).get());
            }
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                rules.put(key.getId(), gameRules.getRule(key).get());
            }
        });
        return rules;
    }
    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp) {
            sp = sp.server.getPlayerList().getPlayer(player);
            if (sp == null) return;
            switch (sync){
                case ASK_ALL -> CommonNetwork.sendToPlayer(sp, All.fromPlayerList(sp.server));
                case CLASSIC_CRAFTING,LEGACY_CRAFTING -> ((LegacyPlayer) sp).setCrafting(sync == Sync.CLASSIC_CRAFTING);
                case CLASSIC_TRADING,LEGACY_TRADING -> ((LegacyPlayer) sp).setTrading(sync == Sync.CLASSIC_TRADING);
                case CLASSIC_STONECUTTING,LEGACY_STONECUTTING -> ((LegacyPlayer) sp).setStonecutting(sync == Sync.CLASSIC_STONECUTTING);
                case CLASSIC_LOOM,LEGACY_LOOM -> ((LegacyPlayer) sp).setLoom(sync == Sync.CLASSIC_LOOM);
                case DISABLE_EXHAUSTION,ENABLE_EXHAUSTION -> ((LegacyPlayerInfo)sp).setDisableExhaustion(sync == Sync.DISABLE_EXHAUSTION);
                case ENABLE_MAY_FLY_SURVIVAL,DISABLE_MAY_FLY_SURVIVAL -> ((LegacyPlayerInfo)sp).setMayFlySurvival(sync == Sync.ENABLE_MAY_FLY_SURVIVAL);
            }
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    public record All(Map<UUID, LegacyPlayerInfo> players, Map<String,Object> gameRules, GameType defaultGameType, CommonNetwork.Identifier<All> identifier) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<All> ID_C2S = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_all_c2s"),b->new All(b, All.ID_C2S));
        public static final CommonNetwork.Identifier<All> ID_S2C = CommonNetwork.Identifier.create(Legacy4J.createModLocation("player_info_sync_all_s2c"),b->new All(b, All.ID_S2C));

        public All(Map<String,Object> gameRules,CommonNetwork.Identifier<All> identifier){
            this(Collections.emptyMap(),gameRules,GameType.SURVIVAL,identifier);
        }

        public static final List<GameRules.Key<GameRules.BooleanValue>> NON_OP_GAMERULES = new ArrayList<>(List.of(GameRules.RULE_DOFIRETICK, LegacyGameRules.TNT_EXPLODES,GameRules.RULE_DOMOBLOOT,GameRules.RULE_DOBLOCKDROPS,GameRules.RULE_NATURAL_REGENERATION,LegacyGameRules.GLOBAL_MAP_PLAYER_ICON,GameRules.RULE_DO_IMMEDIATE_RESPAWN));

        public All(CommonNetwork.PlayBuf buf, CommonNetwork.Identifier<All> identifier){
            this(buf.get().readMap(HashMap::new, b-> b.readUUID(), b-> LegacyPlayerInfo.decode(buf)), buf.get().readMap(HashMap::new, FriendlyByteBuf::readUtf, b->{
                int type = b.readVarInt();
                if (type == 0) return b.readBoolean();
                else return b.readVarInt();
            }),buf.get().readEnum(GameType.class),identifier);
        }

        public static All fromPlayerList(MinecraftServer server){
            return new All(server.getPlayerList().getPlayers().stream().collect(Collectors.toMap(e -> e.getGameProfile().getId(), e -> (LegacyPlayerInfo) e)), getWritableGameRules(server.getGameRules()), server.getDefaultGameType(), All.ID_S2C);
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeMap(players, (b,u)-> b.writeUUID(u), (b,info)-> LegacyPlayerInfo.encode(buf, info));
            buf.get().writeMap(gameRules,FriendlyByteBuf::writeUtf,(b,obj)-> {
                b.writeVarInt(obj instanceof Boolean ? 0 : 1);
                if (obj instanceof Boolean bol)  b.writeBoolean(bol);
                else if (obj instanceof  Integer i) b.writeVarInt(i);
            });
            buf.get().writeEnum(defaultGameType);
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
            executor.executeWhen(()->{
                if (p.get() != null && p.get().level().isClientSide && FactoryAPIClient.hasModOnServer){
                    Legacy4JClient.defaultServerGameType = defaultGameType;
                    Legacy4JClient.updateLegacyPlayerInfos(players);
                    return true;
                }
                return false;
            });
            executor.execute(()-> {
                GameRules displayRules = p.get() instanceof ServerPlayer sp ? sp.getServer().getGameRules() : Legacy4JClient.gameRules;
                displayRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                    @Override
                    public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                        if (gameRules.containsKey(key.getId()) && (p.get().level().isClientSide || NON_OP_GAMERULES.contains(key) || p.get().hasPermissions(2))) {
                            if (gameRules.get(key.getId()) instanceof Boolean b && displayRules.getRule(key) instanceof GameRules.BooleanValue v)
                                v.set(b, null);
                            if (gameRules.get(key.getId()) instanceof Integer i && displayRules.getRule(key) instanceof GameRules.IntegerValue v)
                                v.set(i, null);
                        }
                    }
                });
            });
        }
    }
}
