package wily.legacy.init;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.*;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.RegisterListing;
import wily.legacy.Legacy4J;
import wily.legacy.network.PlayerInfoSync;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class LegacyGameRules {
    private static final RegisterListing<GameRule<?>> GAME_RULE_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.GAME_RULE);
    private static Predicate<GameRule<Boolean>> clientRuleResolver = key -> false;

    // Only the TNT Limit is accurate (enabled) by default, since it won't make the gameplay worse, as no one is going to blow a lot of TNTs at the same time, mainly in Survival
    public static final RegisterListing.Holder<GameRule<Boolean>> GLOBAL_MAP_PLAYER_ICON = registerBoolean("global_map_player_icon", GameRuleCategory.PLAYER, true);
    public static final RegisterListing.Holder<GameRule<Boolean>> DEFAULT_SHOW_ARMOR_STANDS_ARMS = registerBoolean("default_show_armor_stand_arms", GameRuleCategory.MISC,true);
    public static final RegisterListing.Holder<GameRule<Integer>> TNT_LIMIT = registerInteger("tnt_limit", GameRuleCategory.MISC, 20, 0, Integer.MAX_VALUE);
    public static final RegisterListing.Holder<GameRule<Integer>> FALLING_BLOCK_LIMIT = registerInteger("falling_block_limit", GameRuleCategory.MOBS, 0, 0, Integer.MAX_VALUE);
    public static final RegisterListing.Holder<GameRule<Boolean>> PLAYER_STARTING_MAP = registerBoolean("player_starting_map", GameRuleCategory.PLAYER, true);
    public static final RegisterListing.Holder<GameRule<Integer>> DEFAULT_MAP_SIZE = registerInteger("default_map_size", GameRuleCategory.MISC, 0, 0, 4);
    public static final RegisterListing.Holder<GameRule<Boolean>> PLAYER_STARTING_BUNDLE = registerBoolean("player_starting_bundle", GameRuleCategory.PLAYER, false);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_MAP_GRID = registerBoolean("legacy_map_grid", GameRuleCategory.PLAYER, true);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_SWIMMING = registerBoolean("legacy_swimming", GameRuleCategory.PLAYER, true);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_FLIGHT = registerBoolean("legacy_flight", GameRuleCategory.PLAYER, true);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_SHIELD_CONTROLS = registerBoolean("legacy_shield_controls", GameRuleCategory.PLAYER, false);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_MOBCAP_LIMITS = registerBoolean("legacy_mobcap_limits", GameRuleCategory.MOBS, false);
    public static final RegisterListing.Holder<GameRule<Boolean>> LEGACY_OFFHAND_LIMITS = registerBoolean("legacy_offhand_limits", GameRuleCategory.PLAYER, false);

    private static <T> RegisterListing.Holder<GameRule<T>> register(String location, GameRuleCategory gameRuleCategory, GameRuleType gameRuleType, ArgumentType<T> argumentType, GameRules.VisitorCaller<T> visitorCaller, Codec<T> codec, ToIntFunction<T> toIntFunction, T object, FeatureFlagSet featureFlagSet) {
        return GAME_RULE_REGISTER.add(location, () -> new GameRule<>(gameRuleCategory, gameRuleType, argumentType, visitorCaller, codec, toIntFunction, object, featureFlagSet));
    }

    private static RegisterListing.Holder<GameRule<Boolean>> registerBoolean(String location, GameRuleCategory gameRuleCategory, boolean defaultValue) {
        return register(location, gameRuleCategory, GameRuleType.BOOL, BoolArgumentType.bool(), GameRuleTypeVisitor::visitBoolean, Codec.BOOL, (boolean_) -> boolean_ ? 1 : 0, defaultValue, FeatureFlagSet.of());
    }

    private static RegisterListing.Holder<GameRule<Integer>> registerInteger(String location, GameRuleCategory gameRuleCategory, int defaultValue, int min, int max) {
        return register(location, gameRuleCategory, GameRuleType.INT, IntegerArgumentType.integer(min, max), GameRuleTypeVisitor::visitInteger, Codec.intRange(min, max), (i) -> i, defaultValue, FeatureFlagSet.of());
    }

    public static GameRule<Boolean> getTntExplodes() {
        return GameRules.TNT_EXPLODES;
    }

    public static GameRule<Boolean> getPvp() {
        return GameRules.PVP;
    }

    //For some reason, that's hardcoded now. Better for performance, I guess
    public static <T> void onGameRuleChanged(MinecraftServer server, GameRule<T> gameRule, T object) {
        if (gameRule == DEFAULT_SHOW_ARMOR_STANDS_ARMS.get() ||
            gameRule == LEGACY_FLIGHT.get() ||
            gameRule == LEGACY_SHIELD_CONTROLS.get() ||
            gameRule == LEGACY_OFFHAND_LIMITS.get()) {
            PlayerInfoSync.All.syncGamerule(gameRule, object, server);
        }
    }

    public static boolean getSidedBooleanGamerule(Entity entity, GameRule<Boolean> key){
        if (!entity.level().isClientSide())
            return ((ServerLevel)entity.level()).getGameRules().get(key);
        return clientRuleResolver.test(key);
    }


    public static boolean getSidedBooleanGamerule(Entity entity, RegisterListing.Holder<GameRule<Boolean>> key) {
        return getSidedBooleanGamerule(entity, key.get());
    }

    public static void setClientRuleResolver(Predicate<GameRule<Boolean>> resolver) {
        clientRuleResolver = resolver == null ? key -> false : resolver;
    }

    public static void register() {
        GAME_RULE_REGISTER.register();
    }
}
