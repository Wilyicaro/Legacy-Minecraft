package wily.legacy.network;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.function.Supplier;

public record ClientAdvancementsPacket(Map<ResourceLocation,Advancement.Builder> map) implements CommonNetwork.Packet{
    public static AdvancementList advancementTree = new AdvancementList();

    public ClientAdvancementsPacket(FriendlyByteBuf buf) {
        this(buf.readMap(FriendlyByteBuf::readResourceLocation,Advancement.Builder::fromNetwork));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(map, FriendlyByteBuf::writeResourceLocation,(b, h)->h.serializeToNetwork(b));
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        advancementTree.clear();
        advancementTree.add(map);
    }
}
