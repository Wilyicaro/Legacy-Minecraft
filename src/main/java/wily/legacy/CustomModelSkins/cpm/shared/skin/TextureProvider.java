package wily.legacy.CustomModelSkins.cpm.shared.skin;

import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpl.util.DynamicTexture;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.ImageIO;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper.ImageBlock;

import java.io.IOException;
import java.io.OutputStream;

public class TextureProvider {
    public DynamicTexture texture;
    public Vec2i size;

    public TextureProvider(IOHelper in, ModelDefinition def) throws IOException {
        size = in.read2s();
        ImageBlock block = in.readImage();
        block.doReadImage();
        texture = new DynamicTexture(block.getImage());
    }

    public TextureProvider(Image imgIn, Vec2i size) {
        this.size = size;
        texture = new DynamicTexture(imgIn);
    }

    public void write(IOHelper dout) throws IOException {
        dout.write2s(size);
        try (OutputStream baos = dout.writeNextBlock().getDout()) {
            ImageIO.write(texture.getImage(), baos);
        }
    }

    public void bind() {
        if (texture == null) return;
        texture.bind();
    }

    public void free() {
        if (texture != null) texture.free();
    }

    public Vec2i getSize() {
        return size;
    }

    public Image getImage() {
        return texture == null ? null : texture.getImage();
    }

    public void setImage(Image image) {
        if (texture == null) texture = new DynamicTexture(image);
        else texture.setImage(image);
    }
}
