package wily.legacy.mixin.base;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?}
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientboundAwardStatsPacket.class)
public interface ClientBoundAwardStatsPacketAccessor {
    //? if >=1.20.5 {
    @Accessor("STAT_VALUES_STREAM_CODEC")
    static StreamCodec<RegistryFriendlyByteBuf, Object2IntMap<Stat<?>>> getStatsValueCodec(){
        return null;
    }
    //?} else {
    /*@Invoker("readStatCap")
    static <T> Stat<T> decodeStatCap(FriendlyByteBuf friendlyByteBuf, StatType<T> statType) {
        return null;
    }
    @Invoker("writeStatCap")
    static <T> void encodeStatCap(FriendlyByteBuf friendlyByteBuf, Stat<T> stat) {

    }
    *///?}
}
