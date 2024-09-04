package wily.legacy.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.Legacy4J;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public interface LegacyIngredient extends Predicate<ItemStack> {
    Map<ResourceLocation, Function<FriendlyByteBuf,LegacyIngredient>> CODECS = new HashMap<>();
    ResourceLocation DEFAULT_ID = new ResourceLocation(Legacy4J.MOD_ID,"ingredient");

    static void init() {
        register(NBTIngredient.ID, NBTIngredient::decode);
    }

    ResourceLocation getId();

    static void register(ResourceLocation id, Function<FriendlyByteBuf,? extends LegacyIngredient> codec){
        CODECS.put(id, (Function<FriendlyByteBuf, LegacyIngredient>) codec);
    }
    static LegacyIngredient of(Ingredient ing){
        return (Object)ing instanceof LegacyIngredient i ? i : new LegacyIngredient() {
            @Override
            public ResourceLocation getId() {
                return DEFAULT_ID;
            }

            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public void encode(FriendlyByteBuf buf) {
                ing.toNetwork(buf);
            }

            @Override
            public boolean test(ItemStack stack) {
                return ing.test(stack);
            }

            @Override
            public Ingredient toIngredient() {
                return ing;
            }
        };
    }
    static LegacyIngredient of(ItemStack... stacks){
        return of(Ingredient.of(stacks));
    }

    default Ingredient toIngredient(){
        return (Ingredient) this;
    }

    int getCount();

    void encode(FriendlyByteBuf buf);

    static void encode(FriendlyByteBuf buf,LegacyIngredient ingredient){
        buf.writeResourceLocation(ingredient.getId());
        ingredient.encode(buf);
    }

    static LegacyIngredient decode(FriendlyByteBuf buf){
        return CODECS.getOrDefault(buf.readResourceLocation(),b-> of(Ingredient.fromNetwork(b))).apply(buf);
    }
}
