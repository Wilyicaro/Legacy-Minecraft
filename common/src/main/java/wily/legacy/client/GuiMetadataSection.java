package wily.legacy.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ExtraCodecs;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public record GuiMetadataSection(GuiSpriteScaling scaling) {
    public static final GuiMetadataSection DEFAULT = new GuiMetadataSection(GuiSpriteScaling.DEFAULT);

    public static final Codec<GuiMetadataSection> CODEC = RecordCodecBuilder.create((instance) -> instance.group(strictOptionalField(GuiSpriteScaling.CODEC, "scaling", GuiSpriteScaling.DEFAULT).forGetter(GuiMetadataSection::scaling)).apply(instance, GuiMetadataSection::new));
    public static final MetadataSectionType<GuiMetadataSection> TYPE = new MetadataSectionType<>() {
        @Override
        public String getMetadataSectionName() {
            return "gui";
            }


        public GuiMetadataSection fromJson(JsonObject jsonObject) {
            return CODEC.parse(JsonOps.INSTANCE, jsonObject).result().orElse(DEFAULT);
        }

        @Override
        public JsonObject toJson(GuiMetadataSection object) {
            return Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, object), IllegalArgumentException::new).getAsJsonObject();
        }
    };
    public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String string) {
        return new StrictOptionalFieldCodec<>(string, codec);
    }

    public static <A> MapCodec<A> strictOptionalField(Codec<A> codec, String string, A object) {
        return strictOptionalField(codec, string).xmap((optional) -> optional.orElse(object), (object2) -> Objects.equals(object2, object) ? Optional.empty() : Optional.of(object2));
    }
    public static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final String name;
        private final Codec<A> elementCodec;

        public StrictOptionalFieldCodec(String string, Codec<A> codec) {
            this.name = string;
            this.elementCodec = codec;
        }

        public <T> DataResult<Optional<A>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            T object = mapLike.get(this.name);
            return object == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(dynamicOps, object).map(Optional::of);
        }

        public <T> RecordBuilder<T> encode(Optional<A> optional, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
            return optional.isPresent() ? recordBuilder.add(this.name, this.elementCodec.encodeStart(dynamicOps, optional.get())) : recordBuilder;
        }

        public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            return Stream.of(dynamicOps.createString(this.name));
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof StrictOptionalFieldCodec<?> o)) {
                return false;
            } else {
                return Objects.equals(this.name, o.name) && Objects.equals(this.elementCodec, o.elementCodec);
            }
        }

        public int hashCode() {
            return Objects.hash(this.name, this.elementCodec);
        }

        public String toString() {
            return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
        }
    }
    static <T> Codec<T> validate(Codec<T> codec, Function<T, DataResult<T>> function) {
        if (codec instanceof MapCodec.MapCodecCodec<T> mapCodecCodec) {
            return ExtraCodecs.validate(mapCodecCodec.codec(), function).codec();
        }
        return codec.flatXmap(function, function);
    }
}