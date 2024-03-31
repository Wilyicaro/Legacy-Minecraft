package wily.legacy.network;

import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.player.LegacyPlayer;
import wily.legacy.player.LegacyPlayerInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record PlayerInfoSync(int type, UUID player) implements CommonPacket {
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
        buf.writeUUID(player);
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
        if (p instanceof ServerPlayer sp) {
            sp = sp.server.getPlayerList().getPlayer(player);
            if (sp == null) return;
            if (type == 0) LegacyMinecraft.NETWORK.sendToPlayer(sp, new HostOptions(sp.server.getPlayerList().getPlayers().stream().collect(Collectors.toMap(e -> e.getGameProfile().getId(), e -> (LegacyPlayerInfo) e)), getWritableGameRules(sp.server.getGameRules())));
            else if (type <= 2) ((LegacyPlayer) sp).setCrafting(type == 1);
            else if (type <= 4) ((LegacyPlayerInfo)sp).setDisableExhaustion(type == 3);
            else if (type <= 6) ((LegacyPlayerInfo)sp).setMayFlySurvival(type == 5);
        }
    }
    public record HostOptions(Map<UUID, LegacyPlayerInfo> players, Map<String,Object> gameRules) implements CommonPacket {
        public HostOptions(FriendlyByteBuf buf){
            this(buf.readMap(HashMap::new, FriendlyByteBuf::readUUID, LegacyPlayerInfo::fromNetwork), buf.readMap(HashMap::new, FriendlyByteBuf::readUtf, b->{
                int type = b.readVarInt();
                if (type == 0) return b.readBoolean();
                else return b.readVarInt();
            }));
        }
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeMap(players,FriendlyByteBuf::writeUUID,(b,i)->i.toNetwork(b));
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
                LegacyMinecraftClient.updateLegacyPlayerInfos(players);
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
