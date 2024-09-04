package wily.legacy.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.Legacy4J;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public interface LegacyIngredient extends Predicate<ItemStack> {
    Map<ResourceLocation,StreamCodec<RegistryFriendlyByteBuf,LegacyIngredient>> CODECS = new HashMap<>();
    StreamCodec<RegistryFriendlyByteBuf,LegacyIngredient> CODEC = StreamCodec.of(LegacyIngredient::encode,LegacyIngredient::decode);
    ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"ingredient");

    static void init() {
        register(DataComponentIngredient.ID,DataComponentIngredient.STREAM_CODEC);
    }

    ResourceLocation getId();

    static void register(ResourceLocation id, StreamCodec<RegistryFriendlyByteBuf,? extends LegacyIngredient> codec){
        CODECS.put(id, (StreamCodec<RegistryFriendlyByteBuf, LegacyIngredient>) codec);
    }

    static LegacyIngredient of(Ingredient ing){
        return (LegacyIngredient) (Object) ing;
    }
    static LegacyIngredient of(ItemStack... stacks){
        return of(Ingredient.of(stacks));
    }

    default Ingredient toIngredient(){
        return (Ingredient) (Object) this;
    }

    int getCount();

    static void encode(RegistryFriendlyByteBuf buf,LegacyIngredient ingredient){
        StreamCodec<RegistryFriendlyByteBuf,LegacyIngredient> codec = CODECS.getOrDefault(ingredient.getId(),Ingredient.CONTENTS_STREAM_CODEC.map(LegacyIngredient::of,LegacyIngredient::toIngredient));
        buf.writeResourceLocation(ingredient.getId());
        codec.encode(buf,ingredient);
    }
    static LegacyIngredient decode(RegistryFriendlyByteBuf buf){
        StreamCodec<RegistryFriendlyByteBuf,LegacyIngredient> codec = CODECS.getOrDefault(buf.readResourceLocation(),Ingredient.CONTENTS_STREAM_CODEC.map(LegacyIngredient::of,LegacyIngredient::toIngredient));
        return codec.decode(buf);
    }

}
