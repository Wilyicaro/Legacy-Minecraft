package wily.legacy.CustomModelSkins.cpm.shared.skin;

import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftObjectHolder;
import wily.legacy.CustomModelSkins.cpm.shared.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public abstract class PlayerTextureLoader {
    private final Map<TextureType, Texture> textures = new ConcurrentHashMap<>();
    private final File skinsDirectory;
    private CompletableFuture<Void> loadFuture;

    private static class Texture {
        private final TextureType type;
        private CompletableFuture<Image> future;
        private String url;
        private File cachedFile;
        private UnaryOperator<Image> postProcessor = UnaryOperator.identity();

        private Texture(TextureType type) {
            this.type = type;
        }

        private CompletableFuture<Image> get() {
            if (future != null) return future;
            future = load0();
            return future == null ? CompletableFuture.completedFuture(null) : future;
        }

        private CompletableFuture<Image> load0() {
            File dbg = new File(type.name().toLowerCase(Locale.ROOT) + "_test.png");
            if (MinecraftObjectHolder.DEBUGGING && dbg.exists()) {
                Log.info("[ConsoleSkins] Using texture file: " + dbg.getAbsolutePath());
                return Image.loadFrom(dbg).thenApply(postProcessor);
            }
            if (cachedFile != null && cachedFile.isFile()) {
                Log.info("[ConsoleSkins] Using texture cache file: " + cachedFile.getAbsolutePath());
                return Image.loadFrom(cachedFile).thenApply(postProcessor).exceptionally(e -> null);
            }
            return url == null ? null : CompletableFuture.completedFuture(null);
        }
    }

    protected PlayerTextureLoader(File skinsDirectory) {
        this.skinsDirectory = skinsDirectory;
    }

    protected abstract CompletableFuture<Void> load0();

    public CompletableFuture<Void> load() {
        if (loadFuture == null) loadFuture = load0();
        return loadFuture;
    }

    protected void defineTexture(TextureType type, String url) {
        textures.computeIfAbsent(type, Texture::new).url = url;
    }

    protected void defineTexture(TextureType type, String url, String hash) {
        Texture tx = textures.computeIfAbsent(type, Texture::new);
        tx.url = url;
        if (skinsDirectory != null && hash != null) {
            String safe = Integer.toHexString(hash.hashCode());
            File dir = new File(skinsDirectory, safe.length() > 2 ? safe.substring(0, 2) : "xx");
            tx.cachedFile = new File(dir, safe);
        }
    }

    public CompletableFuture<Image> getTexture(TextureType type) {
        return textures.computeIfAbsent(type, Texture::new).get();
    }

    public void cleanup() {
        textures.clear();
        loadFuture = null;
    }

    public boolean hasTexture(TextureType type) {
        Texture t = textures.get(type);
        return t != null && t.url != null;
    }
}
