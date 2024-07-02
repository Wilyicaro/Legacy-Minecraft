package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import wily.legacy.Legacy4J;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommonNetworkManager {

    public static final ResourceLocation NETWORK = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"network");
    public static final ResourceLocation CLIENT_NETWORK = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"client_network");
    private static final List<Class<? extends CommonPacket>> PACKET_CLASSES = new ArrayList<>();
    private static final List<Function<RegistryFriendlyByteBuf,CommonPacket>> PACKETS_FROM_NETWORK = new ArrayList<>();


    public record PacketHandler(CustomPacketPayload.Type<? extends CustomPacketPayload> type,CommonPacket commonPacket) implements CustomPacketPayload,CommonPacket{
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
        public void apply(NetworkManager.PacketContext ctx) {
            commonPacket.apply(ctx);
        }
    }

    public static void init(){
        NetworkManager.registerReceiver(NetworkManager.c2s(), PacketHandler.CLIENT_TYPE, PacketHandler.CLIENT_CODEC, PacketHandler::apply);
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), PacketHandler.TYPE, PacketHandler.CODEC, PacketHandler::apply);
        } else {
            NetworkManager.registerS2CPayloadType(PacketHandler.TYPE, PacketHandler.CODEC);
        }
    }

    public static void register(Class<? extends CommonPacket> commonPacketClass, Function<RegistryFriendlyByteBuf,CommonPacket> function){
        PACKET_CLASSES.add(commonPacketClass);
        PACKETS_FROM_NETWORK.add(function);
    }
    public static<T extends CommonPacket> void sendToServer(T commonPacket){
        NetworkManager.sendToServer(new PacketHandler(PacketHandler.CLIENT_TYPE,commonPacket));
    }
    public static<T extends CommonPacket> void sendToPlayer(ServerPlayer serverPlayer, T commonPacket){
        NetworkManager.sendToPlayer(serverPlayer,new PacketHandler(PacketHandler.TYPE,commonPacket));
    }
    public static<T extends CommonPacket> void sendToPlayers(Iterable<ServerPlayer> players, T commonPacket){
        NetworkManager.sendToPlayers(players,new PacketHandler(PacketHandler.TYPE,commonPacket));
    }
}
