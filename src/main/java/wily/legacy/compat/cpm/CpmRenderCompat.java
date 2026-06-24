package wily.legacy.compat.cpm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CpmRenderCompat {
    private static final boolean RENDER_STATE_PRESENT;
    private static final Class<?> PRS_CLASS;
    private static final Method PRS_GET_PLAYER;
    private static final Method PLAYER_GET_MODEL_DEF;
    private static final Field PLAYER_IS_MODEL;
    private static final boolean MODEL_MANAGER_PRESENT;
    private static final Field CLIENT_BASE_MC;
    private static final Method MC_GET_PLAYER_RENDER_MANAGER;
    private static final Method RENDER_MANAGER_IS_BOUND;

    static {
        Class<?> prs = null;
        Method getPlayer = null;
        Method getModelDef = null;
        Field isModel = null;
        boolean renderStateOk = false;
        try {
            prs = Class.forName("com.tom.cpm.client.PlayerRenderStateAccess");
            getPlayer = prs.getMethod("cpm$getPlayer");
            Class<?> playerCls = Class.forName("com.tom.cpm.shared.config.Player");
            getModelDef = playerCls.getMethod("getModelDefinition");
            isModel = playerCls.getField("isModel");
            renderStateOk = true;
        } catch (Throwable ignored) {
        }
        PRS_CLASS = prs;
        PRS_GET_PLAYER = getPlayer;
        PLAYER_GET_MODEL_DEF = getModelDef;
        PLAYER_IS_MODEL = isModel;
        RENDER_STATE_PRESENT = renderStateOk;

        Field clientMc = null;
        Method getPlayerRenderManager = null;
        Method isBound = null;
        boolean modelManagerOk = false;
        try {
            Class<?> clientBase = Class.forName("com.tom.cpm.client.ClientBase");
            clientMc = clientBase.getField("mc");
            Class<?> minecraftObject = Class.forName("com.tom.cpm.client.MinecraftObject");
            getPlayerRenderManager = minecraftObject.getMethod("getPlayerRenderManager");
            Class<?> renderManager = Class.forName("com.tom.cpm.shared.model.render.ModelRenderManager");
            isBound = renderManager.getMethod("isBound", Object.class);
            modelManagerOk = true;
        } catch (Throwable ignored) {
        }
        CLIENT_BASE_MC = clientMc;
        MC_GET_PLAYER_RENDER_MANAGER = getPlayerRenderManager;
        RENDER_MANAGER_IS_BOUND = isBound;
        MODEL_MANAGER_PRESENT = modelManagerOk;
    }

    private CpmRenderCompat() {
    }

    public static boolean isCpmModelActive(Object target) {
        return isCpmRenderStateActive(target) || isCpmBoundModel(target);
    }

    private static boolean isCpmRenderStateActive(Object renderState) {
        if (!RENDER_STATE_PRESENT || renderState == null) return false;
        try {
            if (!PRS_CLASS.isInstance(renderState)) return false;
            Object player = PRS_GET_PLAYER.invoke(renderState);
            if (player == null) return false;
            if (PLAYER_GET_MODEL_DEF != null) {
                Object def = PLAYER_GET_MODEL_DEF.invoke(player);
                if (def != null) return true;
            }
            if (PLAYER_IS_MODEL != null && PLAYER_IS_MODEL.getType() == boolean.class) {
                return PLAYER_IS_MODEL.getBoolean(player);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isCpmBoundModel(Object model) {
        if (!MODEL_MANAGER_PRESENT || model == null) return false;
        try {
            Object minecraftObject = CLIENT_BASE_MC.get(null);
            if (minecraftObject == null) return false;
            Object renderManager = MC_GET_PLAYER_RENDER_MANAGER.invoke(minecraftObject);
            if (renderManager == null) return false;
            Object active = RENDER_MANAGER_IS_BOUND.invoke(renderManager, model);
            return active instanceof Boolean value && value;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
