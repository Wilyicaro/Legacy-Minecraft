package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import wily.legacy.LegacyMinecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record ServerDisplayInfoSync(int type) implements CommonPacket {
    public ServerDisplayInfoSync(FriendlyByteBuf buf){
        this(buf.readVarInt());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
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
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        Player p = ctx.get().getPlayer();
        if (p instanceof ServerPlayer sp )
            if (type == 0)
                LegacyMinecraft.NETWORK.sendToPlayer(sp, new HostOptions(LegacyMinecraft.playerVisualIds,  getWritableGameRules(sp.server.getGameRules())));
    }
    public record HostOptions(Map<String,Integer> players, Map<String,Object> gameRules) implements CommonPacket {
        public HostOptions(FriendlyByteBuf buf){
            this(buf.readMap(Object2IntOpenHashMap::new, FriendlyByteBuf::readUtf,FriendlyByteBuf::readVarInt), buf.readMap(HashMap::new, FriendlyByteBuf::readUtf,b->{
                int type = b.readVarInt();
                if (type == 0) return b.readBoolean();
                else return b.readVarInt();
            }));
        }
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeMap(players,FriendlyByteBuf::writeUtf,FriendlyByteBuf::writeVarInt);
            buf.writeMap(gameRules,FriendlyByteBuf::writeUtf,(b,obj)-> {
                b.writeVarInt(obj instanceof Boolean ? 0 : 1);
                if (obj instanceof Boolean bol)  b.writeBoolean(bol);
                else if (obj instanceof  Integer i) b.writeVarInt(i);
            });
        }

        @Override
        public void apply(Supplier<NetworkManager.PacketContext> ctx) {
            Player p = ctx.get().getPlayer();
            if (p.level().isClientSide){
                LegacyMinecraft.playerVisualIds.clear();
                LegacyMinecraft.playerVisualIds.putAll(players);
                GameRules displayRules = p.level().getGameRules();
                GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                    @Override
                    public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                        if (gameRules.containsKey(key.getId()))  {
                            if (gameRules.get(key.getId()) instanceof Boolean b && displayRules.getRule(key) instanceof GameRules.BooleanValue v)
                                v.set(b,null);
                            if (gameRules.get(key.getId()) instanceof Integer i && displayRules.getRule(key) instanceof GameRules.IntegerValue v)
                                v.set(i,null);
                        }
                    }
                });
            }
        }
    }
}
