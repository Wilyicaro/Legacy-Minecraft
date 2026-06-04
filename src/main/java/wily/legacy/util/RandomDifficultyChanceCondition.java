package wily.legacy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record RandomDifficultyChanceCondition(float peaceful, float easy, float normal, float hard) implements LootItemCondition {
    public static final MapCodec<RandomDifficultyChanceCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("peaceful", 0.0F).forGetter(RandomDifficultyChanceCondition::peaceful),
            Codec.FLOAT.optionalFieldOf("easy", 0.65F).forGetter(RandomDifficultyChanceCondition::easy),
            Codec.FLOAT.optionalFieldOf("normal", 0.65F).forGetter(RandomDifficultyChanceCondition::normal),
            Codec.FLOAT.optionalFieldOf("hard", 0.8F).forGetter(RandomDifficultyChanceCondition::hard)
    ).apply(i, RandomDifficultyChanceCondition::new));

    @Override
    public boolean test(LootContext lootContext) {
        float chance = switch (lootContext.getLevel().getDifficulty()) {
            case PEACEFUL -> peaceful;
            case EASY -> easy;
            case NORMAL -> normal;
            case HARD -> hard;
        };
        return lootContext.getRandom().nextFloat() < chance;
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }
}
