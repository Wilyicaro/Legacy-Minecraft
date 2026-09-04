package wily.legacy.entity;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.mixin.base.ClientBoundAwardStatsPacketAccessor;

public interface LegacyPlayerInfo {
    static LegacyPlayerInfo of(Object obj) {
        return (LegacyPlayerInfo) obj;
    }

    static void updateMayFlySurvival(Player player, boolean mayFlySurvival, boolean updateAbilities) {
        if (player.getAbilities().mayfly != mayFlySurvival && player.gameMode().isSurvival()) {
            player.getAbilities().mayfly = mayFlySurvival;
            if (!player.getAbilities().mayfly && player.getAbilities().flying) player.getAbilities().flying = false;
            if (updateAbilities) player.onUpdateAbilities();
        }
    }

    static void setAndUpdateMayFlySurvival(ServerPlayer player, boolean mayFlySurvival, boolean updateAbilities) {
        LegacyPlayerInfo.of(player).setMayFlySurvival(mayFlySurvival);
        updateMayFlySurvival(player, mayFlySurvival, updateAbilities);
    }

    static void initializeTrustPermissions(ServerPlayer player) {
        initializeTrustPermissions(LegacyPlayerInfo.of(player), PlayerTrustPolicy.isTrustEnabled(player.level().getServer()), PlayerTrustPolicy.isFullAuthority(player));
    }

    static void initializeTrustPermissions(LegacyPlayerInfo info, boolean trustPlayers, boolean fullTrustAuthority) {
        info.setFullTrustAuthority(fullTrustAuthority);
        if (info.isTrustPermissionsInitialized()) return;
        info.setTrustPermissions(trustPlayers ? PlayerTrustPermissions.TRUSTED : PlayerTrustPermissions.RESTRICTED);
        info.setTrustPermissionsInitialized(true);
    }

    static LegacyPlayerInfo decode(CommonNetwork.PlayBuf buf) {
        int identifierIndex = buf.get().readVarInt();
        boolean visible = buf.get().readBoolean();
        boolean exhaustionDisabled = buf.get().readBoolean();
        boolean mayFlySurvival = buf.get().readBoolean();
        PlayerTrustPermissions trustPermissions = PlayerTrustPermissions.decode(buf);
        PlayerHostPrivileges hostPrivileges = PlayerHostPrivileges.decode(buf);
        boolean moderator = buf.get().readBoolean();
        boolean trustPermissionsInitialized = buf.get().readBoolean();
        boolean fullTrustAuthority = buf.get().readBoolean();
        return new Instance(identifierIndex, visible, exhaustionDisabled, mayFlySurvival, trustPermissions, hostPrivileges, moderator, trustPermissionsInitialized, fullTrustAuthority,/*? if <1.20.5 {*//*buf.get().readMap(Object2IntOpenHashMap::new, b->ClientBoundAwardStatsPacketAccessor.decodeStatCap(b,BuiltInRegistries.STAT_TYPE.byId(b.readVarInt())),FriendlyByteBuf::readVarInt)*//*?} else {*/ClientBoundAwardStatsPacketAccessor.getStatsValueCodec().decode(buf.get())/*?}*/);
    }

    static void encode(CommonNetwork.PlayBuf buf, LegacyPlayerInfo info) {
        buf.get().writeVarInt(info.getIdentifierIndex());
        buf.get().writeBoolean(info.isVisible());
        buf.get().writeBoolean(info.isExhaustionDisabled());
        buf.get().writeBoolean(info.mayFlySurvival());
        info.getTrustPermissions().encode(buf);
        info.getHostPrivileges().encode(buf);
        buf.get().writeBoolean(info.isModerator());
        buf.get().writeBoolean(info.isTrustPermissionsInitialized());
        buf.get().writeBoolean(info.hasFullTrustAuthority());
        //? if <1.20.5 {
        /*buf.get().writeMap(info.getStatsMap(), ClientBoundAwardStatsPacketAccessor::encodeStatCap, FriendlyByteBuf::writeVarInt);
         *///?} else {
        ClientBoundAwardStatsPacketAccessor.getStatsValueCodec().encode(buf.get(), info.getStatsMap());
        //?}
    }

    default GameProfile legacyMinecraft$getProfile() {
        return null;
    }

    Data legacyMinecraft$getPlayerInfoData();

    default int getIdentifierIndex() {
        return legacyMinecraft$getPlayerInfoData().identifierIndex;
    }

    default void setIdentifierIndex(int identifierIndex) {
        legacyMinecraft$getPlayerInfoData().identifierIndex = identifierIndex;
    }

    default boolean isVisible() {
        return legacyMinecraft$getPlayerInfoData().visible;
    }

    default void setVisibility(boolean visible) {
        legacyMinecraft$getPlayerInfoData().visible = visible;
    }

    default boolean isExhaustionDisabled() {
        return legacyMinecraft$getPlayerInfoData().exhaustionDisabled;
    }

    default void setDisableExhaustion(boolean exhaustionDisabled) {
        legacyMinecraft$getPlayerInfoData().exhaustionDisabled = exhaustionDisabled;
    }

