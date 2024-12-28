package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelStorageSource.class)
public class ClientLevelStorageSourceMixin {
    @Shadow @Final private static String TAG_DATA;

    @ModifyArg(method = "readLightweightData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/visitors/SkipFields;<init>([Lnet/minecraft/nbt/visitors/FieldSelector;)V"))
    private static FieldSelector[] readLightweightData(FieldSelector[] args){
        return new FieldSelector[]{new FieldSelector(TAG_DATA, CompoundTag.TYPE, "Player")};
    }
}
