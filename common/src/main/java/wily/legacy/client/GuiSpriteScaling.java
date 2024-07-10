package wily.legacy.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface GuiSpriteScaling {
    Codec<GuiSpriteScaling> CODEC = GuiSpriteScaling.Type.CODEC.dispatch(GuiSpriteScaling::type, Type::codec);
    GuiSpriteScaling DEFAULT = new Stretch();

    Type type();

    enum Type implements StringRepresentable {
        STRETCH("stretch", GuiSpriteScaling.Stretch.CODEC),
        TILE("tile", GuiSpriteScaling.Tile.CODEC),
        NINE_SLICE("nine_slice", GuiSpriteScaling.NineSlice.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private final String key;
        private final Codec<? extends GuiSpriteScaling> codec;

        Type(String string2, Codec codec) {
            this.key = string2;
            this.codec = codec;
        }

        public String getSerializedName() {
            return this.key;
        }

        public Codec<? extends GuiSpriteScaling> codec() {
            return this.codec;
        }
    }

    class Stretch implements GuiSpriteScaling {
        public static final Codec<Stretch> CODEC = Codec.unit(Stretch::new);

        public Type type() {
            return GuiSpriteScaling.Type.STRETCH;
        }
    }
    record NineSlice(int width, int height, Border border) implements GuiSpriteScaling {
        public static final Codec<NineSlice> CODEC = GuiMetadataSection.validate(RecordCodecBuilder.create((instance) -> instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(NineSlice::width), ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(NineSlice::height), Border.CODEC.fieldOf("border").forGetter(NineSlice::border)).apply(instance, NineSlice::new)), NineSlice::validate);

        private static DataResult<NineSlice> validate(NineSlice arg) {
            Border border = arg.border();
            if (border.left() + border.right() >= arg.width()) {
                return DataResult.error(() -> {
                    int var10000 = border.left();
                    return "Nine-sliced texture has no horizontal center slice: " + var10000 + " + " + border.right() + " >= " + arg.width();
                });
            } else {
                return border.top() + border.bottom() >= arg.height() ? DataResult.error(() -> {
                    int var10000 = border.top();
                    return "Nine-sliced texture has no vertical center slice: " + var10000 + " + " + border.bottom() + " >= " + arg.height();
                }) : DataResult.success(arg);
            }
        }

        public Type type() {
            return GuiSpriteScaling.Type.NINE_SLICE;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }

        public Border border() {
            return this.border;
        }

        public record Border(int left, int top, int right, int bottom) {
            private static final Codec<Border> VALUE_CODEC = ExtraCodecs.POSITIVE_INT.flatComapMap((integer) -> new Border(integer, integer, integer, integer), (arg) -> {
                OptionalInt optionalInt = arg.unpackValue();
                return optionalInt.isPresent() ? DataResult.success(optionalInt.getAsInt()) : DataResult.error(() -> "Border has different side sizes");
            });
            private static final Codec<Border> RECORD_CODEC = RecordCodecBuilder.create((instance) -> instance.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(Border::left), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(Border::top), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(Border::right), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(Border::bottom)).apply(instance, Border::new));
            static final Codec<Border> CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC).xmap((either) -> either.map(Function.identity(), Function.identity()), (arg) -> arg.unpackValue().isPresent() ? Either.left(arg) : Either.right(arg));

            private OptionalInt unpackValue() {
                return this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom() ? OptionalInt.of(this.left()) : OptionalInt.empty();
            }

            public int left() {
                return this.left;
            }

            public int top() {
                return this.top;
            }

            public int right() {
                return this.right;
            }

            public int bottom() {
                return this.bottom;
            }
        }
    }


    record Tile(int width, int height) implements GuiSpriteScaling {
        public static final Codec<Tile> CODEC = RecordCodecBuilder.create((instance) -> instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(Tile::width), ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(Tile::height)).apply(instance, Tile::new));

        public Type type() {
            return GuiSpriteScaling.Type.TILE;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }
    }
}
