package wily.legacy.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import wily.legacy.Legacy4J;

public class LegacyTags {
    public static final TagKey<Block> PUSHABLE_BLOCK = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("pushable"));
    public static final TagKey<Block> WATER_CAULDRONS = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("water_cauldrons"));
    public static final TagKey<EntityType<?>> OLD_SPLASH_SOUND = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.createModLocation("old_splash_sound"));
}
