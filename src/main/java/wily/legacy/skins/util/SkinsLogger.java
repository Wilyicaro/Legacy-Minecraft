package wily.legacy.skins.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkinsLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConsoleSkins");
    private static final boolean ENABLED = Boolean.getBoolean("consoleskins.debug");

    private SkinsLogger() {
    }

    public static void debug(String msg, Object... args) {
        if (!ENABLED) return;
        LOGGER.info(msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }
}
