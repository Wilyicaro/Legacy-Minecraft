package wily.legacy.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.network.PlayerInfoSync;

import java.util.function.BiConsumer;

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
        if (!entity.level().isClientSide()) {
            return FactoryAPIPlatform.getEntityServer(entity).getGameRules().getBoolean(key);
        }
        if (Legacy4JClient.hasModOnServer()) {
            return Legacy4JClient.gameRules.getBoolean(key);
        }
        if (key.equals(LEGACY_SWIMMING) && LegacyOptions.forceLegacySwimming.get()) {
            return true;
        }
        if (key.equals(LEGACY_FLIGHT) && LegacyOptions.forceLegacyFlight.get()) {
            return true;
        }
        return false;
    }

    public static GameRules.Type<GameRules.IntegerValue> createInteger(int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> biConsumer){
        return GameRules.IntegerValue.create(defaultValue, min, max, FeatureFlagSet.of(), biConsumer);
    }

    public static void init(){
    }
}
