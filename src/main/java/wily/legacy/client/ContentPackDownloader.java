package wily.legacy.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import wily.legacy.Legacy4J;
import wily.legacy.client.ContentManager.Category;
import wily.legacy.client.ContentManager.Pack;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.DownloadedSkinPackStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ContentPackDownloader {
    private static final java.util.Set<String> ACTIVE_DOWNLOADS = ConcurrentHashMap.newKeySet();
    private static final String TRANSPARENCY_FIX_URL = "https://cdn.modrinth.com/data/MK3k9U5o/versions/BdYLypGY/Transparency%20Fix.zip";
    private static final String DOWNLOAD_KEY_FILE = ".legacy4j_download_key";

    private ContentPackDownloader() {
    }

    static boolean isDownloading(Pack pack, Category category) {
        return ACTIVE_DOWNLOADS.contains(downloadKey(category, pack));
    }

    static boolean isInstalled(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            boolean standaloneInstalled = !hasStandaloneContent(pack) || isStandalonePackInstalled(pack, category);
            return standaloneInstalled && areBundleChildren(pack, ContentPackDownloader::isInstalled) && (!pack.hasResourceAlbum() || DownloadedResourceAlbums.hasManagedBundleAlbum(pack));
        }
        return isStandalonePackInstalled(pack, category);
    }

    static boolean needsUpdate(Pack pack, Category category) {
        return hasInstalledContent(pack, category) && !isInstalled(pack, category);
    }

    static CompletableFuture<Boolean> download(Pack pack, Category category) {
        return pack.hasBundlePacks() ? downloadBundlePack(pack, category) : downloadSinglePack(pack, category);
    }

    static void delete(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            if (hasStandaloneContent(pack)) deleteStandalonePack(pack, category);
            DownloadedResourceAlbums.removeBundle(pack);
            for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
                ContentManager.getCategory(bundlePack.categoryId()).ifPresent(bundleCategory -> delete(bundlePack.toPack(), bundleCategory));
            }
            return;
        }
        deleteStandalonePack(pack, category);
    }

    private static boolean shouldApplyTransparencyFix(Category category) {
        return "resourcepacks".equals(category.targetDirectoryName()) && ("texture_packs".equals(category.id()) || "mashup_packs".equals(category.id()));
    }

    private static void applyTransparencyFix(Path targetFolder) throws IOException {
        Path fixTempFile = downloadTemp(new URL(TRANSPARENCY_FIX_URL), "legacy_pack_fix_");
        try {
            extractZip(fixTempFile, targetFolder);
        } finally {
            Files.deleteIfExists(fixTempFile);
        }
    }

    private static Path downloadTemp(URL url, String prefix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, ".zip");
        try (InputStream stream = ContentManager.openRemoteStream(url, 5000, 10000)) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private static Optional<byte[]> readOptionalFileBytes(Path path) throws IOException {
        return Files.isRegularFile(path) ? Optional.of(Files.readAllBytes(path)) : Optional.empty();
    }

    private static void restoreOptionalFileBytes(Path path, Optional<byte[]> bytes) throws IOException {
        if (bytes.isPresent()) {
            Files.write(path, bytes.get());
        } else {
            Files.deleteIfExists(path);
        }
    }

    private static Optional<String> readOptionalFileText(Path path) throws IOException {
        return Files.isRegularFile(path) ? Optional.of(Files.readString(path)) : Optional.empty();
    }

    private static void mergeTransparencyFixPackMeta(Path targetFolder, Optional<String> originalPackMeta) throws IOException {
        if (originalPackMeta.isEmpty()) return;
        Path packMetaPath = targetFolder.resolve("pack.mcmeta");
        JsonObject original = JsonParser.parseString(originalPackMeta.get()).getAsJsonObject();
        if (Files.isRegularFile(packMetaPath)) {
            JsonObject fixed = JsonParser.parseString(Files.readString(packMetaPath)).getAsJsonObject();
            if (fixed.has("overlays")) original.add("overlays", fixed.get("overlays").deepCopy());
        }
        Files.writeString(packMetaPath, original.toString());
    }

    private static String downloadKey(Category category, Pack pack) {
        return category.id() + ":" + pack.id();
    }

    private static boolean isStandalonePackInstalled(Pack pack, Category category) {
        String folderName = category.targetDirectoryName();
        Path path = ContentManager.getContentDir(folderName).resolve(pack.id());
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;
        if ((DownloadedSkinPackStore.managesTargetDirectory(folderName) || CustomSkinPackStore.managesTargetDirectory(folderName)) && !DownloadedSkinPackStore.isValidPackInstall(path)) return false;
        if (pack.hasWorldTemplate() && !LegacyWorldTemplate.isDownloadedPackInstalled(pack)) return false;
        if (!pack.downloadVariants().isEmpty() && !pack.activeDownloadKey().equals(readDownloadKey(path))) return false;

        if (pack.activeCheckSum().isPresent()) {
            Path checksumFile = path.resolve(".md5");
            if (Files.exists(checksumFile)) {
                try {
                    String existingChecksum = Files.readString(checksumFile).trim();
                    return pack.activeCheckSum().get().equals(existingChecksum);
                } catch (IOException e) {
                    return false;
                }
            }
            return false;
        }

        return true;
    }

    private static boolean hasInstalledContent(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            return hasStandalonePackFiles(pack, category)
                || pack.hasResourceAlbum() && DownloadedResourceAlbums.hasManagedBundleAlbum(pack)
                || areBundleChildren(pack, ContentPackDownloader::hasInstalledContent);
        }
        return hasStandalonePackFiles(pack, category);
    }

    private static boolean hasStandalonePackFiles(Pack pack, Category category) {
        Path path = ContentManager.getContentDir(category.targetDirectoryName()).resolve(pack.id());
        return hasStandaloneContent(pack) && Files.isDirectory(path);
    }

    private static boolean hasStandaloneContent(Pack pack) {
        return pack.activeDownloadURI().isPresent();
    }

    private static String readDownloadKey(Path packDir) {
        Path path = packDir.resolve(DOWNLOAD_KEY_FILE);
        if (!Files.isRegularFile(path)) return "default";
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "default";
        }
    }

    private static void writeDownloadKey(Path packDir, Pack pack) throws IOException {
        Path path = packDir.resolve(DOWNLOAD_KEY_FILE);
        if (pack.downloadVariants().isEmpty()) {
            Files.deleteIfExists(path);
            return;
        }
        Files.writeString(path, pack.activeDownloadKey(), StandardCharsets.UTF_8);
    }

    private static CompletableFuture<Boolean> downloadBundlePack(Pack pack, Category category) {
        List<Pack.BundlePack> bundlePacks = pack.bundlePacks();
        if (bundlePacks.isEmpty()) return CompletableFuture.completedFuture(false);
        ACTIVE_DOWNLOADS.add(downloadKey(category, pack));
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        if (hasStandaloneContent(pack)) futures.add(isStandalonePackInstalled(pack, category) ? CompletableFuture.completedFuture(false) : downloadPackFiles(pack, category, !pack.hasResourceAlbum(), false));
        futures.addAll(bundlePacks.stream().map(bundlePack -> {
            Optional<Category> bundleCategory = ContentManager.getCategory(bundlePack.categoryId());
            if (bundleCategory.isEmpty()) {
                Legacy4J.LOGGER.warn("Unknown bundle pack category {} while installing {}", bundlePack.categoryId(), pack.id());
                return CompletableFuture.completedFuture(false);
            }
            return downloadSinglePack(bundlePack.toPack(), bundleCategory.get(), !pack.hasResourceAlbum());
        }).toList());
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
            boolean installedAnything = false;
            for (CompletableFuture<Boolean> future : futures) {
                installedAnything |= future.getNow(false);
            }
            if (pack.hasResourceAlbum()) {
                boolean standaloneInstalled = !hasStandaloneContent(pack) || isStandalonePackInstalled(pack, category);
                if (standaloneInstalled && areBundleChildren(pack, ContentPackDownloader::isInstalled)) {
                    installedAnything |= DownloadedResourceAlbums.syncBundle(pack);
                } else {
                    DownloadedResourceAlbums.removeBundle(pack);
                }
            }
            return installedAnything;
        }).whenComplete((result, throwable) -> ACTIVE_DOWNLOADS.remove(downloadKey(category, pack)));
    }

    private static CompletableFuture<Boolean> downloadSinglePack(Pack pack, Category category) {
        return downloadSinglePack(pack, category, true);
    }

    private static CompletableFuture<Boolean> downloadSinglePack(Pack pack, Category category, boolean syncResourceAlbum) {
        if (isInstalled(pack, category)) return CompletableFuture.completedFuture(false);
        return downloadPackFiles(pack, category, syncResourceAlbum, true);
    }

    private static CompletableFuture<Boolean> downloadPackFiles(Pack pack, Category category, boolean syncResourceAlbum, boolean trackActiveDownload) {
        String folderName = category.targetDirectoryName();
        Path contentDir = ContentManager.getContentDir(folderName);
        if (trackActiveDownload) ACTIVE_DOWNLOADS.add(downloadKey(category, pack));
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<URI> downloadURI = pack.activeDownloadURI();
                if (downloadURI.isEmpty()) {
                    Legacy4J.LOGGER.warn("Pack {} has no download URI.", pack.id());
                    return false;
                }
                Path downloadedTempFile = downloadTemp(downloadURI.get().toURL(), "legacy_pack_");

                Optional<String> checkSum = pack.activeCheckSum();
                if (checkSum.isPresent()) {
                    String fileHash = ContentManager.readFileCheckSum(downloadedTempFile);
                    if (!checkSum.get().equals(fileHash)) {
                        Legacy4J.LOGGER.warn("Checksum mismatch for pack {}. Expected {}, got {}", pack.id(), checkSum.get(), fileHash);
                        Files.deleteIfExists(downloadedTempFile);
                        return false;
                    }
                }

                Path targetFolder = contentDir.resolve(pack.id());
                if (Files.exists(targetFolder)) deleteDirectoryRecursively(targetFolder.toFile());
                Files.createDirectories(targetFolder);

                extractZip(downloadedTempFile, targetFolder);
                if (shouldApplyTransparencyFix(category)) {
                    Optional<byte[]> originalPackIcon = readOptionalFileBytes(targetFolder.resolve("pack.png"));
                    Optional<String> originalPackMeta = readOptionalFileText(targetFolder.resolve("pack.mcmeta"));
                    applyTransparencyFix(targetFolder);
                    restoreOptionalFileBytes(targetFolder.resolve("pack.png"), originalPackIcon);
                    mergeTransparencyFixPackMeta(targetFolder, originalPackMeta);
                }
                boolean rootResourcePack = ContentManager.isRootResourcePackDirectory(contentDir);
                if (rootResourcePack) {
                    DownloadedPackMetadata.write(targetFolder, pack, category);
                }
                if (DownloadedSkinPackStore.managesTargetDirectory(folderName)) {
                    DownloadedSkinPackStore.normalizeInstalledPack(targetFolder);
                } else if (CustomSkinPackStore.managesTargetDirectory(folderName)) {
                    CustomSkinPackStore.normalizeDownloadedPack(targetFolder);
                } else if (rootResourcePack) {
                    if (syncResourceAlbum && category.useResourceAlbum()) {
                        DownloadedResourceAlbums.sync(pack);
                    } else {
                        DownloadedResourceAlbums.remove(pack.id());
                    }
                }
                LegacyWorldTemplate.downloadDownloadedPack(pack);
                Files.deleteIfExists(downloadedTempFile);

                writeDownloadKey(targetFolder, pack);
                if (checkSum.isPresent()) {
                    Files.writeString(targetFolder.resolve(".md5"), checkSum.get());
                }

                PackAlbum.Selector.invalidatePackAssets(pack.id());
                Minecraft.getInstance().execute(LegacyWorldTemplate::refreshDownloadedPacks);
                return true;
            } catch (Exception e) {
                deleteStandalonePack(pack, category);
                Legacy4J.LOGGER.warn("Error when downloading content pack to {}: {}", contentDir.resolve(pack.id()), e.getMessage());
                return false;
            }
        }).whenComplete((result, throwable) -> {
            if (trackActiveDownload) ACTIVE_DOWNLOADS.remove(downloadKey(category, pack));
        });
    }

    private static void deleteStandalonePack(Pack pack, Category category) {
        Path packDir = ContentManager.getContentDir(category.targetDirectoryName()).resolve(pack.id());
        if (Files.exists(packDir)) deleteDirectoryRecursively(packDir.toFile());
        DownloadedPackMetadata.clear(pack.id());
        DownloadedResourceAlbums.remove(pack.id());
        PackAlbum.Selector.invalidatePackAssets(pack.id());
        LegacyWorldTemplate.removeDownloadedPack(pack.id());
        LegacyWorldTemplate.refreshDownloadedPacks();
    }

    private static boolean areBundleChildren(Pack pack, BiPredicate<Pack, Category> check) {
        for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
            Optional<Category> bundleCategory = ContentManager.getCategory(bundlePack.categoryId());
            if (bundleCategory.isEmpty() || !check.test(bundlePack.toPack(), bundleCategory.get())) return false;
        }
        return true;
    }

    private static void deleteDirectoryRecursively(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        directory.delete();
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            String rootPrefix = detectRootPrefix(zip);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String normalizedName = normalizeZipEntryName(entry.getName());
                if (rootPrefix != null && normalizedName.startsWith(rootPrefix)) normalizedName = normalizedName.substring(rootPrefix.length());
                if (normalizedName.isEmpty()) continue;
                Path entryPath = targetDir.resolve(normalizedName);

                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry is outside of the target directory: " + normalizedName);
                }

                if (entry.isDirectory() || normalizedName.endsWith("/")) {
                    Files.createDirectories(entryPath);
                } else {
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static String detectRootPrefix(ZipFile zip) {
        String root = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            String name = normalizeZipEntryName(entries.nextElement().getName());
            if (name.isEmpty()) continue;
            int split = name.indexOf('/');
            if (split < 0) return null;
            String candidate = name.substring(0, split);
            if (candidate.isEmpty()) return null;
            if (root == null) root = candidate;
            else if (!root.equals(candidate)) return null;
        }
        return root == null ? null : root + "/";
    }

    private static String normalizeZipEntryName(String name) {
        if (name == null) return "";
        String normalized = name.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }
}
