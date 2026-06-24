package wily.legacy.skins.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
//?}
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmPoseSupport {
    private static final String[] AGE_FIELD_NAMES = {"ageInTicks", "age", "tick", "ticks", "time"};
    private static final String[] AGE_METHOD_NAMES = {"ageInTicks", "getAgeInTicks"};
    private static final Map<Class<?>, AgeAccessor> cachedAgeAccessors = new ConcurrentHashMap<>();

    private ArmPoseSupport() {
    }

    //? if <1.21.2 {
    public static Object entityState(LivingEntity entity, float ageInTicks, float attackTime) {
        return entity instanceof Player player ? new EntityPoseState(player, ageInTicks, attackTime) : entity;
    }
    //?}

    public static String getSkinId(Object state) {
        String menuDollSkinId = MenuDollPose.getSkinId(state);
        if (menuDollSkinId != null) return menuDollSkinId;
        if (state instanceof RenderStateSkinIdAccess access) return access.consoleskins$getSkinId();
        Player player = getPlayer(state);
        if (player == null) return null;
        return SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
    }

    static UUID getEntityUuid(Object state) {
        if (state instanceof RenderStateSkinIdAccess access) return access.consoleskins$getEntityUuid();
        Player player = getPlayer(state);
        return player == null ? null : player.getUUID();
    }

    static Player getPlayer(Object state) {
        if (state instanceof EntityPoseState entityState) return entityState.player();
        if (state instanceof Player player) return player;
        if (!(state instanceof RenderStateSkinIdAccess access)) return null;
        UUID uuid = access.consoleskins$getEntityUuid();
        if (uuid == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getPlayerByUUID(uuid);
    }

    public static Pose getPose(Object state) {
        if (MenuDollPose.isState(state)) return MenuDollPose.isCrouching(state) ? Pose.CROUCHING : Pose.STANDING;
        if (state instanceof EntityPoseState entityState) return entityState.player().getPose();
        if (state instanceof Player player) return player.getPose();
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState) return renderState.pose;
        *///?}
        return null;
    }

    static boolean hasPose(Object state, SkinPoseRegistry.PoseTag tag) {
        return SkinPoseRegistry.hasPose(tag, getSkinId(state));
    }

    public static boolean isMenuDoll(Object state) {
        if (MenuDollPose.isState(state)) return true;
        //? if >=1.21.2 {
        /*return state instanceof PlayerRenderState renderState && renderState.id == GuiDollRender.MENU_DOLL_ID;
        *///?}
        //? if <1.21.2
        return false;
    }

    public static boolean isCrouching(Object state) {
        if (MenuDollPose.isState(state)) return MenuDollPose.isCrouching(state);
        if (state instanceof EntityPoseState entityState) return entityState.player().isShiftKeyDown() || entityState.player().getPose() == Pose.CROUCHING;
        if (state instanceof Player player) return player.isShiftKeyDown() || player.getPose() == Pose.CROUCHING;
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState) return renderState.isCrouching || renderState.pose == Pose.CROUCHING || renderState.hasPose(Pose.CROUCHING);
        *///?}
        return false;
    }

    public static boolean isSitting(Object state) {
        if (state instanceof RenderStateSkinIdAccess access && access.consoleskins$isSitting()) return true;
        Player player = getPlayer(state);
        return player != null && (player.isPassenger() || player.getPose() == Pose.SITTING);
    }

    public static boolean isMoving(Object state) {
        if (state instanceof RenderStateSkinIdAccess access && access.consoleskins$isMoving()) return true;
        return getMoveSpeedSq(state) > 1.0E-4F;
    }

    public static float getMoveSpeedSq(Object state) {
        if (state instanceof RenderStateSkinIdAccess access) return access.consoleskins$getMoveSpeedSq();
        Player player = getPlayer(state);
        if (player == null) return 0.0F;
        var movement = player.getDeltaMovement();
        return movement == null ? 0.0F : (float) (movement.x * movement.x + movement.z * movement.z);
    }

    public static boolean isUsingItem(Object state) {
        if (state instanceof RenderStateSkinIdAccess access && access.consoleskins$isUsingItem()) return true;
        Player player = getPlayer(state);
        return player != null && player.isUsingItem();
    }

    public static boolean isBlocking(Object state) {
        if (state instanceof RenderStateSkinIdAccess access && access.consoleskins$isBlocking()) return true;
        Player player = getPlayer(state);
        return player != null && player.isBlocking();
    }

    static boolean isFallFlying(Object state) {
        Player player = getPlayer(state);
        if (player != null && player.isFallFlying()) return true;
        //? if >=1.21.2 {
        /*return state instanceof PlayerRenderState renderState && renderState.isFallFlying;
        *///?}
        //? if <1.21.2
        return false;
    }

    static float getYRot(Object state) {
        Player player = getPlayer(state);
        if (player != null) return player.getYRot();
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState) return renderState.yRot;
        *///?}
        return 0.0F;
    }

    static float getXRot(Object state) {
        Player player = getPlayer(state);
        if (player != null) return player.getXRot();
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState) return renderState.xRot;
        *///?}
        return 0.0F;
    }

    public static float getAttackTime(Object state) {
        if (MenuDollPose.isState(state)) return MenuDollPose.getAttackTime(state);
        if (state instanceof EntityPoseState entityState) return entityState.attackTime();
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState) return renderState.attackTime;
        *///?}
        return 0.0F;
    }

    static HumanoidArm getAttackArm(Object state) {
        //? if >=1.21.2 {
        /*if (state instanceof PlayerRenderState renderState && renderState.attackArm != null) return renderState.attackArm;
        *///?}
        Player player = getPlayer(state);
        if (player == null) return HumanoidArm.RIGHT;
        if (player.swingingArm == InteractionHand.OFF_HAND) return player.getMainArm().getOpposite();
        return player.getMainArm();
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

    static ArmFlags includeModelBlocking(Object state, ArmFlags flags) {
        return flags.merge(armPoseIsBlocking(state, true), armPoseIsBlocking(state, false));
    }

    private static HumanoidArm getUsedArm(Player player) {
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

    static void applyAttackSwing(PlayerModel model, Object state, float attackTime) {
        if (attackTime <= 0.0F) return;
        HumanoidArm arm = getAttackArm(state);
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
        if (state instanceof EntityPoseState entityState) return entityState.ageInTicks();
        if (state == null) return timeFallback();
        if (isMenuDoll(state)) return timeFallback();
        float value = cachedAgeAccessors.computeIfAbsent(state.getClass(), ArmPoseSupport::resolveAgeAccessor).read(state);
        return Float.isNaN(value) ? timeFallback() : value;
    }

    static float getIdleSwayTime(Object state) {
        if (state instanceof EntityPoseState entityState) return entityState.ageInTicks() * 0.05F;
        return getAgeInTicks(state);
    }

    private static boolean armPoseIsBlocking(Object state, boolean right) {
        if (state instanceof EntityPoseState entityState) {
            ArmFlags arms = getShieldBlockingArms(entityState.player(), true);
            return right ? arms.right() : arms.left();
        }
        if (state instanceof Player player) {
            ArmFlags arms = getShieldBlockingArms(player, true);
            return right ? arms.right() : arms.left();
        }
        if (state instanceof RenderStateSkinIdAccess access && access.consoleskins$isBlocking()) {
            ArmFlags arms = getShieldBlockingArms(getPlayer(state), true);
            return right ? arms.right() : arms.left();
        }
        return false;
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

    private static float timeFallback() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
    }

    record EntityPoseState(Player player, float ageInTicks, float attackTime) {
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
