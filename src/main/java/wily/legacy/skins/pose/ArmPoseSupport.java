package wily.legacy.skins.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ArmPoseSupport {
    private static final String[] AGE_FIELD_NAMES = {"ageInTicks", "age", "tick", "ticks", "time"};
    private static final String[] AGE_METHOD_NAMES = {"ageInTicks", "getAgeInTicks"};
    private static final Map<Class<?>, AgeAccessor> cachedAgeAccessors = new ConcurrentHashMap<>();
    private static volatile java.lang.reflect.Field cachedRightArmPoseField;
    private static volatile java.lang.reflect.Field cachedLeftArmPoseField;
    private static volatile boolean triedResolveArmPoseFields;

    private ArmPoseSupport() {
    }

    static Player getPlayer(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return null;
        UUID uuid = access.consoleskins$getEntityUuid();
        if (uuid == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getPlayerByUUID(uuid);
    }

    static ArmFlags getShieldBlockingArms(Player player, boolean lenient) {
        if (player == null || !player.isUsingItem()) return ArmFlags.NONE;
        var useItem = player.getUseItem();
        boolean blocking = player.isBlocking();
        if (!blocking) {
            if (!lenient) return ArmFlags.NONE;
            blocking = useItem.is(Items.SHIELD) || useItem.getItem() instanceof ShieldItem;
        }
        if (!blocking) return ArmFlags.NONE;
        HumanoidArm usedArm = getUsedArm(player);
        return new ArmFlags(usedArm == HumanoidArm.RIGHT, usedArm == HumanoidArm.LEFT);
    }

    static ArmFlags getHoldingArms(Player player) {
        if (player == null) return ArmFlags.NONE;
        HumanoidArm mainArm = player.getMainArm();
        boolean mainItem = !player.getMainHandItem().isEmpty();
        boolean offItem = !player.getOffhandItem().isEmpty();
        boolean right = mainItem && mainArm == HumanoidArm.RIGHT || offItem && mainArm == HumanoidArm.LEFT;
        boolean left = mainItem && mainArm == HumanoidArm.LEFT || offItem && mainArm == HumanoidArm.RIGHT;
        if (player.isUsingItem()) {
            HumanoidArm usedArm = getUsedArm(player);
            right |= usedArm == HumanoidArm.RIGHT;
            left |= usedArm == HumanoidArm.LEFT;
        }
        return new ArmFlags(right, left);
    }

    static ArmFlags includeModelBlocking(AvatarRenderState state, ArmFlags flags) {
        return flags.merge(armPoseIsBlocking(state, true), armPoseIsBlocking(state, false));
    }

    static HumanoidArm getUsedArm(Player player) {
        HumanoidArm mainArm = player.getMainArm();
        if (player.getUsedItemHand() == InteractionHand.MAIN_HAND) return mainArm;
        return mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    static void applyIdleSway(PlayerModel model,
                              float ageInTicks,
                              float attackTime,
                              float zFreq,
                              float zAmp,
                              float xFreq,
                              float xAmp,
                              float yFreq,
                              float yAmp,
                              boolean rightBlocked,
                              boolean leftBlocked) {
        float idleScale = 1.0F - attackTime;
        if (idleScale <= 0.0F) return;
        float cos = Mth.cos(ageInTicks * zFreq) * zAmp * idleScale;
        float sin = Mth.sin(ageInTicks * xFreq) * xAmp * idleScale;
        float yaw = Mth.sin(ageInTicks * yFreq) * yAmp * idleScale;
        if (!rightBlocked) {
            model.rightArm.zRot += cos;
            model.rightArm.xRot += sin;
            model.rightArm.yRot += yaw;
        }
        if (!leftBlocked) {
            model.leftArm.zRot -= cos;
            model.leftArm.xRot -= sin;
            model.leftArm.yRot -= yaw;
        }
    }

    static void applyAttackSwing(PlayerModel model, AvatarRenderState state, float attackTime) {
        if (attackTime <= 0.0F) return;
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
        float swingCurve = 1.0F - attackTime;
        swingCurve *= swingCurve;
        swingCurve *= swingCurve;
        swingCurve = 1.0F - swingCurve;
        float swing = Mth.sin(swingCurve * Mth.PI);
        float lift = Mth.sin(attackTime * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        if (arm == HumanoidArm.LEFT) {
            model.leftArm.xRot -= swing * 1.2F + lift;
            model.leftArm.yRot += bodyY * 2.0F;
            model.leftArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
            return;
        }
        model.rightArm.xRot -= swing * 1.2F + lift;
        model.rightArm.yRot += bodyY * 2.0F;
        model.rightArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
    }

    static float getAgeInTicks(Object state) {
        if (state == null) return timeFallback();
        if (!(state instanceof AvatarRenderState avatarState)) return timeFallback();
        if (avatarState.id == GuiDollRender.MENU_DOLL_ID) return timeFallback();
        float value = cachedAgeAccessors.computeIfAbsent(state.getClass(), ArmPoseSupport::resolveAgeAccessor).read(state);
        return Float.isNaN(value) ? timeFallback() : value;
    }

    private static boolean armPoseIsBlocking(AvatarRenderState state, boolean right) {
        if (state == null) return false;
        return isBlockPose(right ? state.rightArmPose : state.leftArmPose);
    }

    private static boolean isBlockPose(HumanoidModel.ArmPose pose) {
        return pose == HumanoidModel.ArmPose.BLOCK;
    }

    private static AgeAccessor resolveAgeAccessor(Class<?> type) {
        for (String name : AGE_FIELD_NAMES) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return new AgeAccessor(field, null);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        for (String name : AGE_METHOD_NAMES) {
            try {
                java.lang.reflect.Method method = type.getMethod(name);
                method.setAccessible(true);
                return new AgeAccessor(null, method);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return AgeAccessor.NONE;
    }

    private static java.lang.reflect.Field resolveArmPoseField(boolean right) {
        if (!triedResolveArmPoseFields) {
            synchronized (ArmPoseSupport.class) {
                if (!triedResolveArmPoseFields) {
                    triedResolveArmPoseFields = true;
                    try {
                        cachedRightArmPoseField = PlayerModel.class.getDeclaredField("rightArmPose");
                        cachedRightArmPoseField.setAccessible(true);
                    } catch (ReflectiveOperationException ignored) {
                    }
                    try {
                        cachedLeftArmPoseField = PlayerModel.class.getDeclaredField("leftArmPose");
                        cachedLeftArmPoseField.setAccessible(true);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }
        return right ? cachedRightArmPoseField : cachedLeftArmPoseField;
    }

    private static float timeFallback() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
    }

    record ArmState(float armX, float armY, float armZ, float armXRot, float armYRot, float armZRot, float armXScale,
                    float armYScale, float armZScale,
                    float sleeveX, float sleeveY, float sleeveZ, float sleeveXRot, float sleeveYRot, float sleeveZRot,
                    float sleeveXScale,
                    float sleeveYScale, float sleeveZScale) {
        static ArmState capture(ModelPart arm, ModelPart sleeve) {
            return new ArmState(arm.x, arm.y, arm.z, arm.xRot, arm.yRot, arm.zRot, arm.xScale, arm.yScale, arm.zScale, sleeve.x, sleeve.y, sleeve.z,
                    sleeve.xRot, sleeve.yRot, sleeve.zRot, sleeve.xScale, sleeve.yScale, sleeve.zScale);
        }

        void restore(ModelPart arm, ModelPart sleeve) {
            arm.x = armX;
            arm.y = armY;
            arm.z = armZ;
            arm.xRot = armXRot;
            arm.yRot = armYRot;
            arm.zRot = armZRot;
            arm.xScale = armXScale;
            arm.yScale = armYScale;
            arm.zScale = armZScale;
            sleeve.x = sleeveX;
            sleeve.y = sleeveY;
            sleeve.z = sleeveZ;
            sleeve.xRot = sleeveXRot;
            sleeve.yRot = sleeveYRot;
            sleeve.zRot = sleeveZRot;
            sleeve.xScale = sleeveXScale;
            sleeve.yScale = sleeveYScale;
            sleeve.zScale = sleeveZScale;
        }

        void syncSleeve(ModelPart arm, ModelPart sleeve) {
            sleeve.x = arm.x;
            sleeve.y = arm.y;
            sleeve.z = arm.z;
            sleeve.xRot = arm.xRot;
            sleeve.yRot = arm.yRot;
            sleeve.zRot = arm.zRot;
            sleeve.xScale = arm.xScale;
            sleeve.yScale = arm.yScale;
            sleeve.zScale = arm.zScale;
        }
    }

    record ArmFlags(boolean right, boolean left) {
        static final ArmFlags NONE = new ArmFlags(false, false);

        ArmFlags merge(boolean right, boolean left) {
            return new ArmFlags(this.right || right, this.left || left);
        }
    }

    record AgeAccessor(java.lang.reflect.Field field, java.lang.reflect.Method method) {
        static final AgeAccessor NONE = new AgeAccessor(null, null);

        float read(Object target) {
            try {
                Object value = field != null ? field.get(target) : method != null ? method.invoke(target) : null;
                if (value instanceof Float number) return number;
                if (value instanceof Number number) return number.floatValue();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            return Float.NaN;
        }
    }
}
