package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.util.TextureStitcher;

public interface IResolvedModelPart {
    IResolvedModelPart EMPTY = new IResolvedModelPart() {
        @Override
        public void preApply(wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition def) {
        }

        @Override
        public void stitch(wily.legacy.CustomModelSkins.cpm.shared.util.TextureStitcher stitcher) {
        }

        @Override
        public void apply(wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition def) {
        }
    };

    default void preApply(ModelDefinition def) {
    }

    default void apply(ModelDefinition def) {
    }

    default void stitch(TextureStitcher stitcher) {
    }
}
