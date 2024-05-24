package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Supplier;

public interface CommonPacket {
    void encode(RegistryFriendlyByteBuf buf);

    void apply(NetworkManager.PacketContext ctx);
}
