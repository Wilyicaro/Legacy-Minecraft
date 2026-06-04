package wily.legacy.client;

import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Map;
import java.util.WeakHashMap;

public class LegacyMapFillAnimation {
    private static final long DURATION = 5500L;
    private static final Map<MapRenderState, State> STATES = new WeakHashMap<>();
    private static MapId mapId;
    private static MapId pendingMapId;
    private static boolean pendingReusedMap;
    private static long startTime;

    public static void start(MapId id) {
        start(id, false);
    }

    public static void start(MapId id, boolean reusedMap) {
        pendingMapId = id;
        pendingReusedMap = reusedMap;
        mapId = null;
    }

    public static void track(MapRenderState state, MapId id, MapItemSavedData data) {
        startWhenReady(id, data);
        if (isActive(id)) {
            STATES.put(state, new State(id, data.colors));
        } else {
            STATES.remove(state);
        }
    }

    public static boolean isColumnVisible(MapRenderState state, int x) {
        State stateData = STATES.get(state);
        if (stateData == null || !isActive(stateData.mapId())) {
            STATES.remove(state);
            return true;
        }
        float progress = Mth.clamp((Util.getMillis() - startTime) / (float) DURATION, 0.0f, 1.0f);
        int pass = progress < 0.5f ? 0 : 1;
        int columns = Mth.clamp(Mth.floor((progress * 2.0f - pass) * 65.0f), 0, 64);
        int column = x / 2;
        boolean firstPassColumn = (x & 1) == 0;
        return pass == 0 ? firstPassColumn && column < columns : firstPassColumn || column < columns;
    }

    public static byte[] colors(MapRenderState state) {
        State stateData = STATES.get(state);
        if (stateData == null || !isActive(stateData.mapId())) return null;
        return stateData.colors();
    }

    private static boolean isActive(MapId id) {
        return id != null && id.equals(mapId) && Util.getMillis() - startTime < DURATION;
    }

    private static void startWhenReady(MapId id, MapItemSavedData data) {
        if (id == null || !id.equals(pendingMapId)) return;
        if (data.scale < 3 || pendingReusedMap) {
            clearPending();
            return;
        }
        if (!hasPixels(data.colors)) return;
        mapId = id;
        clearPending();
        startTime = Util.getMillis();
    }

    private static void clearPending() {
        pendingMapId = null;
        pendingReusedMap = false;
    }

    private static boolean hasPixels(byte[] colors) {
        for (byte color : colors) {
            if (color != 0) return true;
        }
        return false;
    }

    private record State(MapId mapId, byte[] colors) {
    }
}
