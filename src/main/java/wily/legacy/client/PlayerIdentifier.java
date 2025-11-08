package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import wily.legacy.util.LegacySprites;

public record PlayerIdentifier(int index, ResourceLocation optionsMapSprite, int color,
                               ResourceLocation mapDecorationSprite, ResourceLocation offMapDecorationSprite,
                               ResourceLocation offLimitsMapDecorationSprite) {
    public static final Codec<PlayerIdentifier> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("index").forGetter(PlayerIdentifier::index), ResourceLocation.CODEC.fieldOf("optionsMapSprite").orElse(LegacySprites.MAP_PLAYER).forGetter(PlayerIdentifier::optionsMapSprite), CommonColor.INT_COLOR_CODEC.fieldOf("color").orElse(0xFFFFFF).forGetter(PlayerIdentifier::color), ResourceLocation.CODEC.fieldOf("mapDecorationSprite").orElse(MapDecorationTypes.PLAYER.value().assetId()).forGetter(PlayerIdentifier::mapDecorationSprite), ResourceLocation.CODEC.fieldOf("offMapDecorationSprite").orElse(MapDecorationTypes.PLAYER_OFF_MAP.value().assetId()).forGetter(PlayerIdentifier::offMapDecorationSprite), ResourceLocation.CODEC.fieldOf("offLimitsMapDecorationSprite").orElse(MapDecorationTypes.PLAYER_OFF_LIMITS.value().assetId()).forGetter(PlayerIdentifier::offLimitsMapDecorationSprite)).apply(i, PlayerIdentifier::new));
    public static final PlayerIdentifier DEFAULT = new PlayerIdentifier(0, LegacySprites.MAP_PLAYER, 0xFFFFFF, MapDecorationTypes.PLAYER.value().assetId(), MapDecorationTypes.PLAYER_OFF_MAP.value().assetId(), MapDecorationTypes.PLAYER_OFF_LIMITS.value().assetId());
    public static final Int2ObjectMap<PlayerIdentifier> list = new Int2ObjectArrayMap<>();

    public static PlayerIdentifier of(int index) {
        return list.getOrDefault(index % list.size(), DEFAULT);
    }

    public ResourceLocation spriteByMapDecorationType(Holder<MapDecorationType> type) {
        if (MapDecorationTypes.PLAYER.value().equals(type.value())) return mapDecorationSprite();
        else if (MapDecorationTypes.PLAYER_OFF_MAP.value().equals(type.value())) return offMapDecorationSprite();
        else if (MapDecorationTypes.PLAYER_OFF_LIMITS.value().equals(type.value()))
            return offLimitsMapDecorationSprite();
        else return type.value().assetId();
    }
}
