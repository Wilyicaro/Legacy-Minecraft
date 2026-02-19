package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;

public final class StiffArmsPose {
    private StiffArmsPose() {}

    public static float getAgeInTicks(Object avatarRenderState) {
        if (avatarRenderState == null) return timeFallback();

        float v;
        v = tryGetFloatField(avatarRenderState, "ageInTicks");
        if (!Float.isNaN(v)) return v;
        v = tryGetFloatField(avatarRenderState, "age");
        if (!Float.isNaN(v)) return v;
        v = tryGetFloatField(avatarRenderState, "tick");
        if (!Float.isNaN(v)) return v;
        v = tryGetFloatField(avatarRenderState, "ticks");
        if (!Float.isNaN(v)) return v;
        v = tryGetFloatField(avatarRenderState, "time");
        if (!Float.isNaN(v)) return v;

        v = tryInvokeFloatGetter(avatarRenderState, "ageInTicks");
        if (!Float.isNaN(v)) return v;
        v = tryInvokeFloatGetter(avatarRenderState, "getAgeInTicks");
        if (!Float.isNaN(v)) return v;

        return timeFallback();
    }

    public static void removeIdleSway(PlayerModel model, float ageInTicks, boolean isMoving) {
        if (model == null) return;

        float cos = (float) Math.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        float sin = (float) Math.sin(ageInTicks * 0.067F) * 0.05F;

        model.rightArm.zRot -= cos;
        model.leftArm.zRot += cos;
        model.rightArm.xRot -= sin;
        model.leftArm.xRot += sin;

        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
        model.leftSleeve.yRot = model.leftArm.yRot;
        model.leftSleeve.zRot = model.leftArm.zRot;

        if (!isMoving) {
            float dz = (model.rightArm.zRot - model.leftArm.zRot) * 0.5F;
            if (Math.abs(dz) <= 0.12F) {
                model.rightArm.zRot -= dz;
                model.leftArm.zRot += dz;
            }

            float dx = (model.rightArm.xRot - model.leftArm.xRot) * 0.5F;
            if (Math.abs(dx) <= 0.12F) {
                model.rightArm.xRot -= dx;
                model.leftArm.xRot += dx;
            }

            model.rightSleeve.xRot = model.rightArm.xRot;
            model.rightSleeve.yRot = model.rightArm.yRot;
            model.rightSleeve.zRot = model.rightArm.zRot;
            model.leftSleeve.xRot = model.leftArm.xRot;
            model.leftSleeve.yRot = model.leftArm.yRot;
            model.leftSleeve.zRot = model.leftArm.zRot;
        }
    }

    public static void removeIdleSway(PlayerModel model, float ageInTicks) {
        removeIdleSway(model, ageInTicks, false);
    }

    private static float tryGetFloatField(Object o, String name) {
        try {
            var f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(o);
            if (v instanceof Float fl) return fl;
            if (v instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        return Float.NaN;
    }

    private static float tryInvokeFloatGetter(Object o, String name) {
        try {
            var m = o.getClass().getMethod(name);
            Object v = m.invoke(o);
            if (v instanceof Float fl) return fl;
            if (v instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        return Float.NaN;
    }

    private static float timeFallback() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
    }
}
