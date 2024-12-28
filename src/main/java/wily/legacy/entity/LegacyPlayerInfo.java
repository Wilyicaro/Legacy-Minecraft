package wily.legacy.entity;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

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

    static LegacyPlayerInfo decode(FriendlyByteBuf buf){
        return new LegacyPlayerInfo() {
            int index = buf.readVarInt();
            boolean invisible = buf.readBoolean();
            boolean exhaustion = buf.readBoolean();
            boolean mayFly = buf.readBoolean();

            Object2IntMap<Stat<?>> statsMap = buf.readMap(Object2IntOpenHashMap::new,b->decodeStatCap(b,BuiltInRegistries.STAT_TYPE.asHolderIdMap().byId(b.readVarInt()).value()),FriendlyByteBuf::readVarInt);
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

    static void encode(FriendlyByteBuf buf, LegacyPlayerInfo info){
        buf.writeVarInt(info.getIdentifierIndex());
        buf.writeBoolean(info.isVisible());
        buf.writeBoolean(info.isExhaustionDisabled());
        buf.writeBoolean(info.mayFlySurvival());
        buf.writeMap(info.getStatsMap(), LegacyPlayerInfo::encodeStat, FriendlyByteBuf::writeVarInt);
    }

    static <T> void encodeStat(FriendlyByteBuf b, Stat<T> s){
        b.writeVarInt(BuiltInRegistries.STAT_TYPE.getId(s.getType()));
        b.writeVarInt(s.getType().getRegistry().getId(s.getValue()));
    }

    static <T> Stat<T> decodeStatCap(FriendlyByteBuf b, StatType<T> statType) {
        return statType.get(statType.getRegistry().asHolderIdMap().byId(b.readVarInt()).value());
    }
    default void copyFrom(LegacyPlayerInfo info){
        this.setIdentifierIndex(info.getIdentifierIndex());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setStatsMap(info.getStatsMap());
    }
}
