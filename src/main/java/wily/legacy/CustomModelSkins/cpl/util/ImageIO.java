package wily.legacy.CustomModelSkins.cpl.util;

import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftClientAccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class ImageIO {
    private static IImageIO api;

    public static IImageIO getApi() {
        IImageIO a = api;
        if (a == null) api = a = MinecraftClientAccess.get().getImageIO();
        return a;
    }

    public static CompletableFuture<Image> read(File f) {
        return getApi().readF(f);
    }

    public static Image read(InputStream f) throws IOException {
        return getApi().read(f);
    }

    public static void write(Image img, File f) throws IOException {
        getApi().write(img, f);
    }

    public static void write(Image img, OutputStream f) throws IOException {
        getApi().write(img, f);
    }

    public static Vec2i getSize(InputStream din) throws IOException {
        return getApi().getSize(din);
    }

    public static boolean isAvailable() {
        return api != null || MinecraftClientAccess.get() != null;
    }

    public static interface IImageIO {
        default CompletableFuture<Image> readF(File f) {
            try {
                return CompletableFuture.completedFuture(read(f));
            } catch (IOException e) {
                CompletableFuture<Image> cf = new CompletableFuture<>();
                cf.completeExceptionally(e);
                return cf;
            }
        }

        Image read(File f) throws IOException;

        Image read(InputStream f) throws IOException;

        void write(Image img, File f) throws IOException;

        void write(Image img, OutputStream f) throws IOException;

        Vec2i getSize(InputStream din) throws IOException;
    }
}
