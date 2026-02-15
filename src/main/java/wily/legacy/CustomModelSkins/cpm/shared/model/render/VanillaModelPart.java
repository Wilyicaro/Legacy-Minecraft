package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpm.shared.model.PartValues;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;

public interface VanillaModelPart {
    default int getId(RenderedCube id) {
        return id.getCube().id;
    }

    PartValues getDefaultSize(SkinType skinType);

    default VanillaModelPart getCopyFrom() {
        return null;
    }

    default boolean needsPoseSetup() {
        return false;
    }
}
