package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;

import java.util.Locale;

public enum PlayerModelParts implements VanillaModelPart {
    HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, CUSTOM_PART;
    public static final PlayerModelParts[] VALUES = values();

    public int getId(RenderedCube rc) {
        return ordinal();
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public PartValues getDefaultSize(SkinType skinType) {
        return PlayerPartValues.getFor(this, skinType);
    }
}
