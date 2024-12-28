//? if >=1.21.2 {
package wily.legacy.mixin.base;

import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.LegacyMapDecorationRenderState;

@Mixin(MapRenderState.MapDecorationRenderState.class)
public class MapDecorationRenderStateMixin implements LegacyMapDecorationRenderState {
    Holder<MapDecorationType> type;

    @Override
    public Holder<MapDecorationType> getType() {
        return type;
    }

    @Override
    public void setType(Holder<MapDecorationType> type) {
        this.type = type;
    }
}
//?}
