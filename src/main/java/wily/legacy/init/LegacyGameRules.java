package wily.legacy.init;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
//? if >=1.21.11 {
import net.minecraft.world.level.gamerules.*;
//?} else {
/*import net.minecraft.world.level.GameRules;
import net.minecraft.server.MinecraftServer;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.network.PlayerInfoSync;
import java.util.function.BiConsumer;
 *///?}
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;

import java.util.function.ToIntFunction;

public class LegacyGameRules {
    //? if >=1.21.11 {
    public static final GameRule<Boolean> GLOBAL_MAP_PLAYER_ICON = registerBoolean("globalMapPlayerIcon", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> DEFAULT_SHOW_ARMOR_STANDS_ARMS = registerBoolean("defaultShowArmorStandArms", GameRuleCategory.MISC, true);
    public static final GameRule<Integer> TNT_LIMIT = registerInteger("tntLimit", GameRuleCategory.MISC, 20, 0, Integer.MAX_VALUE);
    public static final GameRule<Boolean> PLAYER_STARTING_MAP = registerBoolean("playerStartingMap", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> PLAYER_STARTING_BUNDLE = registerBoolean("playerStartingBundle", GameRuleCategory.PLAYER, false);
    public static final GameRule<Integer> DEFAULT_MAP_SIZE = registerInteger("defaultMapSize", GameRuleCategory.MISC, 1, 0, 4);
    public static final GameRule<Boolean> LEGACY_MAP_GRID = registerBoolean("legacyMapGrid", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> LEGACY_SWIMMING = registerBoolean("legacySwimming", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> LEGACY_FLIGHT = registerBoolean("legacyFlight", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> LCE_MOBCAP_LIMITS = registerBoolean("lceMobcapLimits", GameRuleCategory.MOBS, true);

    private static <T> GameRule<T> register(String location, GameRuleCategory gameRuleCategory, GameRuleType gameRuleType, ArgumentType<T> argumentType, GameRules.VisitorCaller<T> visitorCaller, Codec<T> codec, ToIntFunction<T> toIntFunction, T object, FeatureFlagSet featureFlagSet) {
        return Registry.register(BuiltInRegistries.GAME_RULE, Legacy4J.createModLocation(location), new GameRule<>(gameRuleCategory, gameRuleType, argumentType, visitorCaller, codec, toIntFunction, object, featureFlagSet));
    }

    private static GameRule<Boolean> registerBoolean(String location, GameRuleCategory gameRuleCategory, boolean defaultValue) {
        return register(location, gameRuleCategory, GameRuleType.BOOL, BoolArgumentType.bool(), GameRuleTypeVisitor::visitBoolean, Codec.BOOL, (boolean_) -> boolean_ ? 1 : 0, defaultValue, FeatureFlagSet.of());
    }

    private static GameRule<Integer> registerInteger(String location, GameRuleCategory gameRuleCategory, int defaultValue, int min, int max) {
        return register(location, gameRuleCategory, GameRuleType.INT, IntegerArgumentType.integer(min, max), GameRuleTypeVisitor::visitInteger, Codec.intRange(min, max), (i) -> i, defaultValue, FeatureFlagSet.of());
    }
    //?} else {
    /*public static final GameRules.Key<GameRules.BooleanValue> GLOBAL_MAP_PLAYER_ICON = GameRules.register("globalMapPlayerIcon", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEFAULT_SHOW_ARMOR_STANDS_ARMS = GameRules.register("defaultShowArmorStandArms", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS, booleanValue, server)));
    public static final GameRules.Key<GameRules.IntegerValue> TNT_LIMIT = GameRules.register("tntLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(20));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_MAP = GameRules.register("playerStartingMap", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.IntegerValue> DEFAULT_MAP_SIZE = GameRules.register("defaultMapSize", GameRules.Category.MISC, createInteger(1, 0, 4, ((server, integerValue) -> {})));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_BUNDLE = GameRules.register("playerStartingBundle", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_MAP_GRID = GameRules.register("legacyMapGrid", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_SWIMMING = GameRules.register("legacySwimming", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_SWIMMING, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_FLIGHT = GameRules.register("legacyFlight", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_FLIGHT, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LCE_MOBCAP_LIMITS = GameRules.register("lceMobcapLimits", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));

    public static GameRules.Type<GameRules.IntegerValue> createInteger(int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> biConsumer) {
        return GameRules.IntegerValue.create(defaultValue, min, max, FeatureFlagSet.of(), biConsumer);
    }
    *///?}

    public static boolean getSidedBooleanGamerule(Entity entity, /*? if >=1.21.11 {*/GameRule<Boolean>/*} else {*//*GameRules.Key<GameRules.BooleanValue>*//*?}*/ key) {
        if (!entity.level().isClientSide()) {
            //? if >=1.21.11 {
            return ((ServerLevel) entity.level()).getGameRules().get(key);
            //?} else {
            /*return FactoryAPIPlatform.getEntityServer(entity).getGameRules().getBoolean(key);
             *///?}
        }
        if (Legacy4JClient.hasModOnServer()) {
            return Legacy4JClient.gameRules./*? if >=1.21.11 {*/get/*?} else {*//*getBoolean*//*?}*/(key);
        }
        return (key.equals(LEGACY_SWIMMING) && LegacyOptions.forceLegacySwimming.get()) ||
                (key.equals(LEGACY_FLIGHT) && LegacyOptions.forceLegacyFlight.get());
    }

    public static void init() {
    }
}
