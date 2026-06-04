package wily.legacy.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import wily.legacy.Legacy4J;

public class LegacyTags {
    public static final TagKey<Item> FULL_SIZE_SLOT_ITEMS = TagKey.create(Registries.ITEM, Legacy4J.createModLocation("full_size_slot_items"));
    public static final TagKey<Item> PADDED_SLOT_ITEMS = TagKey.create(Registries.ITEM, Legacy4J.createModLocation("padded_slot_items"));
    public static final TagKey<Block> PUSHABLE_BLOCK = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("pushable"));
    public static final TagKey<Block> WATER_CAULDRONS = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("water_cauldrons"));
    public static final TagKey<Block> SLOW_CHUNK_FEATURES = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("slow_chunk_features"));
    public static final TagKey<EntityType<?>> BABY_ZOMBIE_JOCKEY_MOUNTS = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.createModLocation("baby_zombie_jockey_mounts"));
    public static final TagKey<EntityType<?>> OLD_SPLASH_SOUND = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.createModLocation("old_splash_sound"));
}
