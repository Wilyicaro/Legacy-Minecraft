package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record ClientAdvancementsPacket(List<AdvancementHolder> collection) implements CommonPacket{
    public static AdvancementTree advancementTree = new AdvancementTree();

    public ClientAdvancementsPacket(RegistryFriendlyByteBuf buf) {
        this(AdvancementHolder.LIST_STREAM_CODEC.decode(buf));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        AdvancementHolder.LIST_STREAM_CODEC.encode(buf,collection);
    }

    @Override
    public void apply(NetworkManager.PacketContext ctx) {
        advancementTree.clear();
        advancementTree.addAll(collection);
    }
}
