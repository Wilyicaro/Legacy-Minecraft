package wily.legacy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CommonNetwork {
    abstract class SecureExecutor implements Executor {
        public abstract boolean isSecure();
        final Collection<BooleanSupplier> queue = new ConcurrentLinkedQueue<>();
        public void executeAll(){
            queue.removeIf(BooleanSupplier::getAsBoolean);
        }
        public void execute(Runnable runnable){
            executeWhen(()->{
                if (isSecure()) {
                    runnable.run();
                    return true;
                }return false;
            });
        }
        public void executeWhen(BooleanSupplier supplier){
            queue.add(supplier);
        }
        public void clear(){
            queue.clear();
        }

    }
    ResourceLocation NETWORK = new ResourceLocation(Legacy4J.MOD_ID,"network");
    ResourceLocation CLIENT_NETWORK = new ResourceLocation(Legacy4J.MOD_ID,"client_network");
    List<Class<? extends Packet>> PACKET_CLASSES = new ArrayList<>();
    List<Function<FriendlyByteBuf, Packet>> PACKETS_FROM_NETWORK = new ArrayList<>();


    record PacketHandler(ResourceLocation id, Packet commonPacket) implements Packet{

        public static PacketHandler decodeClient(FriendlyByteBuf buf) {
            return new PacketHandler(CLIENT_NETWORK,PACKETS_FROM_NETWORK.get(buf.readVarInt()).apply(buf));
        }
        public static PacketHandler decode(FriendlyByteBuf buf) {
            return new PacketHandler(NETWORK,PACKETS_FROM_NETWORK.get(buf.readVarInt()).apply(buf));
        }
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(PACKET_CLASSES.indexOf(commonPacket.getClass()));
            commonPacket.encode(buf);
        }

        @Override
        public void apply(SecureExecutor executor, Supplier<Player> player) {
            commonPacket.apply(executor, player);
        }
    }
    interface PayloadRegister {
        void register(boolean client, ResourceLocation id, Function<FriendlyByteBuf, PacketHandler> codec, Consumer<PacketHandler> apply);
    }
    static void registerPayloads(PayloadRegister registry){
        registry.register(true, CLIENT_NETWORK, PacketHandler::decodeClient, PacketHandler::apply);
        registry.register(false, NETWORK, PacketHandler::decode, PacketHandler::apply);
    }

    static void register() {
        register(PlayerInfoSync.class, PlayerInfoSync::new);
        register(PlayerInfoSync.All.class, PlayerInfoSync.All::new);
        register(ServerOpenClientMenuPacket.class, ServerOpenClientMenuPacket::new);
        register(ServerMenuCraftPacket.class, ServerMenuCraftPacket::new);
        register(TipCommand.Packet.class, TipCommand.Packet::decode);
        register(TipCommand.EntityPacket.class, TipCommand.EntityPacket::new);
        register(ClientAdvancementsPacket.class, ClientAdvancementsPacket::new);
        register(ClientAnimalInLoveSyncPacket.class, ClientAnimalInLoveSyncPacket::new);
        register(ServerPlayerMissHitPacket.class, ServerPlayerMissHitPacket::new);
        register(TopMessage.Packet.class, TopMessage.Packet::decode);
        register(ClientEffectActivationPacket.class, ClientEffectActivationPacket::new);
    }

    static void register(Class<? extends Packet> commonPacketClass, Function<FriendlyByteBuf, Packet> function){
        PACKET_CLASSES.add(commonPacketClass);
        PACKETS_FROM_NETWORK.add(function);
    }
    static<T extends Packet> void sendToServer(T commonPacket){
        if (Legacy4JClient.isModEnabledOnServer()) Legacy4JPlatform.sendToServer(new PacketHandler(CLIENT_NETWORK,commonPacket));
    }
    static<T extends Packet> void sendToPlayer(ServerPlayer serverPlayer, T commonPacket){
        Legacy4JPlatform.sendToPlayer(serverPlayer,new PacketHandler(NETWORK,commonPacket));
    }
    static<T extends Packet> void sendToPlayers(Iterable<ServerPlayer> players, T commonPacket){
        players.forEach(p-> sendToPlayer(p,commonPacket));
    }
    interface Consumer<T> {
        void apply(T packet, SecureExecutor executor, Supplier<Player> player);
    }

    interface Packet extends Consumer<Packet> {
        void encode(FriendlyByteBuf buf);
        void apply(SecureExecutor executor, Supplier<Player> player);
        default void apply(Packet packet, SecureExecutor executor, Supplier<Player> player){
            packet.apply(executor, player);
        }
    }
}
