package wily.legacy.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;

import java.util.*;

public class StrictComponentsIngredient implements CustomIngredient {
    public static final CustomIngredientSerializer<StrictComponentsIngredient> SERIALIZER = new StrictComponentsIngredient.Serializer();

    private final Ingredient base;
    private final DataComponentPatch components;


    public StrictComponentsIngredient(Ingredient base, DataComponentPatch components) {
        this.base = base;
        this.components = components;
    }
    public static StrictComponentsIngredient of(ItemStack stack){
        return new StrictComponentsIngredient(Ingredient.of(stack),stack.getComponentsPatch());
    }
    @Override
    public boolean test(ItemStack stack) {
        if (!base.test(stack)) return false;
        return stack.getComponentsPatch().equals(getComponents());
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of(base.getItems()));
        stacks.replaceAll(stack -> {
            ItemStack copy = stack.copy();

            stack.applyComponentsAndValidate(components);

            return copy;
        });
        stacks.removeIf(stack -> !base.test(stack));
        return stacks;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private Ingredient getBase() {
        return base;
    }

    @Nullable
    private DataComponentPatch getComponents() {
        return components;
    }

    private static class Serializer implements CustomIngredientSerializer<StrictComponentsIngredient> {
        private static final ResourceLocation ID = new ResourceLocation(Legacy4J.MOD_ID, "strict_components");
        private static final MapCodec<StrictComponentsIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
        private static final MapCodec<StrictComponentsIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);
        private static final StreamCodec<RegistryFriendlyByteBuf, StrictComponentsIngredient> PACKET_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, StrictComponentsIngredient::getBase,
                DataComponentPatch.STREAM_CODEC, StrictComponentsIngredient::getComponents,
                StrictComponentsIngredient::new
        );

        private static MapCodec<StrictComponentsIngredient> createCodec(Codec<Ingredient> ingredientCodec) {
            return RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            ingredientCodec.fieldOf("base").forGetter(StrictComponentsIngredient::getBase),
                            DataComponentPatch.CODEC.fieldOf("components").forGetter(StrictComponentsIngredient::getComponents)
                    ).apply(instance, StrictComponentsIngredient::new)
            );
        }

        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<StrictComponentsIngredient> getCodec(boolean allowEmpty) {
            return allowEmpty ? ALLOW_EMPTY_CODEC : DISALLOW_EMPTY_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StrictComponentsIngredient> getPacketCodec() {
            return PACKET_CODEC;
        }
    }
}

