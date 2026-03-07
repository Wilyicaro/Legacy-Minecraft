package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;

import java.util.UUID;

public final class StiffArmsPose {
    private StiffArmsPose() {}

    private static boolean consoleskins$isBlockPose(Object pose) {
        if (pose == null) return false;
        try {
            if (pose instanceof Enum<?> e) return "BLOCK".equals(e.name());
            return "BLOCK".equals(String.valueOf(pose));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean consoleskins$armPoseIsBlocking(PlayerModel model, boolean right) {
        if (model == null) return false;
        try {
            var f = model.getClass().getDeclaredField(right ? "rightArmPose" : "leftArmPose");
            f.setAccessible(true);
            return consoleskins$isBlockPose(f.get(model));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Player getPlayer(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return null;
        try {
            UUID uuid = a.consoleskins$getEntityUuid();
            if (uuid == null) return null;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return null;
            return mc.level.getPlayerByUUID(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        if (!StiffArmsConfig.isStiffArmsSkin(id)) return false;
        if (state.pose == Pose.SWIMMING) return false;
        Player p = getPlayer(state);
        if (p != null && p.getPose() == Pose.SWIMMING) return false;
        return state.pose == Pose.STANDING || state.pose == Pose.CROUCHING || state.pose == Pose.FALL_FLYING;
    }

        public static void apply(PlayerModel model, AvatarRenderState state) {
            if (state != null && state.pose == Pose.SWIMMING) return;
            Player p = getPlayer(state);
            if (p != null && p.getPose() == Pose.SWIMMING) return;

            float rArmX0 = model.rightArm.x;
            float rArmY0 = model.rightArm.y;
            float rArmZ0 = model.rightArm.z;
            float rArmXRot0 = model.rightArm.xRot;
            float rArmYRot0 = model.rightArm.yRot;
            float rArmZRot0 = model.rightArm.zRot;

            float lArmX0 = model.leftArm.x;
            float lArmY0 = model.leftArm.y;
            float lArmZ0 = model.leftArm.z;
            float lArmXRot0 = model.leftArm.xRot;
            float lArmYRot0 = model.leftArm.yRot;
            float lArmZRot0 = model.leftArm.zRot;

            float rSleeveX0 = model.rightSleeve.x;
            float rSleeveY0 = model.rightSleeve.y;
            float rSleeveZ0 = model.rightSleeve.z;
            float rSleeveXRot0 = model.rightSleeve.xRot;
            float rSleeveYRot0 = model.rightSleeve.yRot;
            float rSleeveZRot0 = model.rightSleeve.zRot;

            float lSleeveX0 = model.leftSleeve.x;
            float lSleeveY0 = model.leftSleeve.y;
            float lSleeveZ0 = model.leftSleeve.z;
            float lSleeveXRot0 = model.leftSleeve.xRot;
            float lSleeveYRot0 = model.leftSleeve.yRot;
            float lSleeveZRot0 = model.leftSleeve.zRot;

            boolean rightBlocking = false;
            boolean leftBlocking = false;
            if (p != null) {
                try {
                    if (p.isUsingItem()) {
                        var use = p.getUseItem();
                        boolean block = false;
                        try {
                            block = p.isBlocking();
                        } catch (Throwable ignored2) {
                        }
                        if (!block) {
                            try {
                                block = use.is(Items.SHIELD) || use.getItem() instanceof ShieldItem;
                            } catch (Throwable ignored2) {
                            }
                        }
                        if (block) {
                            InteractionHand used = p.getUsedItemHand();
                            HumanoidArm main = p.getMainArm();
                            HumanoidArm arm = used == InteractionHand.MAIN_HAND ? main : (main == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
                            rightBlocking = arm == HumanoidArm.RIGHT;
                            leftBlocking = arm == HumanoidArm.LEFT;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            if (!rightBlocking && consoleskins$armPoseIsBlocking(model, true)) rightBlocking = true;
            if (!leftBlocking && consoleskins$armPoseIsBlocking(model, false)) leftBlocking = true;

            if (!rightBlocking) {
                model.rightArm.xRot = 0.0F;
                model.rightArm.yRot = 0.0F;
                model.rightArm.zRot = 0.0F;
            }
            if (!leftBlocking) {
                model.leftArm.xRot = 0.0F;
                model.leftArm.yRot = 0.0F;
                model.leftArm.zRot = 0.0F;
            }

            float attackTime = state == null ? 0.0F : state.attackTime;

            float t = getAgeInTicks(state);
            float idleScale = 1.0F - attackTime;
            if (idleScale < 0.0F) idleScale = 0.0F;
            if (idleScale > 0.0F) {
                float cos = Mth.cos(t * 0.45F) * 0.06F * idleScale;
                float sin = Mth.sin(t * 0.37F) * 0.04F * idleScale;
                float yaw = Mth.sin(t * 0.28F) * 0.02F * idleScale;
                if (!rightBlocking) {
                    model.rightArm.zRot += cos;
                    model.rightArm.xRot += sin;
                    model.rightArm.yRot += yaw;
                }
                if (!leftBlocking) {
                    model.leftArm.zRot -= cos;
                    model.leftArm.xRot -= sin;
                    model.leftArm.yRot -= yaw;
                }
            }

            if (attackTime > 0.0F) {
                HumanoidArm arm = state.attackArm;
                if (arm == null) arm = HumanoidArm.RIGHT;

                float bodyY = Mth.sin(Mth.sqrt(attackTime) * Mth.PI * 2.0F) * 0.2F;
                if (arm == HumanoidArm.LEFT) bodyY *= -1.0F;

                model.body.yRot = bodyY;
                model.jacket.yRot = bodyY;

                model.rightArm.z = Mth.sin(bodyY) * 5.0F;
                model.rightArm.x = -Mth.cos(bodyY) * 5.0F;
                model.leftArm.z = -Mth.sin(bodyY) * 5.0F;
                model.leftArm.x = Mth.cos(bodyY) * 5.0F;

                model.rightArm.yRot += bodyY;
                model.leftArm.yRot += bodyY;
                model.leftArm.xRot += bodyY;

                float g = 1.0F - attackTime;
                g *= g;
                g *= g;
                g = 1.0F - g;

                float swing = Mth.sin(g * Mth.PI);
                float lift = Mth.sin(attackTime * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;

                if (arm == HumanoidArm.LEFT) {
                    model.leftArm.xRot -= swing * 1.2F + lift;
                    model.leftArm.yRot += bodyY * 2.0F;
                    model.leftArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
                } else {
                    model.rightArm.xRot -= swing * 1.2F + lift;
                    model.rightArm.yRot += bodyY * 2.0F;
                    model.rightArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
                }
            }

            if (rightBlocking) {
                model.rightArm.x = rArmX0;
                model.rightArm.y = rArmY0;
                model.rightArm.z = rArmZ0;
                model.rightArm.xRot = rArmXRot0;
                model.rightArm.yRot = rArmYRot0;
                model.rightArm.zRot = rArmZRot0;

                model.rightSleeve.x = rSleeveX0;
                model.rightSleeve.y = rSleeveY0;
                model.rightSleeve.z = rSleeveZ0;
                model.rightSleeve.xRot = rSleeveXRot0;
                model.rightSleeve.yRot = rSleeveYRot0;
                model.rightSleeve.zRot = rSleeveZRot0;
            } else {
                model.rightSleeve.x = model.rightArm.x;
                model.rightSleeve.y = model.rightArm.y;
                model.rightSleeve.z = model.rightArm.z;
                model.rightSleeve.xRot = model.rightArm.xRot;
                model.rightSleeve.yRot = model.rightArm.yRot;
                model.rightSleeve.zRot = model.rightArm.zRot;
            }

            if (leftBlocking) {
                model.leftArm.x = lArmX0;
                model.leftArm.y = lArmY0;
                model.leftArm.z = lArmZ0;
                model.leftArm.xRot = lArmXRot0;
                model.leftArm.yRot = lArmYRot0;
                model.leftArm.zRot = lArmZRot0;

                model.leftSleeve.x = lSleeveX0;
                model.leftSleeve.y = lSleeveY0;
                model.leftSleeve.z = lSleeveZ0;
                model.leftSleeve.xRot = lSleeveXRot0;
                model.leftSleeve.yRot = lSleeveYRot0;
                model.leftSleeve.zRot = lSleeveZRot0;
            } else {
                model.leftSleeve.x = model.leftArm.x;
                model.leftSleeve.y = model.leftArm.y;
                model.leftSleeve.z = model.leftArm.z;
                model.leftSleeve.xRot = model.leftArm.xRot;
                model.leftSleeve.yRot = model.leftArm.yRot;
                model.leftSleeve.zRot = model.leftArm.zRot;
            }
        }

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
