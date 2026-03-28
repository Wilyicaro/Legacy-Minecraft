package wily.legacy.compat.cpm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CpmRenderCompat {
	private static final boolean PRESENT;
	private static final Class<?> PRS_CLASS;
	private static final Method PRS_GET_PLAYER;
	private static final Method PLAYER_GET_MODEL_DEF;
	private static final Field PLAYER_IS_MODEL;

	static {
		Class<?> prs = null;
		Method getPlayer = null;
		Method getModelDef = null;
		Field isModel = null;
		boolean ok = false;
		try {
			prs = Class.forName("com.tom.cpm.client.PlayerRenderStateAccess");
			getPlayer = prs.getMethod("cpm$getPlayer");
			Class<?> playerCls = Class.forName("com.tom.cpm.shared.config.Player");
			getModelDef = playerCls.getMethod("getModelDefinition");
			isModel = playerCls.getField("isModel");
			ok = true;
		} catch (Throwable ignored) {
		}
		PRS_CLASS = prs;
		PRS_GET_PLAYER = getPlayer;
		PLAYER_GET_MODEL_DEF = getModelDef;
		PLAYER_IS_MODEL = isModel;
		PRESENT = ok;
	}

	private CpmRenderCompat() {
	}

	public static boolean isCpmModelActive(Object avatarRenderState) {
		if (!PRESENT || avatarRenderState == null) return false;
		try {
			if (!PRS_CLASS.isInstance(avatarRenderState)) return false;
			Object pl = PRS_GET_PLAYER.invoke(avatarRenderState);
			if (pl == null) return false;
			if (PLAYER_GET_MODEL_DEF != null) {
				Object def = PLAYER_GET_MODEL_DEF.invoke(pl);
				if (def != null) return true;
			}
			if (PLAYER_IS_MODEL != null && PLAYER_IS_MODEL.getType() == boolean.class) {
				return PLAYER_IS_MODEL.getBoolean(pl);
			}
		} catch (Throwable ignored) {
		}
		return false;
	}
}
