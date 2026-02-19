package wily.legacy.CustomModelSkins.cpl.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class Image {
    private final int[] data;
    private final int w, h;

    public Image(int w, int h) {
        this.w = w;
        this.h = h;
        data = new int[w * h];
    }

    public Image(Image c) {
        this(c.w, c.h);
        System.arraycopy(c.data, 0, data, 0, data.length);
    }

    public void setRGB(int x, int y, int rgb) {
        data[y * w + x] = rgb;
    }

    public int getRGB(int x, int y) {
        return data[y * w + x];
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public static CompletableFuture<Image> loadFrom(File f) {
        return ImageIO.read(f);
    }

    public static Image loadFrom(InputStream f) throws IOException {
        return ImageIO.read(f);
    }

    public void storeTo(OutputStream f) throws IOException {
        ImageIO.write(this, f);
    }

    public void draw(Image i) {
        int xMax = Math.min(w, i.w), yMax = Math.min(h, i.h);
        for (int y = 0; y < yMax; y++) System.arraycopy(i.data, y * i.w, data, y * w, xMax);
    }

    public void draw(Image i, int xs, int ys) {
        int dx = Math.max(0, xs), dy = Math.max(0, ys), sx = Math.max(0, -xs), sy = Math.max(0, -ys);
        int xMax = Math.min(w - dx, i.w - sx), yMax = Math.min(h - dy, i.h - sy);
        if (xMax <= 0 || yMax <= 0) return;
        for (int y = 0; y < yMax; y++) System.arraycopy(i.data, (y + sy) * i.w + sx, data, (y + dy) * w + dx, xMax);
    }

    public void fill(int color) {
        java.util.Arrays.fill(data, color);
    }
}
