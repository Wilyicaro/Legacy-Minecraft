package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import wily.legacy.Legacy4JClient;
import wily.legacy.player.LegacyPlayer;
import wily.legacy.player.LegacyPlayerInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record PlayerInfoSync(int type, UUID uuid) implements CommonNetwork.Packet {
    public PlayerInfoSync(FriendlyByteBuf buf){
        this(buf.readVarInt(),buf.readUUID());
    }
    public PlayerInfoSync(int type, Player player){
        this(type,player.getUUID());
    }
    public PlayerInfoSync(int type, GameProfile profile){
        this(type,profile.getId());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeUUID(uuid);
    }

    public static Map<String,Object> getWritableGameRules(GameRules gameRules){
        Map<String,Object> rules = new HashMap<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
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
            sp = sp.server.getPlayerList().getPlayer(uuid);
            if (sp == null) return;
            if (type == 0) CommonNetwork.sendToPlayer(sp, new All(sp.server.getPlayerList().getPlayers().stream().collect(Collectors.toMap(e -> e.getGameProfile().getId(), e -> (LegacyPlayerInfo) e)), getWritableGameRules(sp.server.getGameRules()),sp.server.getDefaultGameType()));
            else if (type <= 2) ((LegacyPlayer) sp).setCrafting(type == 1);
            else if (type <= 4) ((LegacyPlayerInfo)sp).setDisableExhaustion(type == 3);
            else if (type <= 6) ((LegacyPlayerInfo)sp).setMayFlySurvival(type == 5);
        }
    }
    public record All(Map<UUID, LegacyPlayerInfo> players, Map<String,Object> gameRules,GameType defaultGameType) implements CommonNetwork.Packet {
        public All(FriendlyByteBuf buf){
            this(buf.readMap(HashMap::new, FriendlyByteBuf::readUUID, LegacyPlayerInfo::fromNetwork), buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, b->{
                int type = b.readVarInt();
                if (type == 0) return b.readBoolean();
                else return b.readVarInt();
            }),buf.readEnum(GameType.class));
        }
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeMap(players, FriendlyByteBuf::writeUUID,(b, i)->i.toNetwork(buf));
            buf.writeMap(gameRules,FriendlyByteBuf::writeUtf,(b,obj)-> {
                b.writeVarInt(obj instanceof Boolean ? 0 : 1);
                if (obj instanceof Boolean bol)  b.writeBoolean(bol);
                else if (obj instanceof  Integer i) b.writeVarInt(i);
            });
            buf.writeEnum(defaultGameType);
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            executor.execute(()-> {
                if (player.get().level().isClientSide) {
                    Legacy4JClient.defaultServerGameType = defaultGameType;
                    Legacy4JClient.updateLegacyPlayerInfos(players);
                    GameRules displayRules = player.get().level().getGameRules();
                    GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                        @Override
                        public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                            if (gameRules.containsKey(key.getId())) {
                                if (gameRules.get(key.getId()) instanceof Boolean b && displayRules.getRule(key) instanceof GameRules.BooleanValue v)
                                    v.set(b, null);
                                if (gameRules.get(key.getId()) instanceof Integer i && displayRules.getRule(key) instanceof GameRules.IntegerValue v)
                                    v.set(i, null);
                            }
                        }
                    });
                }
            });
        }
    }
}
