package wily.legacy.CustomModelSkins.cpm.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.ImageIO.IImageIO;
import wily.legacy.CustomModelSkins.cpm.mixin.access.NativeImageAccessor;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class NativeImageIO implements IImageIO {
    @Override
    public Image read(File f) throws IOException {
        try (FileInputStream fi = new FileInputStream(f)) {
            return read(fi);
        }
    }

    @Override
    public Image read(InputStream f) throws IOException {
        if (f == null) {
            return new wily.legacy.CustomModelSkins.cpl.util.Image(1, 1);
        }
        try (NativeImage ni = NativeImage.read(f)) {
            Image i = new Image(ni.getWidth(), ni.getHeight());
            for (int y = 0; y < ni.getHeight(); y++) {
                for (int x = 0; x < ni.getWidth(); x++) {
                    int rgb = ni.getPixel(x, y);
                    i.setRGB(x, y, rgb);
                }
            }
            return i;
        }
    }

    @Override
    public void write(Image img, File f) throws IOException {
        try (NativeImage i = createFromBufferedImage(img)) {
            i.writeToFile(f);
        }
    }

    @Override
    public void write(Image img, OutputStream f) throws IOException {
        try (NativeImage i = createFromBufferedImage(img); WritableByteChannel writablebytechannel = Channels.newChannel(f)) {
            if (!((NativeImageAccessor) (Object) i).callWriteToChannel(writablebytechannel)) {
                throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
            }
        }
    }

    @Override
    public Vec2i getSize(InputStream din) throws IOException {
        ByteBuffer byteBufferIn = null;
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            byteBufferIn = TextureUtil.readResource(din);
            ((Buffer) byteBufferIn).rewind();
            IntBuffer intbuffer = memorystack.mallocInt(1);
            IntBuffer intbuffer1 = memorystack.mallocInt(1);
            IntBuffer intbuffer2 = memorystack.mallocInt(1);
            if (!STBImage.stbi_info_from_memory(byteBufferIn, intbuffer, intbuffer1, intbuffer2)) {
                throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }
            return new Vec2i(intbuffer.get(0), intbuffer1.get(0));
        } finally {
            MemoryUtil.memFree(byteBufferIn);
        }
    }

    public static NativeImage createFromBufferedImage(Image texture) {
        NativeImage ni = new NativeImage(texture.getWidth(), texture.getHeight(), false);
        for (int y = 0; y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidth(); x++) {
                int rgb = texture.getRGB(x, y);
                ni.setPixel(x, y, rgb);
            }
        }
        return ni;
    }
}
