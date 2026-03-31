package wily.legacy.Skins.client.render.boxloader;

public enum ArmorSlot {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;
    public static ArmorSlot fromString(String key) {
        if (key == null) return null;
        String k = key.trim();
        if (k.isEmpty()) return null;
        String u = k.toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (u.equals("HEAD") || u.equals("HELMET")) return HELMET;
        if (u.equals("CHEST") || u.equals("CHESTPLATE") || u.equals("CHESTPIECE") || u.equals("CHEST_PIECE") || u.equals("TORSO")) return CHESTPLATE;
        if (u.equals("LEGS") || u.equals("LEGGINGS") || u.equals("PANTS")) return LEGGINGS;
        if (u.equals("FEET") || u.equals("BOOTS") || u.equals("SHOES")) return BOOTS;
        try {
            return ArmorSlot.valueOf(u);
        } catch (IllegalArgumentException ignored) { return null; }
    }
    public static ArmorSlot fromEquipmentSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        if (slot == null) return null;
        return switch (slot) {
            case HEAD -> HELMET;
            case CHEST -> CHESTPLATE;
            case LEGS -> LEGGINGS;
            case FEET -> BOOTS;
            default -> null;
        };
    }
}
