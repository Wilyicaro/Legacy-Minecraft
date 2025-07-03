//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public interface LegacyMapDecorationRenderState {
    Holder<MapDecorationType> getType();
    void setType(Holder<MapDecorationType> type);
    static LegacyMapDecorationRenderState of(MapRenderState.MapDecorationRenderState renderState){
        return ((LegacyMapDecorationRenderState) renderState);
    }
    default void extractRenderState(MapDecoration decoration){
        setType(decoration.type());
    }
}
//?}
