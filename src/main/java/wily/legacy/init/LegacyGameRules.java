package wily.legacy.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.network.PlayerInfoSync;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class LegacyGameRules {
    private static Predicate<GameRules.Key<GameRules.BooleanValue>> clientRuleResolver = key -> false;

    // Only the TNT Limit is accurate (enabled) by default, since it won't make the gameplay worse, as no one is going to blow a lot of TNTs at the same time, mainly in Survival
    public static final GameRules.Key<GameRules.BooleanValue> GLOBAL_MAP_PLAYER_ICON = GameRules.register("globalMapPlayerIcon", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEFAULT_SHOW_ARMOR_STANDS_ARMS = GameRules.register("defaultShowArmorStandArms", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, booleanValue) ->  PlayerInfoSync.All.syncGamerule(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS, booleanValue, server)));
    public static final GameRules.Key<GameRules.IntegerValue> TNT_LIMIT = GameRules.register("tntLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(20));
    public static final GameRules.Key<GameRules.IntegerValue> FALLING_BLOCK_LIMIT = GameRules.register("fallingBlockLimit", GameRules.Category.MOBS, GameRules.IntegerValue.create(0));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_MAP = GameRules.register("playerStartingMap", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.IntegerValue> DEFAULT_MAP_SIZE = GameRules.register("defaultMapSize", GameRules.Category.MISC, createInteger(0, 0, 4, ((server, integerValue) -> {})));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_BUNDLE = GameRules.register("playerStartingBundle", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_MAP_GRID = GameRules.register("legacyMapGrid", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_SWIMMING = GameRules.register("legacySwimming", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_SWIMMING, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_FLIGHT = GameRules.register("legacyFlight", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_FLIGHT, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_SHIELD_CONTROLS = GameRules.register("legacyShieldControls", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_SHIELD_CONTROLS, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_MOBCAP_LIMITS = GameRules.register("legacyMobcapLimits", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_OFFHAND_LIMITS = GameRules.register("legacyOffhandLimits", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_OFFHAND_LIMITS, booleanValue, server)));

    public static GameRules.Key<GameRules.BooleanValue> getTntExplodes() {
        return GameRules.RULE_TNT_EXPLODES;
    }

    public static GameRules.Key<GameRules.BooleanValue> getPvp() {
        return GameRules.RULE_PVP;
    }

    public static boolean getSidedBooleanGamerule(Entity entity, GameRules.Key<GameRules.BooleanValue> key){
        if (!entity.level().isClientSide())
            return FactoryAPIPlatform.getEntityServer(entity).getGameRules().getBoolean(key);
        return clientRuleResolver.test(key);
    }

    public static GameRules.Type<GameRules.IntegerValue> createInteger(int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> biConsumer){
        return GameRules.IntegerValue.create(defaultValue, min, max, FeatureFlagSet.of(), biConsumer);
    }

    public static void setClientRuleResolver(Predicate<GameRules.Key<GameRules.BooleanValue>> resolver) {
        clientRuleResolver = resolver == null ? key -> false : resolver;
    }

    public static void init(){
    }
}
