package wily.legacy.client;

import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3x2f;

import java.util.function.Consumer;

public class LegacyGuiItemRenderer {
    public static final Logger LOGGER = LogManager.getLogger("legacy_gui_item_renderer");
    public static float OPACITY = 1;

    public static void pushOpacity(float hudOpacity) {
        OPACITY = hudOpacity;
    }

    public static void popOpacity() {
        OPACITY = 1;
    }

    public static void secureTranslucentRender(boolean translucent, float alpha, Consumer<Boolean> render) {
        if (!translucent) {
            render.accept(false);
            return;
        }

        pushOpacity(alpha);
        render.accept(true);
        popOpacity();
    }

    public static float getXScale(Matrix3x2f matrix) {
        return Mth.sqrt(matrix.m00() * matrix.m00() + matrix.m01() * matrix.m01());
    }

    public static float getYScale(Matrix3x2f matrix) {
        return Mth.sqrt(matrix.m10() * matrix.m10() + matrix.m11() * matrix.m11());
    }

    public static int getScale(Matrix3x2f matrix) {
        return Math.round(Math.max(getXScale(matrix), getYScale(matrix)) * 16);
    }
}
