package wily.legacy.Skins.client.render.boxloader;

import net.minecraft.client.model.geom.ModelPart;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public final class BuiltBoxModel {
    private final int textureWidth;
    private final int textureHeight;
    private final float bboxHeight;
    private final float bboxWidth;
    private final EnumMap<AttachSlot, List<ModelPart>> bySlot;
    private final EnumSet<AttachSlot> hideSlots;

    public BuiltBoxModel(int textureWidth, int textureHeight, float bboxHeight, float bboxWidth, EnumMap<AttachSlot, List<ModelPart>> bySlot, EnumSet<AttachSlot> hideSlots) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bboxHeight = bboxHeight;
        this.bboxWidth = bboxWidth;
        this.bySlot = bySlot;
        this.hideSlots = hideSlots;
    }

    public int textureWidth() {
        return textureWidth;
    }

    public int textureHeight() {
        return textureHeight;
    }

    public float bboxHeight() {
        return bboxHeight;
    }

    public float bboxWidth() {
        return bboxWidth;
    }

    public List<ModelPart> get(AttachSlot slot) {
        return bySlot.get(slot);
    }

    public boolean hides(AttachSlot slot) {
        return hideSlots.contains(slot);
    }
}
