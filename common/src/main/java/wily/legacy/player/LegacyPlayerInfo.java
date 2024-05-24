package wily.legacy.player;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

public interface LegacyPlayerInfo {
    default GameProfile legacyMinecraft$getProfile(){
        return null;
    }
    int getPosition();
    void setPosition(int i);
    boolean isVisible();
    void setVisibility(boolean visible);
    boolean isExhaustionDisabled();
    void setDisableExhaustion(boolean exhaustion);
    boolean mayFlySurvival();
    void setMayFlySurvival(boolean mayFly);

    Object2IntMap<Stat<?>> getStatsMap();
    void setStatsMap(Object2IntMap<Stat<?>> statsMap);

    static LegacyPlayerInfo fromNetwork(FriendlyByteBuf buf){
        return new LegacyPlayerInfo() {
            int pos = buf.readVarInt();
            boolean invisible = buf.readBoolean();
            boolean exhaustion = buf.readBoolean();
            boolean mayFly = buf.readBoolean();

            Object2IntMap<Stat<?>> statsMap = buf.readMap(Object2IntOpenHashMap::new, b -> ClientboundAwardStatsPacket.readStatCap(b, b.readById(BuiltInRegistries.STAT_TYPE)), FriendlyByteBuf::readVarInt);
            public int getPosition() {
                return pos;
            }
            public void setPosition(int i) {
                pos = i;
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
    default void toNetwork(FriendlyByteBuf buf){
        buf.writeVarInt(getPosition());
        buf.writeBoolean(isVisible());
        buf.writeBoolean(isExhaustionDisabled());
        buf.writeBoolean(mayFlySurvival());
        buf.writeMap(getStatsMap(),ClientboundAwardStatsPacket::writeStatCap, FriendlyByteBuf::writeVarInt);
    }
    default void copyFrom(LegacyPlayerInfo info){
        this.setPosition(info.getPosition());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setStatsMap(info.getStatsMap());
    }
}
