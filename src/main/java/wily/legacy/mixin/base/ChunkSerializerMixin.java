//? if <1.21.3 {
package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
//? if >=1.21 {
/*import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @Unique
    private static final String LEGACY_WATER_LIGHT_VERSION_TAG = "legacy4j:water_light_version";
    @Unique
    private static final int LEGACY_WATER_LIGHT_VERSION = 2;

    @Inject(method = "read", at = @At("HEAD"))
    private static void legacy$invalidateOldWaterSkyLight(
            ServerLevel level, PoiManager poiManager,
            //? if >=1.21 {
            /*RegionStorageInfo regionStorageInfo,
            *///?}
            ChunkPos chunkPos, CompoundTag chunkTag, CallbackInfoReturnable<ProtoChunk> cir) {
        if (!level.dimensionType().hasSkyLight()
                || chunkTag.getInt(LEGACY_WATER_LIGHT_VERSION_TAG) >= LEGACY_WATER_LIGHT_VERSION) return;
        ListTag sections = chunkTag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            section.remove("SkyLight");
            section.remove("BlockLight");
        }
        chunkTag.remove("isLightOn");
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void legacy$markWaterSkyLightVersion(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        if (chunk.isLightCorrect()) cir.getReturnValue().putInt(LEGACY_WATER_LIGHT_VERSION_TAG, LEGACY_WATER_LIGHT_VERSION);
    }
}
//?}
