package wily.legacy.skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import wily.legacy.skins.client.render.boxloader.AttachSlot;

public final class PlayerModelParts {
    public static final AttachSlot[] ALL = {
            AttachSlot.HEAD,
            AttachSlot.HAT,
            AttachSlot.BODY,
            AttachSlot.RIGHT_ARM,
            AttachSlot.LEFT_ARM,
            AttachSlot.RIGHT_LEG,
            AttachSlot.LEFT_LEG,
            AttachSlot.JACKET,
            AttachSlot.RIGHT_SLEEVE,
            AttachSlot.LEFT_SLEEVE,
            AttachSlot.RIGHT_PANTS,
            AttachSlot.LEFT_PANTS
    };

    private PlayerModelParts() {
    }

    public static ModelPart get(PlayerModel model, AttachSlot slot) {
        if (model == null || slot == null) return null;
        return switch (slot) {
            case HEAD -> model.head;
            case HAT -> model.hat;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case JACKET -> model.jacket;
            case RIGHT_SLEEVE -> model.rightSleeve;
            case LEFT_SLEEVE -> model.leftSleeve;
            case RIGHT_PANTS -> model.rightPants;
            case LEFT_PANTS -> model.leftPants;
            default -> null;
        };
    }
}
