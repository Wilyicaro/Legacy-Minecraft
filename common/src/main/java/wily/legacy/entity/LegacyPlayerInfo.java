package wily.legacy.entity;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import wily.legacy.mixin.ServerPlayerMixin;

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

    static LegacyPlayerInfo fromNetwork(FriendlyByteBuf buf){
        return new LegacyPlayerInfo() {
            int index = buf.readVarInt();
            boolean invisible = buf.readBoolean();
            boolean exhaustion = buf.readBoolean();
            boolean mayFly = buf.readBoolean();

            Object2IntMap<Stat<?>> statsMap = ClientboundAwardStatsPacket.STAT_VALUES_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
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
    default void toNetwork(RegistryFriendlyByteBuf buf){
        buf.writeVarInt(getIdentifierIndex());
        buf.writeBoolean(isVisible());
        buf.writeBoolean(isExhaustionDisabled());
        buf.writeBoolean(mayFlySurvival());
        ClientboundAwardStatsPacket.STAT_VALUES_STREAM_CODEC.encode(buf,getStatsMap());
    }
    default void copyFrom(LegacyPlayerInfo info){
        this.setIdentifierIndex(info.getIdentifierIndex());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setStatsMap(info.getStatsMap());
    }
}
