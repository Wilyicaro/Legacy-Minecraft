package wily.legacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

    }
    ResourceLocation NETWORK = new ResourceLocation(Legacy4J.MOD_ID,"network");
    ResourceLocation CLIENT_NETWORK = new ResourceLocation(Legacy4J.MOD_ID,"client_network");
    List<Class<? extends Packet>> PACKET_CLASSES = new ArrayList<>();
    List<Function<RegistryFriendlyByteBuf, Packet>> PACKETS_FROM_NETWORK = new ArrayList<>();


    record PacketHandler(CustomPacketPayload.Type<? extends CustomPacketPayload> type, Packet commonPacket) implements CustomPacketPayload, Packet {
        public static final Type<PacketHandler> TYPE = new Type<>(NETWORK);
        public static final Type<PacketHandler> CLIENT_TYPE = new Type<>(CLIENT_NETWORK);
        public static final StreamCodec<RegistryFriendlyByteBuf,PacketHandler> CODEC = StreamCodec.of((p,b)->b.encode(p), PacketHandler::decode);
        public static final StreamCodec<RegistryFriendlyByteBuf,PacketHandler> CLIENT_CODEC = StreamCodec.of((p,b)->b.encode(p), PacketHandler::decodeClient);

        public static PacketHandler decodeClient(RegistryFriendlyByteBuf buf) {
            return new PacketHandler(CLIENT_TYPE,PACKETS_FROM_NETWORK.get(buf.readVarInt()).apply(buf));
        }
        public static PacketHandler decode(RegistryFriendlyByteBuf buf) {
            return new PacketHandler(TYPE,PACKETS_FROM_NETWORK.get(buf.readVarInt()).apply(buf));
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(PACKET_CLASSES.indexOf(commonPacket.getClass()));
            commonPacket.encode(buf);
        }

        @Override
        public void apply(SecureExecutor executor, Supplier<Player> player) {
            commonPacket.apply(executor, player);
        }
    }
    interface PayloadRegister {
        <T extends CustomPacketPayload> void register(boolean client, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, Consumer<T> apply);
    }
    static void registerPayloads(PayloadRegister registry){
        registry.register(true, PacketHandler.CLIENT_TYPE, PacketHandler.CLIENT_CODEC, PacketHandler::apply);
        registry.register(false, PacketHandler.TYPE, PacketHandler.CODEC, PacketHandler::apply);
    }

    static void register() {
        register(PlayerInfoSync.class, PlayerInfoSync::new);
        register(PlayerInfoSync.All.class, PlayerInfoSync.All::new);
        register(ServerOpenClientMenuPacket.class, ServerOpenClientMenuPacket::new);
        register(ServerMenuCraftPacket.class, ServerMenuCraftPacket::new);
        register(TipCommand.Packet.class, TipCommand.Packet::decode);
        register(TipCommand.EntityPacket.class, TipCommand.EntityPacket::new);
        register(ClientAdvancementsPacket.class, ClientAdvancementsPacket::new);
        register(ClientEntityDataSyncPacket.class, ClientEntityDataSyncPacket::new);
        register(ServerPlayerMissHitPacket.class, ServerPlayerMissHitPacket::new);
    }

    static void register(Class<? extends Packet> commonPacketClass, Function<RegistryFriendlyByteBuf, Packet> function){
        PACKET_CLASSES.add(commonPacketClass);
        PACKETS_FROM_NETWORK.add(function);
    }
    static<T extends Packet> void sendToServer(T commonPacket){
        if (Legacy4JClient.isModEnabledOnServer()) Legacy4JPlatform.sendToServer(new PacketHandler(PacketHandler.CLIENT_TYPE,commonPacket));
    }
    static<T extends Packet> void sendToPlayer(ServerPlayer serverPlayer, T commonPacket){
        Legacy4JPlatform.sendToPlayer(serverPlayer,new PacketHandler(PacketHandler.TYPE,commonPacket));
    }
    static<T extends Packet> void sendToPlayers(Iterable<ServerPlayer> players, T commonPacket){
        players.forEach(p-> sendToPlayer(p,commonPacket));
    }
    interface Consumer<T> {
        void apply(T packet, SecureExecutor executor, Supplier<Player> player);
    }

    interface Packet extends Consumer<Packet> {
        void encode(RegistryFriendlyByteBuf buf);
        void apply(SecureExecutor executor, Supplier<Player> player);
        default void apply(Packet packet, SecureExecutor executor, Supplier<Player> player){
            packet.apply(executor, player);
        }
    }
}
