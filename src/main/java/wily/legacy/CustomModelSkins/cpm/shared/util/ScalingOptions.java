package wily.legacy.CustomModelSkins.cpm.shared.util;

public enum ScalingOptions {
    ENTITY("scale", true, 0.01F, 10F), EYE_HEIGHT("eyeHeight", true, 0.01F, 10F), HITBOX_WIDTH("hitboxW", false, 0.01F, 10F), HITBOX_HEIGHT("hitboxH", false, 0.01F, 10F), THIRD_PERSON("thirdPerson", true, 0.01F, 10F), VIEW_BOBBING("viewBonning", true, 0.01F, 10F), MOTION("motion", false, 0.01F, 10F), STEP_HEIGHT("stepHeight", false, 0.01F, 10F), FLIGHT_SPEED("flight", false, 0.01F, 10F), FALL_DAMAGE("fdmg", false, 0.01F, 10F), REACH("reach", false, 0.01F, 10F), MINING_SPEED("msp", false, 0.01F, 10F), ATTACK_SPEED("asp", false, 0.01F, 10F), ATTACK_KNOCKBACK("akb", false, 0.01F, 10F), ATTACK_DMG("attack", false, 0.01F, 10F), DEFENSE("defense", false, 0.01F, 10F), HEALTH("health", false, 0.1F, 4F), MOB_VISIBILITY("mob_vis", false, 0.01F, 10F), WIDTH("width", false, 0.01F, 10F), HEIGHT("height", false, 0.01F, 10F), JUMP_HEIGHT("jump_height", false, 0.01F, 10F), PROJECTILE_DMG("proj_dmg", false, 0.01F, 10F), EXPLOSION_DMG("exp_dmg", false, 0.01F, 10F), KNOCKBACK_RESIST("kbr", false, 0.01F, 10F), SAFE_FALL_DISTANCE("safe_fall", false, 0.01F, 10F),
    ;
    private final String netKey;
    private final boolean defaultEnabled;
    private final float min;
    private final float max;

    ScalingOptions(String netKey, boolean defaultEnabled, float min, float max) {
        this.netKey = netKey;
        this.defaultEnabled = defaultEnabled;
        this.min = min;
        this.max = max;
    }
}
