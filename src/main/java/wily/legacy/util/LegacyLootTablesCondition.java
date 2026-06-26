package wily.legacy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
//? if <1.20.4 {
/*import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
*///?}
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyRegistries;

public enum LegacyLootTablesCondition implements LootItemCondition {
    INSTANCE;

    //? if <1.20.4 {
    /*public static final Serializer SERIALIZER = new Serializer();
    *///?} else if <1.21.1 {
    /*public static final Codec<LegacyLootTablesCondition> CODEC = Codec.unit(INSTANCE);
    *///?} else {
    public static final MapCodec<LegacyLootTablesCondition> CODEC = MapCodec.unit(INSTANCE);
    //?}

    @Override
    public boolean test(LootContext lootContext) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyLootTables);
    }

    @Override
    public LootItemConditionType getType() {
        return LegacyRegistries.LEGACY_LOOT_TABLES.get();
    }

    //? if <1.20.4 {
    /*public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LegacyLootTablesCondition> {
        @Override
        public void serialize(JsonObject jsonObject, LegacyLootTablesCondition condition, JsonSerializationContext context) {
        }

        @Override
        public LegacyLootTablesCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            return INSTANCE;
        }
    }
    *///?}
}
