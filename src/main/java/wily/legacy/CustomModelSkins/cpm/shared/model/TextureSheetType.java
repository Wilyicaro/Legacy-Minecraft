package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureType;

public enum TextureSheetType {
    SKIN(true, TextureType.SKIN, new Vec2i(64, 64)), LIST_ICON(true, null, new Vec2i(8, 8)), CAPE(true, TextureType.CAPE, new Vec2i(64, 32)), ARMOR1(false, null, new Vec2i(64, 32)), ARMOR2(false, null, new Vec2i(64, 32)),
    ;
    public static final TextureSheetType[] VALUES = values();
    public final boolean editable;
    public final TextureType texType;
    private final Vec2i defSize;

    private TextureSheetType(boolean editable, TextureType type, Vec2i defSize) {
        this.editable = editable;
        this.texType = type;
        this.defSize = defSize;
    }

    public Vec2i getDefSize() {
        return new Vec2i(defSize);
    }
}
