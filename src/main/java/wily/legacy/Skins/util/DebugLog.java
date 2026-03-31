package wily.legacy.Skins.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DebugLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConsoleSkins");
    private static final boolean ENABLED = Boolean.getBoolean("consoleskins.debug");

    private DebugLog() {
    }

    public static void debug(String msg, Object... args) {
        if (!ENABLED) return;
        LOGGER.info(msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }
}
