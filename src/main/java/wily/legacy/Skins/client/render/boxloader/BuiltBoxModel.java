package wily.legacy.Skins.client.render.boxloader;

import net.minecraft.client.model.geom.ModelPart;
import wily.legacy.mixin.base.skins.client.ModelPartAccessor;
import wily.legacy.mixin.base.skins.client.SkipDrawAccessor;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltBoxModel {
    private final int textureWidth;
    private final int textureHeight;
    private final float partScale;
    private final float bboxHeight;
    private final float bboxWidth;
    private final EnumMap<AttachSlot, List<ModelPart>> bySlot;
    private final EnumSet<AttachSlot> hideSlots;
    public BuiltBoxModel(int textureWidth, int textureHeight, float partScale, float bboxHeight, float bboxWidth, EnumMap<AttachSlot, List<ModelPart>> bySlot, EnumSet<AttachSlot> hideSlots) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.partScale = partScale;
        this.bboxHeight = bboxHeight;
        this.bboxWidth = bboxWidth;
        this.bySlot = bySlot;
        this.hideSlots = hideSlots;
    }
    public int textureWidth() { return textureWidth; }
    public int textureHeight() { return textureHeight; }
    public float partScale() { return partScale; }
    public float bboxHeight() { return bboxHeight; }
    public float bboxWidth() { return bboxWidth; }
    public List<ModelPart> get(AttachSlot slot) { return bySlot.get(slot); }
    public boolean hides(AttachSlot slot) { return hideSlots.contains(slot); }

    public BuiltBoxModel copy() {
        EnumMap<AttachSlot, List<ModelPart>> copiedSlots = new EnumMap<>(AttachSlot.class);
        bySlot.forEach((slot, parts) -> copiedSlots.put(slot, copyParts(parts)));
        return new BuiltBoxModel(textureWidth, textureHeight, partScale, bboxHeight, bboxWidth, copiedSlots, hideSlots.clone());
    }

    private static List<ModelPart> copyParts(List<ModelPart> parts) {
        if (parts == null || parts.isEmpty()) return parts;
        return parts.stream().map(BuiltBoxModel::copyPart).toList();
    }

    private static ModelPart copyPart(ModelPart source) {
        if (source == null) return null;
        ModelPartAccessor sourceAccess = (ModelPartAccessor) (Object) source;
        Map<String, ModelPart> copiedChildren = new LinkedHashMap<>();
        sourceAccess.consoleskins$getChildren().forEach((key, child) -> copiedChildren.put(key, copyPart(child)));
        ModelPart copy = new ModelPart(sourceAccess.consoleskins$getCubes(), copiedChildren);
        copy.visible = source.visible;
        copy.x = source.x;
        copy.y = source.y;
        copy.z = source.z;
        copy.xRot = source.xRot;
        copy.yRot = source.yRot;
        copy.zRot = source.zRot;
        copy.xScale = source.xScale;
        copy.yScale = source.yScale;
        copy.zScale = source.zScale;
        ((SkipDrawAccessor) (Object) copy).consoleskins$setSkipDraw(((SkipDrawAccessor) (Object) source).consoleskins$getSkipDraw());
        return copy;
    }
}
