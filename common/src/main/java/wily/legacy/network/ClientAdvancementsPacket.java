package wily.legacy.network;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.function.Supplier;

public record ClientAdvancementsPacket(Collection<AdvancementHolder> collection) implements CommonNetwork.Packet{
    public static AdvancementTree advancementTree = new AdvancementTree();

    public ClientAdvancementsPacket(FriendlyByteBuf buf) {
        this(buf.readList(AdvancementHolder::read));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(collection,(b,h)->h.write(b));
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        advancementTree.clear();
        advancementTree.addAll(collection);
    }
}
