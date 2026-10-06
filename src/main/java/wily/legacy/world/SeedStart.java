package wily.legacy.world;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface SeedStart {
    @Nullable BlockPos getSeedStart();

    void setSeedStart(@Nullable BlockPos pos);
}
