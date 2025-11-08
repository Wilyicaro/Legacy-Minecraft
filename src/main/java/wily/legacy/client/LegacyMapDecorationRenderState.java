//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public interface LegacyMapDecorationRenderState {
    static LegacyMapDecorationRenderState of(MapRenderState.MapDecorationRenderState renderState) {
        return ((LegacyMapDecorationRenderState) renderState);
    }

    Holder<MapDecorationType> getType();

    void setType(Holder<MapDecorationType> type);

    default void extractRenderState(MapDecoration decoration) {
        setType(decoration.type());
    }
}
//?}
