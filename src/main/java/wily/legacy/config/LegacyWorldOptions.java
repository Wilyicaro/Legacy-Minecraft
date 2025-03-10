package wily.legacy.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyTipBuilder;

import java.util.*;
import java.util.function.Function;

public class LegacyWorldOptions {
    public static final FactoryConfig.StorageHandler WORLD_STORAGE = new FactoryConfig.StorageHandler();
    public static final FactoryConfig<Map<String, LegacyTipBuilder>> customTips = WORLD_STORAGE.register(FactoryConfig.create("customTips", null, ()-> LegacyTipBuilder.MAP_CODEC, new HashMap<>(), v-> {}, WORLD_STORAGE));
    public static final FactoryConfig<List<InitialItem>> initialItems = WORLD_STORAGE.register(FactoryConfig.create("initialItems", null, ()-> InitialItem.LIST_CODEC, List.of(new InitialItem(Items.MAP.getDefaultInstance(), LegacyGameRules.PLAYER_STARTING_MAP), new InitialItem(Items.BUNDLE.getDefaultInstance(), LegacyGameRules.PLAYER_STARTING_BUNDLE)), v-> {}, WORLD_STORAGE));
    public static final FactoryConfig<List<UsedEndPortalPos>> usedEndPortalPositions = WORLD_STORAGE.register(FactoryConfig.create("usedEndPortalPositions", null, ()-> UsedEndPortalPos.LIST_CODEC, new ArrayList<>(), v-> {}, WORLD_STORAGE));

    public record InitialItem(ItemStack item, Optional<GameRules.Key<GameRules.BooleanValue>> dependentGamerule){
        public static final Codec<GameRules.Key<GameRules.BooleanValue>> BOOLEAN_GAMERULE_CODEC = Codec.STRING.xmap(InitialItem::getGameruleFromId, GameRules.Key::getId);
        public static final Codec<InitialItem> CODEC = RecordCodecBuilder.create(i-> i.group(DynamicUtil.ITEM_CODEC.fieldOf("item").forGetter(InitialItem::item), BOOLEAN_GAMERULE_CODEC.optionalFieldOf("gamerule").forGetter(InitialItem::dependentGamerule)).apply(i, InitialItem::new));
        public static final Codec<List<InitialItem>> LIST_CODEC = CODEC.listOf();

        public InitialItem(ItemStack item, GameRules.Key<GameRules.BooleanValue> gamerule){
            this(item, Optional.of(gamerule));
        }

        public InitialItem(ItemStack item){
            this(item, Optional.empty());
        }

        public boolean isEnabled(MinecraftServer server){
            return dependentGamerule.isEmpty() || server.getGameRules().getBoolean(dependentGamerule.get());
        }

        public static GameRules.Key<GameRules.BooleanValue> getGameruleFromId(String id){
            Bearer<GameRules.Key<GameRules.BooleanValue>> keyBearer = Bearer.of(null);
            GameRules gameRules = FactoryAPI.currentServer.getGameRules();
            gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    if (gameRules.getRule(key) instanceof GameRules.BooleanValue && key.getId().equals(id)) keyBearer.set((GameRules.Key<GameRules.BooleanValue>) key);
                }
            });
            return keyBearer.get();
        }
    }

    public record UsedEndPortalPos(BlockPos pos, UUID player, String identifier){
        public static final Codec<UsedEndPortalPos> CODEC = RecordCodecBuilder.create(i-> i.group(BlockPos.CODEC.fieldOf("pos").forGetter(UsedEndPortalPos::pos), Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("player").forGetter(UsedEndPortalPos::player)).apply(i, UsedEndPortalPos::new));
        public static final Codec<List<UsedEndPortalPos>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());

        public UsedEndPortalPos(BlockPos pos, UUID player){
            this(pos, player, "used_end_portal_pos:"+pos.toString());
        }

        public boolean inRange(BlockPos otherPos){
            return Math.abs(pos.getX() - otherPos.getX()) <= 2 && Math.abs(pos.getZ() - otherPos.getZ()) <= 2;
        }

        public boolean isValid(MinecraftServer server){
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
            return serverPlayer != null && serverPlayer.level().dimension() == Level.END;
        }
    }
}
