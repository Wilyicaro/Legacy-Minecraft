package wily.legacy.Skins.client.render;

import net.minecraft.world.entity.EquipmentSlot;

public final class ArmorOffsetContext {
    public static final ThreadLocal<EquipmentSlot> CURRENT_SLOT = new ThreadLocal<>();
    public static final ThreadLocal<float[]> CURRENT_OFFSET = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> POSE_PUSHED = new ThreadLocal<>();

    public static final ThreadLocal<Boolean> APPLIED = new ThreadLocal<>();

    public static final ThreadLocal<Boolean> FORCE_ARMOR_VISIBLE = new ThreadLocal<>();

    public static final ThreadLocal<Object> PARENT_MODEL = new ThreadLocal<>();
    public static final ThreadLocal<boolean[]> PARENT_VIS = new ThreadLocal<>();

    private ArmorOffsetContext() {
    }
}
