package wily.legacy.skins.client.render.boxloader;

import net.minecraft.world.entity.HumanoidArm;

import java.util.Locale;

public enum ToolSlot {
    RIGHT(HumanoidArm.RIGHT),
    LEFT(HumanoidArm.LEFT);

    private final HumanoidArm arm;

    ToolSlot(HumanoidArm arm) {
        this.arm = arm;
    }

    public HumanoidArm arm() {
        return arm;
    }

    public static ToolSlot fromArm(HumanoidArm arm) {
        if (arm == HumanoidArm.RIGHT) return RIGHT;
        if (arm == HumanoidArm.LEFT) return LEFT;
        return null;
    }

    public static ToolSlot fromString(String key) {
        if (key == null) return null;
        return switch (key.trim().toLowerCase(Locale.ROOT)) {
            case "tool0", "tool_0", "right", "right_tool", "right_hand" -> RIGHT;
            case "tool1", "tool_1", "left", "left_tool", "left_hand" -> LEFT;
            default -> null;
        };
    }
}
