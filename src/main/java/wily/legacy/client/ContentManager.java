package wily.legacy.client;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Skins.skin.DownloadedSkinPackStore;
import wily.legacy.util.IOUtil;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ContentManager {

    public static final List<Category> CATEGORIES = new ArrayList<>();
    private static final String CATEGORIES_FILE = "store_categories.json";

    public record Category(
        String id,
        Component title,
        String indexUrl,
        String targetDirectoryName,
        boolean requiresResourceReload
    ) {
        public static final Codec<Category> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Category::id),
            DynamicUtil.getComponentCodec().fieldOf("title").forGetter(Category::title),
            Codec.STRING.fieldOf("indexUrl").forGetter(Category::indexUrl),
            Codec.STRING.fieldOf("targetDirectoryName").forGetter(Category::targetDirectoryName),
            Codec.BOOL.optionalFieldOf("requiresResourceReload", false).forGetter(Category::requiresResourceReload)
        ).apply(i, Category::new));

        public static final Codec<List<Category>> LIST_CODEC = CODEC.listOf();
    }

    public record Pack(
        String id,
        String name,
        String description,
        URI downloadURI,
        Optional<URI> imageUrl,
        Optional<String> checkSum
    ) {
        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Pack::id),
            Codec.STRING.fieldOf("name").forGetter(Pack::name),
            Codec.STRING.optionalFieldOf("description", "").forGetter(Pack::description),
            Codec.STRING.xmap(URI::create, URI::toString).fieldOf("downloadURI").forGetter(Pack::downloadURI),
            Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("imageUrl").forGetter(Pack::imageUrl)
        ).apply(i, Pack::create));

        public static final Codec<List<Pack>> LIST_CODEC = CODEC.listOf();

        public static Pack create(String id, String name, String description, URI compoundDownloadURI, Optional<URI> imageUrl) {
            String[] splitURI = compoundDownloadURI.toString().split("\\?checksum=");
            URI downloadURI = URI.create(splitURI[0]);
            Optional<String> checkSum = splitURI.length < 2 ? Optional.empty() : Optional.of(splitURI[1]);
            return new Pack(id, name, description, downloadURI, imageUrl, checkSum);
        }
    }

    public static CompletableFuture<List<Pack>> fetchIndex(String indexUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader reader = new InputStreamReader(new URL(indexUrl).openStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return Pack.LIST_CODEC.parse(JsonOps.INSTANCE, json.get("packs"))
                        .resultOrPartial(Legacy4J.LOGGER::warn)
                        .orElseGet(ArrayList::new);
            } catch (Exception e) {
                Legacy4J.LOGGER.warn("Failed to fetch content index from {}: {}", indexUrl, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public static class CategoryManager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            CATEGORIES.clear();
            IOUtil.getOrderedNamespaces(resourceManager).forEach(name -> {
                resourceManager.getResource(FactoryAPI.createLocation(name, CATEGORIES_FILE)).ifPresent(r -> {
                    try (BufferedReader bufferedReader = r.openAsReader()) {
                        Category.LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader))
                                .resultOrPartial(Legacy4J.LOGGER::warn)
                                .ifPresent(CATEGORIES::addAll);
                    } catch (IOException e) {
                        Legacy4J.LOGGER.warn("Failed to load store categories from namespace {}: {}", name, e.getMessage());
                    }
                });
            });
        }

        @Override
        public String getName() {
            return "legacy:store_categories";
        }
    }

    public static Path getContentDir(String folderName) {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(folderName);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to create content directory: {}", e.getMessage());
            }
        }
        return dir;
    }

    public static String readFileCheckSum(Path path) {
        try {
            ByteSource byteSource = com.google.common.io.Files.asByteSource(path.toFile());
            return byteSource.hash(Hashing.md5()).toString();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading existing checksum from {}: {}", path, e.getMessage());
            return null;
        }
    }

    public static boolean isPackInstalled(Pack pack, String folderName) {
        Path path = getContentDir(folderName).resolve(pack.id());
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;
        if (DownloadedSkinPackStore.managesTargetDirectory(folderName) && !DownloadedSkinPackStore.isValidPackInstall(path)) return false;

        if (pack.checkSum().isPresent()) {
            Path checksumFile = path.resolve(".md5");
            if (Files.exists(checksumFile)) {
                try {
                    String existingChecksum = Files.readString(checksumFile).trim();
                    return pack.checkSum().get().equals(existingChecksum);
                } catch (IOException e) {
                    return false;
                }
            }
            return false; 
        }

        return true;
    }

    public static void downloadPack(Pack pack, String folderName, Runnable onFinished) {
        if (isPackInstalled(pack, folderName)) {
            Minecraft.getInstance().execute(onFinished);
            return;
        }

        Path contentDir = getContentDir(folderName);
        CompletableFuture.runAsync(() -> {
            try {
                Path downloadedTempFile = Files.createTempFile("legacy_pack_", ".zip");
                
                try (InputStream stream = pack.downloadURI().toURL().openStream()) {
                    Files.copy(stream, downloadedTempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (pack.checkSum().isPresent()) {
                    String fileHash = readFileCheckSum(downloadedTempFile);
                    if (!pack.checkSum().get().equals(fileHash)) {
                        Legacy4J.LOGGER.warn("Checksum mismatch for pack {}. Expected {}, got {}", pack.id(), pack.checkSum().get(), fileHash);
                        Files.deleteIfExists(downloadedTempFile);
                        return; 
                    }
                }

                Path targetFolder = contentDir.resolve(pack.id());
                if (Files.exists(targetFolder)) {
                    deleteDirectoryRecursively(targetFolder.toFile());
                }
                Files.createDirectories(targetFolder);

                extractZip(downloadedTempFile, targetFolder);
                if (DownloadedSkinPackStore.managesTargetDirectory(folderName)) {
                    DownloadedSkinPackStore.normalizeInstalledPack(targetFolder);
                }
                Files.deleteIfExists(downloadedTempFile);

                if (pack.checkSum().isPresent()) {
                    Files.writeString(targetFolder.resolve(".md5"), pack.checkSum().get());
                }

                Minecraft.getInstance().execute(onFinished);
            } catch (Exception e) {
                Legacy4J.LOGGER.warn("Error when downloading content pack to {}: {}", contentDir.resolve(pack.id()), e.getMessage());
            }
        });
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