    default boolean mayFlySurvival() {
        return legacyMinecraft$getPlayerInfoData().mayFlySurvival;
    }

    default void setMayFlySurvival(boolean mayFlySurvival) {
        legacyMinecraft$getPlayerInfoData().mayFlySurvival = mayFlySurvival;
    }

    default PlayerTrustPermissions getTrustPermissions() {
        return legacyMinecraft$getPlayerInfoData().trustPermissions;
    }

    default void setTrustPermissions(PlayerTrustPermissions trustPermissions) {
        legacyMinecraft$getPlayerInfoData().trustPermissions = trustPermissions;
    }

    default PlayerHostPrivileges getHostPrivileges() {
        return legacyMinecraft$getPlayerInfoData().hostPrivileges;
    }

    default void setHostPrivileges(PlayerHostPrivileges hostPrivileges) {
        legacyMinecraft$getPlayerInfoData().hostPrivileges = hostPrivileges;
    }

    default boolean isModerator() {
        return legacyMinecraft$getPlayerInfoData().moderator;
    }

    default void setModerator(boolean moderator) {
        legacyMinecraft$getPlayerInfoData().moderator = moderator;
    }

    default boolean isTrustPermissionsInitialized() {
        return legacyMinecraft$getPlayerInfoData().trustPermissionsInitialized;
    }

    default void setTrustPermissionsInitialized(boolean initialized) {
        legacyMinecraft$getPlayerInfoData().trustPermissionsInitialized = initialized;
    }

    default boolean hasFullTrustAuthority() {
        return legacyMinecraft$getPlayerInfoData().fullTrustAuthority;
    }

    default void setFullTrustAuthority(boolean fullTrustAuthority) {
        legacyMinecraft$getPlayerInfoData().fullTrustAuthority = fullTrustAuthority;
    }

    default Object2IntMap<Stat<?>> getStatsMap() {
        return legacyMinecraft$getPlayerInfoData().statsMap;
    }

    default void setStatsMap(Object2IntMap<Stat<?>> statsMap) {
        legacyMinecraft$getPlayerInfoData().statsMap = statsMap;
    }

    default void copyFrom(LegacyPlayerInfo info) {
        this.setIdentifierIndex(info.getIdentifierIndex());
        this.setVisibility(info.isVisible());
        this.setDisableExhaustion(info.isExhaustionDisabled());
        this.setMayFlySurvival(info.mayFlySurvival());
        this.setTrustPermissions(info.getTrustPermissions());
        this.setHostPrivileges(info.getHostPrivileges());
        this.setModerator(info.isModerator());
        this.setTrustPermissionsInitialized(info.isTrustPermissionsInitialized());
        this.setFullTrustAuthority(info.hasFullTrustAuthority());
        this.setStatsMap(info.getStatsMap());
    }

    final class Data {
        private int identifierIndex = -1;
        private boolean visible = true;
        private boolean exhaustionDisabled;
        private boolean mayFlySurvival;
        private PlayerTrustPermissions trustPermissions = PlayerTrustPermissions.RESTRICTED;
        private PlayerHostPrivileges hostPrivileges = PlayerHostPrivileges.NONE;
        private boolean moderator;
        private boolean trustPermissionsInitialized;
        private boolean fullTrustAuthority;
        private Object2IntMap<Stat<?>> statsMap;

        public Data() {
        }

        public Data(Object2IntMap<Stat<?>> statsMap) {
            this.statsMap = statsMap;
        }

        public Data(int identifierIndex, boolean visible, boolean exhaustionDisabled, boolean mayFlySurvival, PlayerTrustPermissions trustPermissions, PlayerHostPrivileges hostPrivileges, boolean moderator, boolean trustPermissionsInitialized, boolean fullTrustAuthority, Object2IntMap<Stat<?>> statsMap) {
            this.identifierIndex = identifierIndex;
            this.visible = visible;
            this.exhaustionDisabled = exhaustionDisabled;
            this.mayFlySurvival = mayFlySurvival;
            this.trustPermissions = trustPermissions;
            this.hostPrivileges = hostPrivileges;
            this.moderator = moderator;
            this.trustPermissionsInitialized = trustPermissionsInitialized;
            this.fullTrustAuthority = fullTrustAuthority;
            this.statsMap = statsMap;
        }
    }

    class Instance implements LegacyPlayerInfo {
        private final Data data;

        public Instance(int identifierIndex, boolean visible, boolean exhaustionDisabled, boolean mayFlySurvival, PlayerTrustPermissions trustPermissions, PlayerHostPrivileges hostPrivileges, boolean moderator, boolean trustPermissionsInitialized, boolean fullTrustAuthority, Object2IntMap<Stat<?>> statsMap) {
            data = new Data(identifierIndex, visible, exhaustionDisabled, mayFlySurvival, trustPermissions, hostPrivileges, moderator, trustPermissionsInitialized, fullTrustAuthority, statsMap);
        }

        @Override
        public Data legacyMinecraft$getPlayerInfoData() {
            return data;
        }
    }
}
