package wily.legacy.CustomModelSkins.cpl.util;

import wily.legacy.CustomModelSkins.cpm.shared.MinecraftClientAccess;

public class DynamicTexture {
    private Image image;
    private boolean needReload;
    private ITexture texture;

    public DynamicTexture(Image image) {
        this.image = image;
        needReload = true;
    }

    public void bind() {
        if (texture == null) {
            this.texture = MinecraftClientAccess.get().createTexture();
            needReload = true;
        }
        if (needReload) {
            texture.load(image);
            needReload = false;
        }
        texture.bind();
    }

    public void setImage(Image image) {
        this.image = image;
        this.needReload = true;
    }

    public void free() {
        if (texture != null) texture.free();
        texture = null;
    }

    public static interface ITexture {
        void bind();

        void load(Image image);

        void free();
    }

    public Image getImage() {
        return image;
    }
}
