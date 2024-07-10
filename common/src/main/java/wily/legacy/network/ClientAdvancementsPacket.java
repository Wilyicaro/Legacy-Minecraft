package wily.legacy.network;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Supplier;

public record ClientAdvancementsPacket(List<AdvancementHolder> collection) implements CommonNetwork.Packet {
    public static AdvancementTree advancementTree = new AdvancementTree();

    public ClientAdvancementsPacket(RegistryFriendlyByteBuf buf) {
        this(AdvancementHolder.LIST_STREAM_CODEC.decode(buf));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        AdvancementHolder.LIST_STREAM_CODEC.encode(buf,collection);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        advancementTree.clear();
        advancementTree.addAll(collection);
    }
}
