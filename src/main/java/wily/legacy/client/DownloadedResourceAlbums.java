package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DownloadedResourceAlbums {
    private static final String PREFIX = "downloaded_";
    private static final String BUNDLE_PREFIX = PREFIX + "bundle_";

    private DownloadedResourceAlbums() {
    }

    public static String albumId(String packId) {
        return PREFIX + normalize(packId);
    }

    public static boolean isManagedAlbum(String albumId) {
        return albumId != null && albumId.startsWith(PREFIX);
    }

    public static boolean isManagedPack(String packId) {
        return DownloadedPackMetadata.exists(packId) && DownloadedPackMetadata.entry(packId).useResourceAlbum();
    }

    public static void sync(ContentManager.Pack pack) {
        saveIfChanged(syncSingle(normalize(pack.id()), pack.name(), pack.description()));
    }

    public static boolean syncBundle(ContentManager.Pack pack) {
        if (!pack.hasResourceAlbum()) return false;
        ContentManager.Pack.ResourceAlbum resourceAlbum = pack.resourceAlbum().get();
        List<String> childPackIds = pack.bundlePacks().stream().map(bundlePack -> normalize(bundlePack.id())).toList();
        List<String> resolvedPacks = resolveBundlePacks(resourceAlbum.packs(), childPackIds);
        Optional<String> displayPack = resourceAlbum.displayPack()
            .map(packId -> resolveManagedPack(packId, childPackIds))
            .or(() -> resolvedPacks.isEmpty() ? Optional.empty() : Optional.of(resolvedPacks.get(resolvedPacks.size() - 1)));
        boolean changed = removeChildAlbums(childPackIds);
        changed |= putAlbum(new PackAlbum(
            bundleAlbumId(resourceAlbum.resolvedId(pack)),
            resourceAlbum.version(),
            Component.literal(resourceAlbum.resolvedName(pack)),
            resourceAlbum.resolvedDescription(pack).isBlank() ? Component.empty() : Component.literal(resourceAlbum.resolvedDescription(pack)),
            resourceAlbum.icon(),
            resourceAlbum.background(),
            resolvedPacks,
            displayPack
        ));
        saveIfChanged(changed);
        return changed;
    }

    public static boolean hasManagedBundleAlbum(ContentManager.Pack pack) {
        return pack.hasResourceAlbum() && PackAlbum.resourceAlbums.get(bundleAlbumId(pack.resourceAlbum().get().resolvedId(pack))) != null;
    }

    public static void syncAll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        Set<String> bundlePackIds = bundlePackIds();
        boolean changed = pruneMissingAlbums(bundlePackIds);
        if (minecraft.getResourcePackDirectory() != null && java.nio.file.Files.isDirectory(minecraft.getResourcePackDirectory())) {
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.list(minecraft.getResourcePackDirectory())) {
                for (java.nio.file.Path path : (Iterable<java.nio.file.Path>) paths.filter(java.nio.file.Files::isDirectory)::iterator) {
                    String packId = path.getFileName().toString();
                    if (!DownloadedPackMetadata.exists(packId) || bundlePackIds.contains(normalize(packId))) continue;
                    DownloadedPackMetadata.Entry entry = DownloadedPackMetadata.entry(packId);
                    if (!entry.useResourceAlbum()) {
                        changed |= removeAlbumState(albumId(packId));
                        continue;
                    }
                    changed |= syncSingle(packId, entry);
                }
            } catch (java.io.IOException ignored) {
            }
        }
        saveIfChanged(changed);
    }

    public static void remove(String packId) {
        removeAlbum(albumId(packId));
    }

    public static void removeBundle(ContentManager.Pack pack) {
        if (!pack.hasResourceAlbum()) return;
        removeAlbum(bundleAlbumId(pack.resourceAlbum().get().resolvedId(pack)));
    }

    private static boolean syncSingle(String packId) {
        return syncSingle(packId, DownloadedPackMetadata.entry(packId));
    }

    private static boolean syncSingle(String packId, DownloadedPackMetadata.Entry entry) {
        String name = entry.name() == null || entry.name().isBlank() ? packId : entry.name();
        String description = entry.description() == null ? "" : entry.description();
        return syncSingle(packId, name, description);
    }

    private static boolean syncSingle(String packId, String name, String description) {
        String displayPack = resolvePackId(packId);
        List<String> packs = new ArrayList<>(PackAlbum.MINECRAFT.packs());
        packs.remove(displayPack);
        packs.add(displayPack);
        return putAlbum(new PackAlbum(albumId(packId), 0, Component.literal(name), description.isBlank() ? Component.empty() : Component.literal(description), Optional.empty(), Optional.empty(), packs, Optional.of(displayPack)));
    }

    private static boolean pruneMissingAlbums(Set<String> bundlePackIds) {
        boolean changed = false;
        for (PackAlbum album : new ArrayList<>(PackAlbum.resourceAlbums.values())) {
            if (!isManagedAlbum(album.id())) continue;
            if (isManagedBundleAlbum(album.id())) {
                if (bundleChildPackIds(album).isEmpty() || bundleChildPackIds(album).stream().anyMatch(packId -> !DownloadedPackMetadata.exists(packId))) {
                    changed |= removeAlbumState(album.id());
                }
                continue;
            }
            String packId = packId(album.id());
            if (bundlePackIds.contains(packId)) {
                changed |= removeAlbumState(album.id());
                continue;
            }
            if (!DownloadedPackMetadata.exists(packId) || !DownloadedPackMetadata.entry(packId).useResourceAlbum()) changed |= removeAlbumState(album.id());
        }
        return changed;
    }

    private static List<String> resolveBundlePacks(List<String> packs, List<String> childPackIds) {
        List<String> source = packs.isEmpty() ? defaultBundlePacks(childPackIds) : packs;
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String packId : source) resolved.add(resolveManagedPack(packId, childPackIds));
        return new ArrayList<>(resolved);
    }

    private static List<String> defaultBundlePacks(List<String> childPackIds) {
        List<String> packs = new ArrayList<>(PackAlbum.MINECRAFT.packs());
        for (String childPackId : childPackIds) {
            String resolved = resolvePackId(childPackId);
            packs.remove(resolved);
            packs.add(resolved);
        }
        return packs;
    }

    private static String resolveManagedPack(String packId, List<String> childPackIds) {
        String normalized = normalize(packId);
        if (childPackIds.contains(normalized) || DownloadedPackMetadata.exists(normalized)) return resolvePackId(normalized);
        return packId;
    }

    private static String resolvePackId(String packId) {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        String normalized = normalize(packId);
        String fileId = filePackId(normalized);
        if (repository != null) {
            if (repository.getPack(fileId) != null) return fileId;
            if (repository.getPack(normalized) != null) return normalized;
            if (repository.getPack(packId) != null) return packId;
        }
        return packId.startsWith("file/") ? packId : fileId;
    }

    private static Set<String> bundlePackIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (PackAlbum album : PackAlbum.resourceAlbums.values()) {
            if (!isManagedBundleAlbum(album.id())) continue;
            ids.addAll(bundleChildPackIds(album));
        }
        return ids;
    }

    private static List<String> bundleChildPackIds(PackAlbum album) {
        List<String> ids = new ArrayList<>();
        for (String packId : album.packs()) {
            String normalized = normalize(packId);
            if (DownloadedPackMetadata.exists(normalized)) ids.add(normalized);
        }
        return ids;
    }

    private static boolean removeChildAlbums(List<String> childPackIds) {
        boolean changed = false;
        for (String childPackId : childPackIds) changed |= removeAlbumState(albumId(childPackId));
        return changed;
    }

    private static boolean putAlbum(PackAlbum album) {
        PackAlbum oldAlbum = PackAlbum.resourceAlbums.get(album.id());
        if (same(oldAlbum, album)) return false;
        PackAlbum.resourceAlbums.put(album.id(), album);
        return true;
    }

    private static boolean same(PackAlbum oldAlbum, PackAlbum newAlbum) {
        if (oldAlbum == null) return false;
        return oldAlbum.version() == newAlbum.version()
            && oldAlbum.displayName().equals(newAlbum.displayName())
            && oldAlbum.description().equals(newAlbum.description())
            && oldAlbum.iconSprite().equals(newAlbum.iconSprite())
            && oldAlbum.backgroundSprite().equals(newAlbum.backgroundSprite())
            && oldAlbum.packs().equals(newAlbum.packs())
            && oldAlbum.displayPack().equals(newAlbum.displayPack());
    }

    private static void removeAlbum(String albumId) {
        if (!removeAlbumState(albumId)) return;
        PackAlbum.save();
    }

    private static boolean removeAlbumState(String albumId) {
        if (PackAlbum.resourceAlbums.remove(albumId) == null) return false;
        resetDefault(albumId);
        return true;
    }

    private static String bundleAlbumId(String albumId) {
        return BUNDLE_PREFIX + normalize(albumId);
    }

    private static boolean isManagedBundleAlbum(String albumId) {
        return albumId != null && albumId.startsWith(BUNDLE_PREFIX);
    }

    private static String packId(String albumId) {
        return albumId.substring(PREFIX.length());
    }

    private static String filePackId(String packId) {
        return "file/" + packId;
    }

    private static void resetDefault(String albumId) {
        if (albumId.equals(PackAlbum.defaultResourceAlbum.get())) {
            PackAlbum.defaultResourceAlbum.set(PackAlbum.MINECRAFT.id());
        }
    }

    private static void saveIfChanged(boolean changed) {
        if (changed) PackAlbum.save();
    }

    private static String normalize(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }
}
