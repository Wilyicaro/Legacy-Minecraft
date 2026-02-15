package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.math.MatrixStack;
import wily.legacy.CustomModelSkins.cpl.render.VertexBuffer;

public interface Mesh {
    public void draw(MatrixStack matrixStackIn, VertexBuffer bufferIn, float red, float green, float blue, float alpha);

    public RenderMode getLayer();

    public void free();

    public static final Mesh EMPTY = new Mesh() {
        @Override
        public RenderMode getLayer() {
            return RenderMode.DEFAULT;
        }

        @Override
        public void free() {
        }

        @Override
        public void draw(MatrixStack matrixStackIn, VertexBuffer bufferIn, float red, float green, float blue, float alpha) {
        }
    };
}
