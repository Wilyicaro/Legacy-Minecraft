package wily.legacy.CustomModelSkins.cpm.shared.effects;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;
import java.util.function.Supplier;

public enum RenderEffects {
    GLOW, @Deprecated SCALE(EffectScale::new), @Deprecated HIDE(EffectHide::new), COLOR, SINGLE_TEX, PER_FACE_UV(EffectPerFaceUV::new), @Deprecated UV_OVERFLOW, ITEM(EffectRenderItem::new), HIDE_SKULL(EffectHideSkull::new), REMOVE_ARMOR_OFFSET(EffectRemoveArmorOffset::new), EXTRUDE, @Deprecated PLAYER_SCALE, MODEL_SCALE, SCALING, COPY_TRANSFORM, FIRST_PERSON_HAND, @Deprecated DISABLE_VANILLA, REMOVE_BED_OFFSET, INVIS_GLOW,
    ;
    public static final RenderEffects[] VALUES = values();
    private final Supplier<IRenderEffect> factory;

    RenderEffects() {
        this.factory = null;
    }

    RenderEffects(Supplier<IRenderEffect> factory) {
        this.factory = factory;
    }

    public IRenderEffect create() {
        return factory != null ? factory.get() : new DisabledEffect(this);
    }

    private static final class DisabledEffect implements IRenderEffect {
        private final RenderEffects e;

        DisabledEffect(RenderEffects e) {
            this.e = e;
        }

        @Override
        public void load(IOHelper in) throws IOException {
        }

        @Override
        public void write(IOHelper out) throws IOException {
        }

        @Override
        public void apply(ModelDefinition def) {
        }

        @Override
        public RenderEffects getEffect() {
            return e;
        }
    }
}
