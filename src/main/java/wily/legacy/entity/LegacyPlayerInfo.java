package wily.legacy.entity;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.mixin.base.ClientBoundAwardStatsPacketAccessor;

public interface LegacyPlayerInfo {
    static LegacyPlayerInfo of(Object obj) {
        return (LegacyPlayerInfo) obj;
    }

    static void updateMayFlySurvival(ServerPlayer player, boolean mayFlySurvival, boolean updateAbilities) {
        LegacyPlayerInfo.of(player).setMayFlySurvival(mayFlySurvival);
        if (player.getAbilities().mayfly != mayFlySurvival && player.gameMode.isSurvival()) {
            player.getAbilities().mayfly = mayFlySurvival;
            if (!player.getAbilities().mayfly && player.getAbilities().flying) player.getAbilities().flying = false;
            if (updateAbilities) player.onUpdateAbilities();
        }
    }

    static LegacyPlayerInfo decode(CommonNetwork.PlayBuf buf) {
        return new Instance(buf.get().readVarInt(), buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean(),/*? if <1.20.5 {*//*buf.get().readMap(Object2IntOpenHashMap::new, b->ClientBoundAwardStatsPacketAccessor.decodeStatCap(b,BuiltInRegistries.STAT_TYPE.byId(b.readVarInt())),FriendlyByteBuf::readVarInt)*//*?} else {*/ClientBoundAwardStatsPacketAccessor.getStatsValueCodec().decode(buf.get())/*?}*/);
    }

    static void encode(CommonNetwork.PlayBuf buf, LegacyPlayerInfo info) {
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

    default GameProfile legacyMinecraft$getProfile() {
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

    default void copyFrom(LegacyPlayerInfo info) {
        this.setIdentifierIndex(info.getIdentifierIndex());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setStatsMap(info.getStatsMap());
    }

    class Instance implements LegacyPlayerInfo {
        int index = -1;
        boolean visibility = true;
        boolean disableExhaustion;
        boolean mayFlySurvival = false;
        Object2IntMap<Stat<?>> statsMap;

        public Instance(int index, boolean invisible, boolean disableExhaustion, boolean mayFlySurvival, Object2IntMap<Stat<?>> object2IntMap) {
            setIdentifierIndex(index);
            setVisibility(invisible);
            setDisableExhaustion(disableExhaustion);
            setMayFlySurvival(mayFlySurvival);
            setStatsMap(object2IntMap);
        }

        @Override
        public int getIdentifierIndex() {
            return index;
        }

        @Override
        public void setIdentifierIndex(int i) {
            index = i;
        }

        @Override
        public boolean isVisible() {
            return visibility;
        }

        @Override
        public void setVisibility(boolean visible) {
            this.visibility = visible;
        }

        @Override
        public boolean isExhaustionDisabled() {
            return disableExhaustion;
        }

        @Override
        public void setDisableExhaustion(boolean exhaustion) {
            this.disableExhaustion = exhaustion;
        }

        @Override
        public boolean mayFlySurvival() {
            return mayFlySurvival;
        }

        @Override
        public void setMayFlySurvival(boolean mayFly) {
            this.mayFlySurvival = mayFly;
        }

        @Override
        public Object2IntMap<Stat<?>> getStatsMap() {
            return statsMap;
        }

        @Override
        public void setStatsMap(Object2IntMap<Stat<?>> statsMap) {
            this.statsMap = statsMap;
        }
    }
}
