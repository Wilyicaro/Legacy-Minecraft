package wily.legacy.network;

//? if <=1.20.1 {
/*import net.minecraft.advancements.AdvancementList;
*///?} else {
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
//?}
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record ClientAdvancementsPayload(/*? if >1.20.1 {*/List<AdvancementHolder> collection/*?} else {*//*Map<ResourceLocation,Advancement.Builder> map*//*?}*/) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ClientAdvancementsPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_advancements"), ClientAdvancementsPayload::new);
    public static /*? if >1.20.1 {*/AdvancementTree/*?} else {*/ /*AdvancementList*//*?}*/ advancements = new /*? if >1.20.1 {*/AdvancementTree/*?} else {*//*AdvancementList*//*?}*/();

    public ClientAdvancementsPayload(CommonNetwork.PlayBuf buf) {
        this(/*? if >1.20.1 {*/buf.get().readList(b->{
            return /*? if <1.20.5 {*//*AdvancementHolder.read(b)*//*?} else {*/new AdvancementHolder(b.readResourceLocation(),Advancement.STREAM_CODEC.decode(buf.get()))/*?}*/;
        })/*?} else {*//*buf.get().readMap(FriendlyByteBuf::readResourceLocation,Advancement.Builder::fromNetwork)*//*?}*/);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        //? if >1.20.1 {
        buf.get().writeCollection(collection,(b,h)-> {
            //? if <1.20.5 {
            /*h.write(b);
            *///?} else {
            b.writeResourceLocation(h.id());
            Advancement.STREAM_CODEC.encode(buf.get(),h.value());
            //?}
        });
        //?} else {
        /*buf.get().writeMap(map, FriendlyByteBuf::writeResourceLocation,(b, h)->h.serializeToNetwork(b));
        *///?}
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        advancements.clear();
        //? if >1.20.1 {
        advancements.addAll(collection);
        //?} else {
        /*advancements.add(map);
        *///?}
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
