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
                            EnumSet<AttachSlot> hideSlots,
                            Map<ModelPart, Integer> armorMasks,
                            EnumMap<AttachSlot, float[]> coreSlotSizes) {
    private List<ModelPart> copyParts(List<ModelPart> parts, Map<ModelPart, Integer> copiedMasks) {
        if (parts == null || parts.isEmpty()) return parts;
        ArrayList<ModelPart> copied = new ArrayList<>(parts.size());
        for (ModelPart part : parts) {
            ModelPart copy = copyPart(part);
            copied.add(copy);
            copiedMasks.put(copy, armorMasks.getOrDefault(part, 0));
        }
        return List.copyOf(copied);
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

    public List<ModelPart> get(AttachSlot slot, int armorMask) {
        List<ModelPart> parts = get(slot);
        if (parts == null || parts.isEmpty() || armorMask == 0) return parts;
        ArrayList<ModelPart> visible = new ArrayList<>(parts.size());
        for (ModelPart part : parts) {
            if ((armorMasks.getOrDefault(part, 0) & armorMask) == 0) visible.add(part);
        }
        return List.copyOf(visible);
    }

    public boolean hides(AttachSlot slot) {
        return hideSlots.contains(slot);
    }

    public float[] coreSlotSize(AttachSlot slot) {
        return coreSlotSizes == null ? null : coreSlotSizes.get(slot);
    }

    public BuiltBoxModel copy() {
        EnumMap<AttachSlot, List<ModelPart>> copiedSlots = new EnumMap<>(AttachSlot.class);
        IdentityHashMap<ModelPart, Integer> copiedMasks = new IdentityHashMap<>();
        bySlot.forEach((slot, parts) -> copiedSlots.put(slot, copyParts(parts, copiedMasks)));
        return new BuiltBoxModel(textureWidth, textureHeight, partScale, bboxHeight, bboxWidth, copiedSlots, hideSlots.clone(), copiedMasks, copySlotSizes(coreSlotSizes));
    }

    private static EnumMap<AttachSlot, float[]> copySlotSizes(EnumMap<AttachSlot, float[]> sizes) {
        if (sizes == null) return null;
        EnumMap<AttachSlot, float[]> copied = new EnumMap<>(AttachSlot.class);
        sizes.forEach((slot, size) -> copied.put(slot, size == null ? null : Arrays.copyOf(size, size.length)));
        return copied;
    }
}
