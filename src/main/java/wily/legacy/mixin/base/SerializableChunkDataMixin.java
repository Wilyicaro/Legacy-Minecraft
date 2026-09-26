package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class SerializableChunkDataMixin {
    @Unique
    private static final String LEGACY_WATER_LIGHT_VERSION_TAG = "legacy4j:water_light_version";
    @Unique
    private static final int LEGACY_WATER_LIGHT_VERSION = 2;
    @Shadow
    @Final
    private boolean lightCorrect;

    @Inject(method = "parse", at = @At("HEAD"))
    private static void legacy$invalidateOldWaterSkyLight(LevelHeightAccessor level, PalettedContainerFactory containerFactory, CompoundTag chunkTag, CallbackInfoReturnable<SerializableChunkData> cir) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.dimensionType().hasSkyLight()
                || chunkTag.getIntOr(LEGACY_WATER_LIGHT_VERSION_TAG, 0) >= LEGACY_WATER_LIGHT_VERSION) {
            return;
        }

        ListTag sections = chunkTag.getListOrEmpty(SerializableChunkData.SECTIONS_TAG);
        for (int i = 0; i < sections.size(); i++) {
            sections.getCompound(i).ifPresent(section -> {
                section.remove(SerializableChunkData.SKY_LIGHT_TAG);
                section.remove(SerializableChunkData.BLOCK_LIGHT_TAG);
            });
        }
        chunkTag.remove(SerializableChunkData.IS_LIGHT_ON_TAG);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void legacy$markWaterSkyLightVersion(CallbackInfoReturnable<CompoundTag> cir) {
        if (lightCorrect) cir.getReturnValue().putInt(LEGACY_WATER_LIGHT_VERSION_TAG, LEGACY_WATER_LIGHT_VERSION);
    }
}
