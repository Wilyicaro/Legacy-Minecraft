package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteResourceAlbums {
    private static final Set<String> CATEGORY_IDS = Set.of("texture_packs", "mashup_packs");
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> IMAGES = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_IMAGES = ConcurrentHashMap.newKeySet();
    private static final Map<String, CompletableFuture<PackAlbum>> INSTALLS = new ConcurrentHashMap<>();
    private static CompletableFuture<Void> loadFuture;

    private RemoteResourceAlbums() {
    }

    private record Entry(ContentManager.Category category, ContentManager.Pack pack, PackAlbum placeholder) {
    }

    public static synchronized CompletableFuture<Void> load() {
        if (loadFuture != null) return loadFuture;
        List<ContentManager.Category> categories = ContentManager.CATEGORIES.stream().filter(category -> CATEGORY_IDS.contains(category.id())).toList();
        if (categories.isEmpty()) return CompletableFuture.completedFuture(null);
        List<CompletableFuture<List<ContentManager.Pack>>> futures = categories.stream().map(ContentManager::fetchIndex).toList();
        CompletableFuture<Void> result = new CompletableFuture<>();
        loadFuture = result;
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }
            List<Entry> loaded = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                ContentManager.Category category = categories.get(i);
                for (ContentManager.Pack pack : futures.get(i).getNow(List.of())) {
                    if (pack.activeDownloadURI().isEmpty() || pack.hasResourceAlbum()) continue;
                    loaded.add(new Entry(category, pack, createPlaceholder(pack)));
                }
            }
            Runnable register = () -> {
                synchronized (RemoteResourceAlbums.class) {
                    ENTRIES.clear();
                    for (Entry entry : loaded) ENTRIES.put(entry.placeholder().id(), entry);
                    if (loaded.isEmpty()) loadFuture = null;
                }
                for (Entry entry : loaded) requestImage(iconUrl(entry));
                result.complete(null);
            };
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) register.run();
            else minecraft.execute(register);
        });
        return result;
    }

    public static synchronized ListMap<String, PackAlbum> createAlbumMap() {
        ListMap<String, PackAlbum> albums = new ListMap<>();
        for (PackAlbum album : PackAlbum.resourceAlbums.values()) albums.put(album.id(), album);
        addTo(albums);
        return albums;
    }

    public static synchronized void addTo(ListMap<String, PackAlbum> albums) {
        List<PackAlbum> defaultAlbums = new ArrayList<>(albums.values()).stream().filter(album -> !ENTRIES.containsKey(album.id()) && PackAlbum.DEFAULT_RESOURCE_ALBUMS.contains(album)).toList();
        List<PackAlbum> managedAlbums = new ArrayList<>(albums.values()).stream().filter(album -> !ENTRIES.containsKey(album.id()) && !PackAlbum.DEFAULT_RESOURCE_ALBUMS.contains(album) && DownloadedResourceAlbums.isManagedAlbum(album.id())).toList();
        List<PackAlbum> userAlbums = new ArrayList<>(albums.values()).stream().filter(album -> !ENTRIES.containsKey(album.id()) && !PackAlbum.DEFAULT_RESOURCE_ALBUMS.contains(album) && !DownloadedResourceAlbums.isManagedAlbum(album.id())).toList();
        albums.clear();
        for (PackAlbum album : defaultAlbums) albums.put(album.id(), album);
        for (Entry entry : ENTRIES.values()) {
            PackAlbum installed = PackAlbum.resourceAlbums.get(entry.placeholder().id());
            PackAlbum album = installed == null ? entry.placeholder() : installed;
            albums.put(album.id(), album);
        }
        for (PackAlbum album : managedAlbums) albums.put(album.id(), album);
        for (PackAlbum album : userAlbums) albums.put(album.id(), album);
    }

    public static synchronized boolean isPlaceholder(PackAlbum album) {
        if (album == null) return false;
        Entry entry = ENTRIES.get(album.id());
        return entry != null && entry.placeholder() == album;
    }

    public static ResourceLocation getIcon(PackAlbum album) {
        Entry entry = getEntry(album);
        Optional<URI> iconUrl = iconUrl(entry);
        if (iconUrl.isEmpty()) return null;
        String key = iconUrl.get().toString();
        ResourceLocation image = IMAGES.get(key);
        if (image == null) {
            requestImage(iconUrl);
            return null;
        }
        return PackAlbum.Selector.DEFAULT_ICON.equals(image) ? null : image;
    }

    public static boolean isIconPending(PackAlbum album) {
        Entry entry = getEntry(album);
        return iconUrl(entry).map(URI::toString).filter(PENDING_IMAGES::contains).isPresent();
    }

    public static synchronized Optional<CompletableFuture<PackAlbum>> install(PackAlbum album) {
        Entry entry = getEntry(album);
        if (entry == null) return Optional.empty();
        CompletableFuture<PackAlbum> existing = INSTALLS.get(album.id());
        if (existing != null) return Optional.of(existing);
        CompletableFuture<PackAlbum> future = startInstall(entry);
        INSTALLS.put(album.id(), future);
        future.whenComplete((installed, throwable) -> INSTALLS.remove(album.id(), future));
        return Optional.of(future);
    }

    private static CompletableFuture<PackAlbum> startInstall(Entry entry) {
        CompletableFuture<PackAlbum> result = new CompletableFuture<>();
        try {
            ContentManager.prepareDownloadTarget(entry.pack(), entry.category());
        } catch (IOException e) {
            result.completeExceptionally(e);
            return result;
        }
        ContentManager.downloadPack(entry.pack(), entry.category(), installedAnything -> {
            try {
                if (!ContentManager.isPackInstalled(entry.pack(), entry.category())) {
                    throw new IOException("Failed to install " + entry.pack().name());
                }
                PackAlbum installed = PackAlbum.resourceAlbums.get(entry.placeholder().id());
                if (installed == null || installed == entry.placeholder()) {
                    DownloadedResourceAlbums.sync(entry.pack());
                    installed = PackAlbum.resourceAlbums.get(entry.placeholder().id());
                }
                if (installed == null || installed == entry.placeholder()) {
                    throw new IOException("Failed to create resource album for " + entry.pack().name());
                }
                result.complete(installed);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private static synchronized Entry getEntry(PackAlbum album) {
        if (album == null) return null;
        Entry entry = ENTRIES.get(album.id());
        return entry != null && entry.placeholder() == album ? entry : null;
    }

    private static Optional<URI> iconUrl(Entry entry) {
        if (entry == null) return Optional.empty();
        return entry.pack().resourceAlbumIconUrl().map(uri -> uri.isAbsolute() ? uri : URI.create(entry.category().indexUrl()).resolve(uri));
    }

    private static PackAlbum createPlaceholder(ContentManager.Pack pack) {
        String packId = normalize(pack.id());
        String filePackId = "file/" + packId;
        List<String> packs = new ArrayList<>(PackAlbum.MINECRAFT.packs());
        packs.remove(packId);
        packs.remove(filePackId);
        packs.add(filePackId);
        return new PackAlbum(
            DownloadedResourceAlbums.albumId(packId),
            0,
            pack.nameComponent(),
            pack.descriptionComponent(),
            Optional.empty(),
            Optional.empty(),
            packs,
            Optional.of(filePackId)
        );
    }

    private static void requestImage(Optional<URI> imageUrl) {
        if (imageUrl.isEmpty()) return;
        String key = imageUrl.get().toString();
        if (IMAGES.containsKey(key) || !PENDING_IMAGES.add(key)) return;
        CompletableFuture.runAsync(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            try (InputStream stream = ContentManager.openRemoteStream(imageUrl.get().toURL(), 5000, 10000)) {
                NativeImage image = NativeImage.read(stream);
                minecraft.execute(() -> {
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("legacy", "remote_resource_album/" + Integer.toHexString(key.hashCode()));
                    minecraft.getTextureManager().register(location, new DynamicTexture(location::toString, image));
                    IMAGES.put(key, location);
                    PENDING_IMAGES.remove(key);
                });
            } catch (Exception e) {
                Legacy4J.LOGGER.warn("Failed to load remote resource album image {}", key, e);
                minecraft.execute(() -> {
                    IMAGES.put(key, PackAlbum.Selector.DEFAULT_ICON);
                    PENDING_IMAGES.remove(key);
                });
            }
        });
    }

    private static String normalize(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }
}
