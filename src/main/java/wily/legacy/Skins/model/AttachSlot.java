package wily.legacy.skins.model;

public enum AttachSlot {
    HEAD,
    HAT,
    BODY,
    LEFT_ARM,
    RIGHT_ARM,
    LEFT_LEG,
    RIGHT_LEG,

    JACKET,
    LEFT_SLEEVE,
    RIGHT_SLEEVE,
    LEFT_PANTS,
    RIGHT_PANTS;

    public static AttachSlot fromString(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase().replace('-', '_').replace(' ', '_');

        return switch (k) {
            case "head" -> HEAD;
            case "hat", "helm", "helmet" -> HAT;
            case "body", "torso" -> BODY;

            case "left_arm", "l_arm", "arm_left" -> LEFT_ARM;
            case "right_arm", "r_arm", "arm_right" -> RIGHT_ARM;

            case "left_leg", "l_leg", "leg_left" -> LEFT_LEG;
            case "right_leg", "r_leg", "leg_right" -> RIGHT_LEG;

            case "jacket", "body_overlay", "torso_overlay" -> JACKET;
            case "left_sleeve", "l_sleeve", "left_arm_overlay" -> LEFT_SLEEVE;
            case "right_sleeve", "r_sleeve", "right_arm_overlay" -> RIGHT_SLEEVE;
            case "left_pants", "l_pants", "left_leg_overlay" -> LEFT_PANTS;
            case "right_pants", "r_pants", "right_leg_overlay" -> RIGHT_PANTS;

            default -> null;
        };
    }
}
