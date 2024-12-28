package wily.legacy.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
//? if >=1.20.5 {
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
//?}

import wily.legacy.util.LegacySprites;

import java.util.Locale;

public record PlayerIdentifier(int index, ResourceLocation optionsMapSprite, int color, /*? if <1.20.5 {*//*byte mapDecorationIndex, byte offMapDecorationIndex, byte offLimitsMapDecorationIndex*//*?} else {*/ResourceLocation mapDecorationSprite, ResourceLocation offMapDecorationSprite, ResourceLocation offLimitsMapDecorationSprite/*?}*/) {
    //? if <1.20.5 {
    /*public static final Codec<PlayerIdentifier> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("index").forGetter(PlayerIdentifier::index), ResourceLocation.CODEC.fieldOf("optionsMapSprite").orElse(LegacySprites.MAP_PLAYER).forGetter(PlayerIdentifier::optionsMapSprite), /^^/CommonColor.INT_COLOR_CODEC.fieldOf("color").orElse(0xFFFFFF).forGetter(PlayerIdentifier::color), Codec.BYTE.fieldOf("mapDecorationIndex").orElse(MapDecoration.Type.PLAYER.getIcon()).forGetter(PlayerIdentifier::mapDecorationIndex), Codec.BYTE.fieldOf("offMapDecorationIndex").orElse(MapDecoration.Type.PLAYER_OFF_MAP.getIcon()).forGetter(PlayerIdentifier::offMapDecorationIndex), Codec.BYTE.fieldOf("offLimitsMapDecorationSprite").orElse(MapDecoration.Type.PLAYER_OFF_MAP.getIcon()).forGetter(PlayerIdentifier::offLimitsMapDecorationIndex)).apply(i, PlayerIdentifier::new));
    *///?} else {
    public static final Codec<PlayerIdentifier> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("index").forGetter(PlayerIdentifier::index), ResourceLocation.CODEC.fieldOf("optionsMapSprite").orElse(LegacySprites.MAP_PLAYER).forGetter(PlayerIdentifier::optionsMapSprite), CommonColor.INT_COLOR_CODEC.fieldOf("color").orElse(0xFFFFFF).forGetter(PlayerIdentifier::color), ResourceLocation.CODEC.fieldOf("mapDecorationSprite").orElse(MapDecorationTypes.PLAYER.value().assetId()).forGetter(PlayerIdentifier::mapDecorationSprite), ResourceLocation.CODEC.fieldOf("offMapDecorationSprite").orElse(MapDecorationTypes.PLAYER_OFF_MAP.value().assetId()).forGetter(PlayerIdentifier::offMapDecorationSprite), ResourceLocation.CODEC.fieldOf("offLimitsMapDecorationSprite").orElse(MapDecorationTypes.PLAYER_OFF_LIMITS.value().assetId()).forGetter(PlayerIdentifier::offLimitsMapDecorationSprite)).apply(i, PlayerIdentifier::new));
    //?}
    public static final PlayerIdentifier DEFAULT = new PlayerIdentifier(0, LegacySprites.MAP_PLAYER, 0xFFFFFF, /*? if <1.20.5 {*//*MapDecoration.Type.PLAYER.getIcon(),MapDecoration.Type.PLAYER_OFF_MAP.getIcon(),MapDecoration.Type.PLAYER_OFF_LIMITS.getIcon()*//*?} else {*/MapDecorationTypes.PLAYER.value().assetId(), MapDecorationTypes.PLAYER_OFF_MAP.value().assetId(), MapDecorationTypes.PLAYER_OFF_LIMITS.value().assetId()/*?}*/);
    public static final Int2ObjectMap<PlayerIdentifier> list = new Int2ObjectArrayMap<>();


    //? if <1.20.5 {
    /*public byte indexByMapDecorationType(MapDecoration.Type type){
        return switch (type){
            case PLAYER -> mapDecorationIndex;
            case PLAYER_OFF_MAP -> offMapDecorationIndex;
            case PLAYER_OFF_LIMITS -> offLimitsMapDecorationIndex;
            default -> type.getIcon();
        };
    }
    *///?} else {
    public ResourceLocation spriteByMapDecorationType(Holder<MapDecorationType> type){
        if (MapDecorationTypes.PLAYER.value().equals(type.value())) return mapDecorationSprite();
        else if (MapDecorationTypes.PLAYER_OFF_MAP.value().equals(type.value())) return offMapDecorationSprite();
        else if (MapDecorationTypes.PLAYER_OFF_LIMITS.value().equals(type.value())) return offLimitsMapDecorationSprite();
        else return type.value().assetId();
    }
    //?}

    public static PlayerIdentifier of(int index){
        return list.getOrDefault(index % list.size(), DEFAULT);
    }
}
