package wily.legacy.skins.client.render.boxloader;

import net.minecraft.client.model.geom.ModelPart;
import wily.legacy.mixin.base.skins.client.ModelPartAccessor;

import java.util.*;

public record BuiltBoxModel(int textureWidth,
                            int textureHeight,
                            float partScale,
                            float bboxHeight,
                            float bboxWidth,
                            EnumMap<AttachSlot, List<ModelPart>> bySlot,
                            EnumSet<AttachSlot> hideSlots) {
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
        ((ModelPartAccessor) (Object) copy).consoleskins$setSkipDraw(sourceAccess.consoleskins$getSkipDraw());
        return copy;
    }

    public List<ModelPart> get(AttachSlot slot) {
        return bySlot.get(slot);
    }

    public boolean hides(AttachSlot slot) {
        return hideSlots.contains(slot);
    }

    public BuiltBoxModel copy() {
        EnumMap<AttachSlot, List<ModelPart>> copiedSlots = new EnumMap<>(AttachSlot.class);
        bySlot.forEach((slot, parts) -> copiedSlots.put(slot, copyParts(parts)));
        return new BuiltBoxModel(textureWidth, textureHeight, partScale, bboxHeight, bboxWidth, copiedSlots, hideSlots.clone());
    }
}
