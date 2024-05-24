package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.function.Supplier;

public record ClientAdvancementsPacket(Collection<AdvancementHolder> collection) implements CommonPacket{
    public static AdvancementTree advancementTree = new AdvancementTree();

    public ClientAdvancementsPacket(FriendlyByteBuf buf) {
        this(buf.readList(AdvancementHolder::read));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(collection,(b,h)->h.write(b));
    }

    @Override
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        advancementTree.clear();
        advancementTree.addAll(collection);
    }
}
