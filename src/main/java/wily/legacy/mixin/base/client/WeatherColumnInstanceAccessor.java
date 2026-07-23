//? if >=1.21.2 {
/*package wily.legacy.mixin.base.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.WeatherEffectRenderer$ColumnInstance")
public interface WeatherColumnInstanceAccessor {
    @Accessor("bottomY")
    int legacy$getBottomY();

    @Accessor("topY")
    int legacy$getTopY();
}
*///?}
