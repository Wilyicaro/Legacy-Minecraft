package wily.legacy.mixin.base.client;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyClientWorldSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(LevelStorageSource.class)
public class ClientLevelStorageSourceMixin {
    @Shadow
    @Final
    private static String TAG_DATA;

    @ModifyArg(method = "readLightweightData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/visitors/SkipFields;<init>([Lnet/minecraft/nbt/visitors/FieldSelector;)V"))
    private static FieldSelector[] readLightweightData(FieldSelector[] args) {
        return new FieldSelector[]{new FieldSelector(TAG_DATA, CompoundTag.TYPE, "Player")};
    }

    @Inject(method = "makeLevelSummary", at = @At("RETURN"))
    private void makeLevelSummary(Dynamic<?> dynamic, LevelStorageSource.LevelDirectory levelDirectory, boolean bl, int i, CallbackInfoReturnable<LevelSummary> cir) {
        Path path = WorldGenSettings.TYPE.id().withSuffix(".dat").resolveAgainst(levelDirectory.resourcePath(LevelResource.DATA));
        if (!Files.isRegularFile(path)) return;
        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.defaultQuota());
            root.getCompound("data").flatMap(t -> t.getLong("seed")).ifPresent(seed -> LegacyClientWorldSettings.of(cir.getReturnValue().getSettings()).setDisplaySeed(seed));
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to read world gen settings from {}", path, e);
        }
    }
}
