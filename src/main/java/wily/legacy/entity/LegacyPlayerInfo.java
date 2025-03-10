package wily.legacy.entity;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.stats.Stat;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.mixin.base.ClientBoundAwardStatsPacketAccessor;

public interface LegacyPlayerInfo {
    static LegacyPlayerInfo of(Object obj) {
        return (LegacyPlayerInfo) obj;
    }

    default GameProfile legacyMinecraft$getProfile(){
        return null;
    }
    int getIdentifierIndex();
    void setIdentifierIndex(int i);
    boolean isVisible();
    void setVisibility(boolean visible);
    boolean isExhaustionDisabled();
    void setDisableExhaustion(boolean exhaustion);
    boolean mayFlySurvival();
    void setMayFlySurvival(boolean mayFly);

    Object2IntMap<Stat<?>> getStatsMap();
    void setStatsMap(Object2IntMap<Stat<?>> statsMap);

    static LegacyPlayerInfo decode(CommonNetwork.PlayBuf buf){
        return new LegacyPlayerInfo() {
            int index = buf.get().readVarInt();
            boolean invisible = buf.get().readBoolean();
            boolean exhaustion = buf.get().readBoolean();
            boolean mayFly = buf.get().readBoolean();

            Object2IntMap<Stat<?>> statsMap = /*? if <1.20.5 {*//*buf.get().readMap(Object2IntOpenHashMap::new, b->ClientBoundAwardStatsPacketAccessor.decodeStatCap(b,BuiltInRegistries.STAT_TYPE.byId(b.readVarInt())),FriendlyByteBuf::readVarInt)*//*?} else {*/ClientBoundAwardStatsPacketAccessor.getStatsValueCodec().decode(buf.get())/*?}*/;
            public int getIdentifierIndex() {
                return index;
            }
            public void setIdentifierIndex(int i) {
                index = i;
            }
            public boolean isVisible() {
                return invisible;
            }
            public void setVisibility(boolean visible) {
                this.invisible = visible;
            }
            public boolean isExhaustionDisabled() {
                return exhaustion;
            }
            public void setDisableExhaustion(boolean exhaustion) {
                this.exhaustion = exhaustion;
            }
            public boolean mayFlySurvival() {
                return mayFly;
            }
            public void setMayFlySurvival(boolean mayFly) {
                this.mayFly = mayFly;
            }
            @Override
            public Object2IntMap<Stat<?>> getStatsMap() {
                return statsMap;
            }
            @Override
            public void setStatsMap(Object2IntMap<Stat<?>> statsMap) {
                this.statsMap = statsMap;
            }
        };
    }

    static void encode(CommonNetwork.PlayBuf buf, LegacyPlayerInfo info){
        buf.get().writeVarInt(info.getIdentifierIndex());
        buf.get().writeBoolean(info.isVisible());
        buf.get().writeBoolean(info.isExhaustionDisabled());
        buf.get().writeBoolean(info.mayFlySurvival());
        //? if <1.20.5 {
        /*buf.get().writeMap(info.getStatsMap(), ClientBoundAwardStatsPacketAccessor::encodeStatCap, FriendlyByteBuf::writeVarInt);
        *///?} else {
        ClientBoundAwardStatsPacketAccessor.getStatsValueCodec().encode(buf.get(), info.getStatsMap());
        //?}
    }

    default void copyFrom(LegacyPlayerInfo info){
        this.setIdentifierIndex(info.getIdentifierIndex());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setStatsMap(info.getStatsMap());
    }
}
