package wily.legacy.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.world.item.component.CustomData;
//?}

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record MusicDiscHunt(ResourceLocation id, List<Disc> discs, String message, int tipTime) {
    public static final String DEFAULT_MESSAGE = "legacy.music_disc_hunt.progress";
    public static final int DEFAULT_TIP_TIME = 7;
    private static final Codec<MusicDiscHunt> CODEC = RecordCodecBuilder.<MusicDiscHunt>create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(hunt -> hunt.id),
        Disc.CODEC.listOf().fieldOf("discs").forGetter(hunt -> hunt.discs),
        Codec.STRING.optionalFieldOf("message", DEFAULT_MESSAGE).forGetter(hunt -> hunt.message),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("tipTime", DEFAULT_TIP_TIME).forGetter(hunt -> hunt.tipTime)
    ).apply(i, MusicDiscHunt::new)).flatXmap(MusicDiscHunt::validate, DataResult::success);
    public static final Codec<List<MusicDiscHunt>> LIST_CODEC = CODEC.listOf().flatXmap(MusicDiscHunt::validateList, DataResult::success);

    public MusicDiscHunt {
        discs = List.copyOf(discs);
    }

    public String progressKey(Disc disc) {
        return id + "|" + disc.discId;
    }

    public int foundCount(Set<String> progress) {
        int count = 0;
        for (Disc disc : discs) {
            if (progress.contains(progressKey(disc))) count++;
        }
        return count;
    }

    private static DataResult<MusicDiscHunt> validate(MusicDiscHunt hunt) {
        if (hunt.discs.isEmpty()) return DataResult.error(() -> "Music disc hunt " + hunt.id + " has no discs");
        Set<Integer> discIds = new HashSet<>();
        for (Disc disc : hunt.discs) {
            if (!discIds.add(disc.discId)) return DataResult.error(() -> "Music disc hunt " + hunt.id + " contains duplicate disc id " + disc.discId);
        }
        return DataResult.success(hunt);
    }

    private static DataResult<List<MusicDiscHunt>> validateList(List<MusicDiscHunt> hunts) {
        Set<ResourceLocation> ids = new HashSet<>();
        Set<Integer> discIds = new HashSet<>();
        for (MusicDiscHunt hunt : hunts) {
            if (!ids.add(hunt.id)) return DataResult.error(() -> "Duplicate music disc hunt id " + hunt.id);
            for (Disc disc : hunt.discs) {
                if (!discIds.add(disc.discId)) return DataResult.error(() -> "Duplicate music disc id " + disc.discId);
            }
        }
        return DataResult.success(hunts);
    }

    public record Disc(int discId, Item item) {
        private static final String DISC_ID = "discId";
        private static final Codec<Disc> CODEC = RecordCodecBuilder.<Disc>create(i -> i.group(
            Codec.intRange(1, 32).fieldOf(DISC_ID).forGetter(disc -> disc.discId),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(disc -> disc.item)
        ).apply(i, Disc::new));

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || stack.getItem() != item) return false;
            //? if >=1.20.5 {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(DISC_ID)/*? if >=1.21.5 {*//*.orElse(-1)*//*?}*/ == discId;
            //?} else {
            /*return stack.hasTag() && stack.getTag().getInt(DISC_ID) == discId;
            *///?}
        }
    }
}
