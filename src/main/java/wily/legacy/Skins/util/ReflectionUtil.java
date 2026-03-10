package wily.legacy.Skins.util;

import java.lang.reflect.Method;

public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    public static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            try {
                Method m = cls.getMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    public static Object invoke(Method m, Object target, Object... args) {
        try {
            if (m == null) return null;
            return m.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
