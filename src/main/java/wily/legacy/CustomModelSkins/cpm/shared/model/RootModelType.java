package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;

import java.util.Locale;

public enum RootModelType implements VanillaModelPart {
    CAPE(), ELYTRA_LEFT(), ELYTRA_RIGHT(), ARMOR_HELMET(PlayerModelParts.HEAD), ARMOR_BODY(PlayerModelParts.BODY), ARMOR_LEFT_ARM(PlayerModelParts.LEFT_ARM), ARMOR_RIGHT_ARM(PlayerModelParts.RIGHT_ARM), ARMOR_LEGGINGS_BODY(PlayerModelParts.BODY), ARMOR_LEFT_LEG(PlayerModelParts.LEFT_LEG), ARMOR_RIGHT_LEG(PlayerModelParts.RIGHT_LEG), ARMOR_LEFT_FOOT(PlayerModelParts.LEFT_LEG), ARMOR_RIGHT_FOOT(PlayerModelParts.RIGHT_LEG),
    ;
    public static final RootModelType[] VALUES = values();
    private final VanillaModelPart copyFrom;

    private RootModelType() {
        this.copyFrom = null;
    }

    private RootModelType(VanillaModelPart copyFrom) {
        this.copyFrom = copyFrom;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public PartValues getDefaultSize(SkinType skinType) {
        return RootModelValues.getFor(this, skinType);
    }

    public VanillaModelPart getCopyFrom() {
        return copyFrom;
    }

    public boolean needsPoseSetup() {
        return this == CAPE;
    }
}
