package wily.legacy.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if <1.20.4 {
/*import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
*///?}
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import wily.legacy.init.LegacyRegistries;

public record RandomDifficultyChanceCondition(float peaceful, float easy, float normal, float hard) implements LootItemCondition {
    //? if <1.20.4 {
    /*public static final Serializer SERIALIZER = new Serializer();
    *///?} else if <1.21.1 {
    /*public static final Codec<RandomDifficultyChanceCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("peaceful", 0.0F).forGetter(RandomDifficultyChanceCondition::peaceful),
            Codec.FLOAT.optionalFieldOf("easy", 0.65F).forGetter(RandomDifficultyChanceCondition::easy),
            Codec.FLOAT.optionalFieldOf("normal", 0.65F).forGetter(RandomDifficultyChanceCondition::normal),
            Codec.FLOAT.optionalFieldOf("hard", 0.8F).forGetter(RandomDifficultyChanceCondition::hard)
    ).apply(i, RandomDifficultyChanceCondition::new));
    *///?} else {
    public static final MapCodec<RandomDifficultyChanceCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("peaceful", 0.0F).forGetter(RandomDifficultyChanceCondition::peaceful),
            Codec.FLOAT.optionalFieldOf("easy", 0.65F).forGetter(RandomDifficultyChanceCondition::easy),
            Codec.FLOAT.optionalFieldOf("normal", 0.65F).forGetter(RandomDifficultyChanceCondition::normal),
            Codec.FLOAT.optionalFieldOf("hard", 0.8F).forGetter(RandomDifficultyChanceCondition::hard)
    ).apply(i, RandomDifficultyChanceCondition::new));
    //?}

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
    public LootItemConditionType getType() {
        return LegacyRegistries.RANDOM_DIFFICULTY_CHANCE.get();
    }

    //? if <1.20.4 {
    /*public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<RandomDifficultyChanceCondition> {
        @Override
        public void serialize(JsonObject jsonObject, RandomDifficultyChanceCondition condition, JsonSerializationContext context) {
            jsonObject.addProperty("peaceful", condition.peaceful());
            jsonObject.addProperty("easy", condition.easy());
            jsonObject.addProperty("normal", condition.normal());
            jsonObject.addProperty("hard", condition.hard());
        }

        @Override
        public RandomDifficultyChanceCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            return new RandomDifficultyChanceCondition(
                    GsonHelper.getAsFloat(jsonObject, "peaceful", 0.0F),
                    GsonHelper.getAsFloat(jsonObject, "easy", 0.65F),
                    GsonHelper.getAsFloat(jsonObject, "normal", 0.65F),
                    GsonHelper.getAsFloat(jsonObject, "hard", 0.8F)
            );
        }
    }
    *///?}
}
