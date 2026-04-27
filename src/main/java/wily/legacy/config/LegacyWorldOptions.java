package wily.legacy.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyTipBuilder;

import java.util.*;
import java.util.function.Function;

public class LegacyWorldOptions {
    public static final FactoryConfig.StorageHandler WORLD_STORAGE = new FactoryConfig.StorageHandler();
    public static final FactoryConfig<Map<String, LegacyTipBuilder>> customTips = WORLD_STORAGE.register(FactoryConfig.create("customTips", null, () -> LegacyTipBuilder.MAP_CODEC, new HashMap<>(), v -> {}, WORLD_STORAGE));
    public static final FactoryConfig<List<InitialItem>> initialItems = WORLD_STORAGE.register(FactoryConfig.create("initialItems", null, () -> InitialItem.LIST_CODEC, List.of(new InitialItem(createStartingMap(), LegacyGameRules.PLAYER_STARTING_MAP.get()), new InitialItem(Items.BUNDLE.getDefaultInstance(), LegacyGameRules.PLAYER_STARTING_BUNDLE.get())), v -> {}, WORLD_STORAGE));
    public static final FactoryConfig<List<UsedEndPortalPos>> usedEndPortalPositions = WORLD_STORAGE.register(FactoryConfig.create("usedEndPortalPositions", null, () -> UsedEndPortalPos.LIST_CODEC, new ArrayList<>(), v -> {}, WORLD_STORAGE));

    
    private static ItemStack createStartingMap() {
        ItemStack starterMap = Items.MAP.getDefaultInstance();
        CompoundTag customData = new CompoundTag();
        customData.putByte("map_scale", (byte) 3);
        //? if >=1.20.5 {
        starterMap.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        //?} else {
        /*starterMap.getOrCreateTag().putByte(MapItem.MAP_SCALE_TAG, (byte) 3);
        *///?}
        return starterMap;
    }

    public record InitialItem(ItemStack item, Optional<GameRule<Boolean>> dependentGamerule) {
        public static final Codec<GameRule<Boolean>> BOOLEAN_GAMERULE_CODEC = BuiltInRegistries.GAME_RULE.byNameCodec().validate(rule -> rule.gameRuleType() == GameRuleType.BOOL ? DataResult.success(rule) : DataResult.error(() -> "Initial Item gamerule should be a boolean!")).xmap(gameRule -> (GameRule<Boolean>) gameRule, Function.identity());
        public static final Codec<InitialItem> CODEC = RecordCodecBuilder.create(i -> i.group(DynamicUtil.ITEM_CODEC.fieldOf("item").forGetter(InitialItem::item), BOOLEAN_GAMERULE_CODEC.optionalFieldOf("gamerule").forGetter(InitialItem::dependentGamerule)).apply(i, InitialItem::new));
        public static final Codec<List<InitialItem>> LIST_CODEC = CODEC.listOf();

        public InitialItem(ItemStack item, GameRule<Boolean> gamerule) {
            this(item, Optional.of(gamerule));
        }

        public InitialItem(ItemStack item) {
            this(item, Optional.empty());
        }

        public static GameRule<Boolean> getGameruleFromId(String id) {
            Bearer<GameRule<Boolean>> keyBearer = Bearer.of(null);
            GameRules gameRules = FactoryAPI.currentServer.overworld().getGameRules();

            gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
                @Override
                public void visitBoolean(GameRule<Boolean> gameRule) {
                    keyBearer.set(gameRule);
                }
            });
            return keyBearer.get();
        }

        public boolean isEnabled(MinecraftServer server) {
            return dependentGamerule.isEmpty() || server.overworld().getGameRules().get(dependentGamerule.get());
        }
    }

    public record UsedEndPortalPos(BlockPos pos, UUID player, String identifier) {
        public static final Codec<UsedEndPortalPos> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("pos").forGetter(UsedEndPortalPos::pos), Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("player").forGetter(UsedEndPortalPos::player)).apply(i, UsedEndPortalPos::new));
        public static final Codec<List<UsedEndPortalPos>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());

        public UsedEndPortalPos(BlockPos pos, UUID player) {
            this(pos, player, "used_end_portal_pos:" + pos.toString());
        }

        public boolean inRange(BlockPos otherPos) {
            return Math.abs(pos.getX() - otherPos.getX()) <= 2 && Math.abs(pos.getZ() - otherPos.getZ()) <= 2;
        }

        public boolean isValid(MinecraftServer server) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
            return serverPlayer != null && serverPlayer.level().dimension() == Level.END;
        }
    }

    public record NamedArea(BlockPos firstPos, BlockPos secondPos, boolean checkHeight, boolean isBarrier,
                            List<UUID> players) {

    }
}
