package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public interface CommonPacket {
    void encode(FriendlyByteBuf buf);

    void apply(Supplier<NetworkManager.PacketContext> ctx);
}
