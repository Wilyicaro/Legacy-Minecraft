package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import wily.legacy.Legacy4J;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class DownloadedResourceAlbums {
    private static final String PREFIX = "downloaded_";

    private DownloadedResourceAlbums() {
    }

    public static String albumId(String packId) {
        return PREFIX + normalize(packId);
    }

    public static boolean isManagedAlbum(String albumId) {
        return albumId != null && albumId.startsWith(PREFIX);
    }

    public static boolean isManagedPack(String packId) {
        return DownloadedPackMetadata.exists(packId);
    }

    public static void sync(ContentManager.Pack pack) {
        saveIfChanged(sync(normalize(pack.id()), pack.name(), pack.description()));
    }

    public static void syncAll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        boolean changed = pruneMissingAlbums();
        Path dir = minecraft.getResourcePackDirectory();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.list(dir)) {
                for (Path path : (Iterable<Path>) paths.filter(Files::isDirectory)::iterator) {
                    String packId = path.getFileName().toString();
                    if (!DownloadedPackMetadata.exists(packId)) continue;
                    changed |= sync(packId);
                }
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to sync downloaded resource albums", e);
            }
        }
        saveIfChanged(changed);
    }

    public static void remove(String packId) {
        String albumId = albumId(packId);
        if (PackAlbum.resourceAlbums.remove(albumId) == null) return;
        resetDefault(albumId);
        PackAlbum.save();
    }

    private static boolean sync(String packId) {
        DownloadedPackMetadata.Entry entry = DownloadedPackMetadata.entry(packId);
        return sync(packId, entry.name() == null || entry.name().isBlank() ? packId : entry.name(), entry.description() == null ? "" : entry.description());
    }

    private static boolean sync(String packId, String name, String description) {
        String displayPack = resolvePackId(packId);
        List<String> packs = new ArrayList<>(PackAlbum.MINECRAFT.packs());
        packs.remove(displayPack);
        packs.add(displayPack);
        PackAlbum album = new PackAlbum(albumId(packId), 0, Component.literal(name), description.isBlank() ? Component.empty() : Component.literal(description), Optional.empty(), Optional.empty(), packs, Optional.of(displayPack));
        PackAlbum oldAlbum = PackAlbum.resourceAlbums.get(album.id());
        if (same(oldAlbum, album)) return false;
        PackAlbum.resourceAlbums.put(album.id(), album);
        return true;
    }

    private static boolean pruneMissingAlbums() {
        boolean changed = false;
        for (PackAlbum album : new ArrayList<>(PackAlbum.resourceAlbums.values())) {
            if (!isManagedAlbum(album.id())) continue;
            if (DownloadedPackMetadata.exists(packId(album.id()))) continue;
            PackAlbum.resourceAlbums.remove(album.id());
            resetDefault(album.id());
            changed = true;
        }
        return changed;
    }

    private static String resolvePackId(String packId) {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        String fileId = filePackId(packId);
        if (repository != null) {
            if (repository.getPack(fileId) != null) return fileId;
            if (repository.getPack(packId) != null) return packId;
        }
        return fileId;
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
