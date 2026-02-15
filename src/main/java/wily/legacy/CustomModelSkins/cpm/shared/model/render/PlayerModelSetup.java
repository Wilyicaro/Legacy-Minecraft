package wily.legacy.CustomModelSkins.cpm.shared.model.render;

public class PlayerModelSetup {
    public enum ArmPose {
        EMPTY(false);
        private static final ArmPose[] VALUES = values();
        private final boolean twoHanded;

        ArmPose(boolean twoHanded) {
            this.twoHanded = twoHanded;
        }

        public static <T extends Enum<T>> ArmPose of(T value) {
            if (value == null) return EMPTY;
            for (ArmPose p : VALUES) if (p.name().equals(value.name())) return p;
            return EMPTY;
        }
    }
}
