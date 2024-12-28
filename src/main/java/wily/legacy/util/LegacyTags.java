package wily.legacy.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;

public class LegacyTags {
    public static final TagKey<Block> PUSHABLE_BLOCK = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("pushable"));
}
