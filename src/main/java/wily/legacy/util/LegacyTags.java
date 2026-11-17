package wily.legacy.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import wily.legacy.Legacy4J;

public class LegacyTags {
    public static final TagKey<Item> FULL_SIZE_SLOT_ITEMS = TagKey.create(Registries.ITEM, Legacy4J.identifier("full_size_slot_items"));
    public static final TagKey<Item> PADDED_SLOT_ITEMS = TagKey.create(Registries.ITEM, Legacy4J.identifier("padded_slot_items"));
    public static final TagKey<Item> REQUIRES_BUILD_AND_MINE = TagKey.create(Registries.ITEM, Legacy4J.identifier("requires_build_and_mine"));
    public static final TagKey<Block> PUSHABLE_BLOCK = TagKey.create(Registries.BLOCK, Legacy4J.identifier("pushable"));
    public static final TagKey<Block> WATER_CAULDRONS = TagKey.create(Registries.BLOCK, Legacy4J.identifier("water_cauldrons"));
    public static final TagKey<Block> SLOW_CHUNK_FEATURES = TagKey.create(Registries.BLOCK, Legacy4J.identifier("slow_chunk_features"));
    public static final TagKey<Block> DOORS_AND_SWITCHES = TagKey.create(Registries.BLOCK, Legacy4J.identifier("doors_and_switches"));
    public static final TagKey<Block> CONTAINERS = TagKey.create(Registries.BLOCK, Legacy4J.identifier("containers"));
    public static final TagKey<EntityType<?>> BABY_ZOMBIE_JOCKEY_MOUNTS = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.identifier("baby_zombie_jockey_mounts"));
    public static final TagKey<EntityType<?>> OLD_SPLASH_SOUND = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.identifier("old_splash_sound"));
    public static final TagKey<EntityType<?>> ANIMALS = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.identifier("animals"));
    public static final TagKey<EntityType<?>> BUILD_INTERACTION_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.identifier("build_interaction_entities"));
    public static final TagKey<EntityType<?>> CONTAINER_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Legacy4J.identifier("container_entities"));
}
