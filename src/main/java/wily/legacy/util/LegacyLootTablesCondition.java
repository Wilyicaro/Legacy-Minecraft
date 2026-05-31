package wily.legacy.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyRegistries;

public enum LegacyLootTablesCondition implements LootItemCondition {
    INSTANCE;

    public static final MapCodec<LegacyLootTablesCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(LootContext lootContext) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyLootTables);
    }

    @Override
    public LootItemConditionType getType() {
        return LegacyRegistries.LEGACY_LOOT_TABLES.get();
    }
}
