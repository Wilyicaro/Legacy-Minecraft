package wily.legacy.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

public enum LegacyLootTablesCondition implements LootItemCondition {
    INSTANCE;

    public static final MapCodec<LegacyLootTablesCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(LootContext lootContext) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyLootTables);
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }
}
