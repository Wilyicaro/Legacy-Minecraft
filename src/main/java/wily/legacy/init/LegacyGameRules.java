package wily.legacy.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level./*? if <1.21.11 {*//*GameRules*//*?} else {*/gamerules.*/*?}*/;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.PlayerInfoSync;

//? >=1.21.11 {
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//?}
import java.util.function.BiConsumer;

//? <1.21.11 {
/*
public class LegacyGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> GLOBAL_MAP_PLAYER_ICON = GameRules.register("globalMapPlayerIcon", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEFAULT_SHOW_ARMOR_STANDS_ARMS = GameRules.register("defaultShowArmorStandArms", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, booleanValue) ->  PlayerInfoSync.All.syncGamerule(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS, booleanValue, server)));
    public static final GameRules.Key<GameRules.IntegerValue> TNT_LIMIT = GameRules.register("tntLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(20));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_MAP = GameRules.register("playerStartingMap", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.IntegerValue> DEFAULT_MAP_SIZE = GameRules.register("defaultMapSize", GameRules.Category.MISC, createInteger(3, 0, 4, ((server, integerValue) -> {})));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_STARTING_BUNDLE = GameRules.register("playerStartingBundle", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_MAP_GRID = GameRules.register("legacyMapGrid", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_SWIMMING = GameRules.register("legacySwimming", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_SWIMMING, booleanValue, server)));
    public static final GameRules.Key<GameRules.BooleanValue> LEGACY_FLIGHT = GameRules.register("legacyFlight", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (server, booleanValue) -> PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_FLIGHT, booleanValue, server)));

    public static GameRules.Key<GameRules.BooleanValue> getTntExplodes() {
        return GameRules.RULE_TNT_EXPLODES;
    }

    public static GameRules.Key<GameRules.BooleanValue> getPvp() {
        return GameRules.RULE_PVP;
    }

    public static boolean getSidedBooleanGamerule(Entity entity, GameRules.Key<GameRules.BooleanValue> key){
        return entity.level().isClientSide() && Legacy4JClient.hasModOnServer() && Legacy4JClient.gameRules.getBoolean(key) || !entity.level().isClientSide() && FactoryAPIPlatform.getEntityServer(entity).getGameRules().getBoolean(key);
    }

    public static GameRules.Type<GameRules.IntegerValue> createInteger(int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> biConsumer){
        return GameRules.IntegerValue.create(defaultValue, min, max, FeatureFlagSet.of(), biConsumer);
    }

    public static void init(){
    }
}
*///?} else {
public class LegacyGameRules {

    public static final GameRule<Boolean> GLOBAL_MAP_PLAYER_ICON = registerBoolean("global_map_player_icon", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> DEFAULT_SHOW_ARMOR_STANDS_ARMS = registerBoolean("default_show_armor_stand_arms", GameRuleCategory.MISC, true);
    public static final GameRule<Integer> TNT_LIMIT = registerInteger("tnt_limit", GameRuleCategory.MISC, 20, 0, Integer.MAX_VALUE);
    public static final GameRule<Boolean> PLAYER_STARTING_MAP = registerBoolean("player_starting_map", GameRuleCategory.PLAYER, true);
    public static final GameRule<Integer> DEFAULT_MAP_SIZE = registerInteger("default_map_size", GameRuleCategory.MISC, 3, 0, 4);
    public static final GameRule<Boolean> PLAYER_STARTING_BUNDLE = registerBoolean("player_starting_bundle", GameRuleCategory.PLAYER, false);
    public static final GameRule<Boolean> LEGACY_MAP_GRID = registerBoolean("legacy_map_grid", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> LEGACY_SWIMMING = registerBoolean("legacy_swimming", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> LEGACY_FLIGHT = registerBoolean("legacy_flight", GameRuleCategory.PLAYER, true);

    public static GameRule<Boolean> getTntExplodes() {
        return GameRules.TNT_EXPLODES;
    }

    public static GameRule<Boolean> getPvp() {
        return GameRules.PVP;
    }

    public static boolean getSidedBooleanGamerule(Entity entity, GameRule<Boolean> gameRule) {
        if (entity.level().isClientSide()) {
            return Legacy4JClient.hasModOnServer() && Legacy4JClient.gameRules.get(gameRule);
        } else {
            return FactoryAPIPlatform.getEntityServer(entity).getWorldData().getGameRules().get(gameRule);
        }
    }

    private static GameRule<Boolean> registerBoolean(String id, GameRuleCategory category, boolean defaultValue) {
        return Registry.register(
            BuiltInRegistries.GAME_RULE,
            id,
            new GameRule<>(
                category,
                GameRuleType.BOOL,
                BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean,
                Codec.BOOL,
                (b) -> b ? 1 : 0,
                defaultValue,
                FeatureFlagSet.of()
            )
        );
    }

    private static GameRule<Integer> registerInteger(String id, GameRuleCategory category, int defaultValue, int min, int max) {
        return Registry.register(
            BuiltInRegistries.GAME_RULE,
            id,
            new GameRule<>(
                category,
                GameRuleType.INT,
                IntegerArgumentType.integer(min, max),
                GameRuleTypeVisitor::visitInteger,
                Codec.intRange(min, max),
                (i) -> i,
                defaultValue,
                FeatureFlagSet.of()
            )
        );
    }

    public static void init(){
    }
}
//?}
