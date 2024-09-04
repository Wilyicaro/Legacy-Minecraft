package wily.legacy.inventory;


import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.Legacy4J;

public class NBTIngredient extends Ingredient implements LegacyIngredient {
    public static final ResourceLocation ID = new ResourceLocation(Legacy4J.MOD_ID,"data_component_ingredient");
    private static final Codec<CompoundTag> NBT_CODEC = ExtraCodecs.xor(
            Codec.STRING, CompoundTag.CODEC
    ).flatXmap(either -> either.map(s -> {
        try {
            return DataResult.success(TagParser.parseTag(s));
        } catch (CommandSyntaxException e) {
            return DataResult.error(e::getMessage);
        }
    }, DataResult::success), nbtCompound -> DataResult.success(Either.left(nbtCompound.getAsString())));

    private static final Codec<NBTIngredient> ALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC);
    private static final Codec<NBTIngredient> DISALLOW_EMPTY_CODEC = createCodec(Ingredient.CODEC_NONEMPTY);

    private static Codec<NBTIngredient> createCodec(Codec<Ingredient> ingredientCodec) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        ingredientCodec.fieldOf("base").forGetter(NBTIngredient::getBase),
                        NBT_CODEC.optionalFieldOf("nbt", null).forGetter(NBTIngredient::getNbt),
                        Codec.BOOL.optionalFieldOf("strict", false).forGetter(NBTIngredient::isStrict),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(NBTIngredient::getCount)
                ).apply(instance, NBTIngredient::new)
        );
    }

    private final Ingredient base;
    @Nullable
    private final CompoundTag nbt;
    private final boolean strict;
    private final ItemStack[] items;
    private final int count;

    public NBTIngredient(Ingredient base, @Nullable CompoundTag nbt, boolean strict, int count) {
        super(Stream.empty());
        if (nbt == null && !strict) {
            throw new IllegalArgumentException("NbtIngredient can only have null NBT in strict mode");
        }
        this.count = count;

        this.base = base;
        this.nbt = nbt;
        this.strict = strict;
        items = Arrays.stream(base.getItems()).map(i-> {
            ItemStack s = i.copyWithCount(count);
            if (nbt != null) s.setTag(nbt);
            return s;
        }).filter(base).toArray(ItemStack[]::new);
    }
    public static NBTIngredient of(boolean strict, ItemStack stack, int count){
        return new NBTIngredient(Ingredient.of(stack),stack.getTag(),strict,count);
    }
    public static NBTIngredient of(boolean strict, ItemStack stack){
        return of(strict,stack,stack.getCount());
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!base.test(stack)) return false;

        if (strict) {
            return Objects.equals(nbt, stack.getTag());
        } else {
            return NbtUtils.compareNbt(nbt, stack.getTag(), true);
        }
    }

    @Override
    public boolean isEmpty() {
        return items.length == 0;
    }

    @Override
    public ItemStack[] getItems() {
        return items;
    }

    public Ingredient getBase() {
        return base;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public boolean isStrict() {
        return strict;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        base.toNetwork(buf);
        buf.writeNbt(nbt);
        buf.writeBoolean(strict);
        buf.writeVarInt(count);
    }
    public static NBTIngredient decode(FriendlyByteBuf buf){
        return new NBTIngredient(Ingredient.fromNetwork(buf), buf.readNbt(), buf.readBoolean(), buf.readVarInt());
    }

}
