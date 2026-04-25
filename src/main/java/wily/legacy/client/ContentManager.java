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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.DownloadedSkinPackStore;
import wily.legacy.util.IOUtil;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ContentManager {

    public static final List<Category> CATEGORIES = new ArrayList<>();
    private static final String CATEGORIES_FILE = "store_categories.json";
    private static final String STARTERPACKS_CATEGORY_ID = "starterpacks";
    private static final String STARTERPACKS_PACK_ID = "starterpacks_bundle";
    private static final java.util.Set<String> ACTIVE_DOWNLOADS = ConcurrentHashMap.newKeySet();
    private static final String TRANSPARENCY_FIX_URL = "https://cdn.modrinth.com/data/MK3k9U5o/versions/BdYLypGY/Transparency%20Fix.zip";

    public record Category(
        String id,
        Component title,
        String indexUrl,
        String targetDirectoryName,
        boolean requiresResourceReload,
        boolean useResourceAlbum
    ) {
        public static final Codec<Category> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Category::id),
            DynamicUtil.getComponentCodec().fieldOf("title").forGetter(Category::title),
            Codec.STRING.fieldOf("indexUrl").forGetter(Category::indexUrl),
            Codec.STRING.fieldOf("targetDirectoryName").forGetter(Category::targetDirectoryName),
            Codec.BOOL.optionalFieldOf("requiresResourceReload", false).forGetter(Category::requiresResourceReload),
            Codec.BOOL.optionalFieldOf("useResourceAlbum", true).forGetter(Category::useResourceAlbum)
        ).apply(i, Category::new));

        public static final Codec<List<Category>> LIST_CODEC = CODEC.listOf();
    }

    public record Pack(
        String id,
        String name,
        String description,
        Optional<URI> downloadURI,
        Optional<URI> imageUrl,
        Optional<String> checkSum,
        Optional<URI> worldTemplateDownloadURI,
        Optional<String> worldTemplateCheckSum,
        Optional<String> worldTemplateFolderName,
        List<BundlePack> bundlePacks,
        Optional<ResourceAlbum> resourceAlbum
    ) {
        public record ResourceAlbum(
            Optional<String> id,
            int version,
            Optional<String> name,
            Optional<String> description,
            Optional<ResourceLocation> icon,
            Optional<ResourceLocation> background,
            List<String> packs,
            Optional<String> displayPack
        ) {
            public static final Codec<ResourceAlbum> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("id").forGetter(ResourceAlbum::id),
                Codec.INT.optionalFieldOf("version", 0).forGetter(ResourceAlbum::version),
                Codec.STRING.optionalFieldOf("name").forGetter(ResourceAlbum::name),
                Codec.STRING.optionalFieldOf("description").forGetter(ResourceAlbum::description),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ResourceAlbum::icon),
                ResourceLocation.CODEC.optionalFieldOf("background").forGetter(ResourceAlbum::background),
                Codec.STRING.listOf().optionalFieldOf("packs", List.of()).forGetter(ResourceAlbum::packs),
                Codec.STRING.optionalFieldOf("displayPack").forGetter(ResourceAlbum::displayPack)
            ).apply(i, ResourceAlbum::new));

            public String resolvedId(Pack pack) {
                return id.filter(s -> !s.isBlank()).orElse(pack.id());
            }

            public String resolvedName(Pack pack) {
                return name.filter(s -> !s.isBlank()).orElse(pack.name());
            }

            public String resolvedDescription(Pack pack) {
                return description.filter(s -> !s.isBlank()).orElse(pack.description());
            }
        }

        public record BundlePack(
            String categoryId,
            String id,
            String name,
            String description,
            Optional<URI> downloadURI,
            Optional<URI> imageUrl,
            Optional<String> checkSum,
            Optional<URI> worldTemplateDownloadURI,
            Optional<String> worldTemplateCheckSum,
            Optional<String> worldTemplateFolderName
        ) {
            public static final Codec<BundlePack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("categoryId").forGetter(BundlePack::categoryId),
                Codec.STRING.fieldOf("id").forGetter(BundlePack::id),
                Codec.STRING.fieldOf("name").forGetter(BundlePack::name),
                Codec.STRING.optionalFieldOf("description", "").forGetter(BundlePack::description),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("downloadURI").forGetter(BundlePack::downloadURI),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("imageUrl").forGetter(BundlePack::imageUrl),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("worldTemplateDownloadURI").forGetter(BundlePack::worldTemplateDownloadURI),
                Codec.STRING.optionalFieldOf("worldTemplateFolderName").forGetter(BundlePack::worldTemplateFolderName)
            ).apply(i, BundlePack::create));

            public static BundlePack create(String categoryId, String id, String name, String description, Optional<URI> compoundDownloadURI, Optional<URI> imageUrl, Optional<URI> compoundWorldTemplateDownloadURI, Optional<String> worldTemplateFolderName) {
                ParsedURI download = ParsedURI.of(compoundDownloadURI);
                ParsedURI worldTemplate = ParsedURI.of(compoundWorldTemplateDownloadURI);
                return new BundlePack(categoryId, id, name, description, download.uri(), imageUrl, download.checkSum(), worldTemplate.uri(), worldTemplate.checkSum(), worldTemplateFolderName);
            }

            public Pack toPack() {
                return new Pack(id, name, description, downloadURI, imageUrl, checkSum, worldTemplateDownloadURI, worldTemplateCheckSum, worldTemplateFolderName, List.of(), Optional.empty());
            }
        }

        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Pack::id),
            Codec.STRING.fieldOf("name").forGetter(Pack::name),
            Codec.STRING.optionalFieldOf("description", "").forGetter(Pack::description),
            Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("downloadURI").forGetter(Pack::downloadURI),
            Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("imageUrl").forGetter(Pack::imageUrl),
            Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("worldTemplateDownloadURI").forGetter(Pack::worldTemplateDownloadURI),
            Codec.STRING.optionalFieldOf("worldTemplateFolderName").forGetter(Pack::worldTemplateFolderName),
            BundlePack.CODEC.listOf().optionalFieldOf("bundlePacks", List.of()).forGetter(Pack::bundlePacks),
            ResourceAlbum.CODEC.optionalFieldOf("resourceAlbum").forGetter(Pack::resourceAlbum)
        ).apply(i, Pack::create));

        public static final Codec<List<Pack>> LIST_CODEC = CODEC.listOf();

        public static Pack create(String id, String name, String description, Optional<URI> compoundDownloadURI, Optional<URI> imageUrl, Optional<URI> compoundWorldTemplateDownloadURI, Optional<String> worldTemplateFolderName, List<BundlePack> bundlePacks, Optional<ResourceAlbum> resourceAlbum) {
            ParsedURI download = ParsedURI.of(compoundDownloadURI);
            ParsedURI worldTemplate = ParsedURI.of(compoundWorldTemplateDownloadURI);
            return new Pack(id, name, description, download.uri(), imageUrl, download.checkSum(), worldTemplate.uri(), worldTemplate.checkSum(), worldTemplateFolderName, bundlePacks, resourceAlbum);
        }

        public Component nameComponent() {
            return STARTERPACKS_PACK_ID.equals(id) ? Component.translatable("legacy.menu.store.starterpacks.download_all") : Component.literal(name);
        }

        public Component descriptionComponent() {
            return STARTERPACKS_PACK_ID.equals(id) ? Component.translatable("legacy.menu.store.starterpacks.download_all.description") : Component.literal(description);
        }

        public boolean hasWorldTemplate() {
            return worldTemplateDownloadURI.isPresent();
        }

        public boolean hasBundlePacks() {
            return !bundlePacks.isEmpty();
        }

        public boolean hasResourceAlbum() {
            return resourceAlbum.isPresent();
        }
    }

    private record ParsedURI(Optional<URI> uri, Optional<String> checkSum) {
        private static ParsedURI of(URI compoundURI) {
            return of(Optional.of(compoundURI));
        }

        private static ParsedURI of(Optional<URI> compoundURI) {
            if (compoundURI.isEmpty()) return new ParsedURI(Optional.empty(), Optional.empty());
            String[] splitURI = compoundURI.get().toString().split("\\?checksum=");
            return new ParsedURI(Optional.of(URI.create(splitURI[0])), splitURI.length < 2 ? Optional.empty() : Optional.of(splitURI[1]));
        }
    }

    public static InputStream openRemoteStream(URL url, int connectTimeout, int readTimeout) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection.getInputStream();
    }

    public static CompletableFuture<List<Pack>> fetchIndex(Category category) {
        if (STARTERPACKS_CATEGORY_ID.equals(category.id())) {
            return fetchStarterpacksIndex(category);
        }
        return fetchRemoteIndex(category.indexUrl());
    }

    private static CompletableFuture<List<Pack>> fetchRemoteIndex(String indexUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = openRemoteStream(new URL(indexUrl), 5000, 10000);
                 InputStreamReader reader = new InputStreamReader(stream)) {
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

    private static CompletableFuture<List<Pack>> fetchStarterpacksIndex(Category category) {
        return fetchStarterpacksBundlePacks().thenApply(bundlePacks -> {
            return List.of(new Pack(
                STARTERPACKS_PACK_ID,
                "Download All!",
                "Download every mash-up pack, skin pack, texture pack, and more!",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                bundlePacks,
                Optional.empty()
            ));
        });
    }

    private static List<Category> getStarterpacksCategories() {
        return CATEGORIES.stream()
            .filter(c -> !STARTERPACKS_CATEGORY_ID.equals(c.id()) && !"bundle_packs".equals(c.id()) && !"legacy4j".equals(c.id()))
            .toList();
    }

    private static CompletableFuture<List<Pack.BundlePack>> fetchStarterpacksBundlePacks() {
        List<Category> categories = getStarterpacksCategories();
        List<CompletableFuture<List<Pack>>> futures = categories.stream().map(ContentManager::fetchIndex).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
            List<Pack.BundlePack> bundlePacks = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                Category category = categories.get(i);
                for (Pack pack : futures.get(i).getNow(List.of())) {
                    bundlePacks.add(new Pack.BundlePack(
                        category.id(),
                        pack.id(),
                        pack.name(),
                        pack.description(),
                        pack.downloadURI(),
                        pack.imageUrl(),
                        pack.checkSum(),
                        pack.worldTemplateDownloadURI(),
                        pack.worldTemplateCheckSum(),
                        pack.worldTemplateFolderName()
                    ));
                }
            }
            return bundlePacks;
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

    private static Optional<Category> getCategory(String categoryId) {
        return CATEGORIES.stream().filter(c -> c.id().equals(categoryId)).findFirst();
    }

    private static boolean shouldApplyTransparencyFix(Category category) {
        return "resourcepacks".equals(category.targetDirectoryName()) && ("texture_packs".equals(category.id()) || "mashup_packs".equals(category.id()));
    }

    private static void applyTransparencyFix(Path targetFolder) throws IOException {
        Path fixTempFile = Files.createTempFile("legacy_pack_fix_", ".zip");
        try {
            try (InputStream stream = openRemoteStream(new URL(TRANSPARENCY_FIX_URL), 5000, 10000)) {
                Files.copy(stream, fixTempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            extractZip(fixTempFile, targetFolder);
        } finally {
            Files.deleteIfExists(fixTempFile);
        }
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

    public static boolean isPackDownloading(Pack pack, Category category) {
        return ACTIVE_DOWNLOADS.contains(downloadKey(category, pack));
    }

    public static boolean isPackInstalled(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            boolean standaloneInstalled = !hasStandaloneContent(pack) || isStandalonePackInstalled(pack, category);
            return standaloneInstalled && areBundleChildrenInstalled(pack) && (!pack.hasResourceAlbum() || DownloadedResourceAlbums.hasManagedBundleAlbum(pack));
        }
        return isStandalonePackInstalled(pack, category);
    }

    private static boolean isStandalonePackInstalled(Pack pack, Category category) {
        String folderName = category.targetDirectoryName();
        Path path = getContentDir(folderName).resolve(pack.id());
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;
        if ((DownloadedSkinPackStore.managesTargetDirectory(folderName) || CustomSkinPackStore.managesTargetDirectory(folderName)) && !DownloadedSkinPackStore.isValidPackInstall(path)) return false;
        if (pack.hasWorldTemplate() && !LegacyWorldTemplate.isDownloadedPackInstalled(pack)) return false;

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

    private static boolean hasStandaloneContent(Pack pack) {
        return pack.downloadURI().isPresent();
    }

    public static void downloadPack(Pack pack, Category category, Consumer<Boolean> onComplete) {
        downloadPackInternal(pack, category).whenComplete((installedAnything, throwable) ->
            Minecraft.getInstance().execute(() -> onComplete.accept(installedAnything != null && installedAnything))
        );
    }

    private static CompletableFuture<Boolean> downloadPackInternal(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            return downloadBundlePack(pack, category);
        }
        return downloadSinglePack(pack, category);
    }

    private static CompletableFuture<Boolean> downloadBundlePack(Pack pack, Category category) {
        List<Pack.BundlePack> bundlePacks = pack.bundlePacks();
        if (bundlePacks.isEmpty()) return CompletableFuture.completedFuture(false);
        ACTIVE_DOWNLOADS.add(downloadKey(category, pack));
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        if (hasStandaloneContent(pack)) futures.add(isStandalonePackInstalled(pack, category) ? CompletableFuture.completedFuture(false) : downloadPackFiles(pack, category, !pack.hasResourceAlbum(), false));
        futures.addAll(bundlePacks.stream().map(bundlePack -> {
            Optional<Category> bundleCategory = getCategory(bundlePack.categoryId());
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
                if (standaloneInstalled && areBundleChildrenInstalled(pack)) {
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
        if (isPackInstalled(pack, category)) {
            return CompletableFuture.completedFuture(false);
        }
        return downloadPackFiles(pack, category, syncResourceAlbum, true);
    }

    private static CompletableFuture<Boolean> downloadPackFiles(Pack pack, Category category, boolean syncResourceAlbum, boolean trackActiveDownload) {
        String folderName = category.targetDirectoryName();
        Path contentDir = getContentDir(folderName);
        if (trackActiveDownload) ACTIVE_DOWNLOADS.add(downloadKey(category, pack));
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (pack.downloadURI().isEmpty()) {
                    Legacy4J.LOGGER.warn("Pack {} has no download URI.", pack.id());
                    return false;
                }
                Path downloadedTempFile = Files.createTempFile("legacy_pack_", ".zip");
                
                try (InputStream stream = pack.downloadURI().get().toURL().openStream()) {
                    Files.copy(stream, downloadedTempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (pack.checkSum().isPresent()) {
                    String fileHash = readFileCheckSum(downloadedTempFile);
                    if (!pack.checkSum().get().equals(fileHash)) {
                        Legacy4J.LOGGER.warn("Checksum mismatch for pack {}. Expected {}, got {}", pack.id(), pack.checkSum().get(), fileHash);
                        Files.deleteIfExists(downloadedTempFile);
                        return false;
                    }
                }

                Path targetFolder = contentDir.resolve(pack.id());
                if (Files.exists(targetFolder)) {
                    deleteDirectoryRecursively(targetFolder.toFile());
                }
                Files.createDirectories(targetFolder);

                extractZip(downloadedTempFile, targetFolder);
                if (shouldApplyTransparencyFix(category)) {
                    Optional<byte[]> originalPackIcon = readOptionalFileBytes(targetFolder.resolve("pack.png"));
                    Optional<String> originalPackMeta = readOptionalFileText(targetFolder.resolve("pack.mcmeta"));
                    applyTransparencyFix(targetFolder);
                    restoreOptionalFileBytes(targetFolder.resolve("pack.png"), originalPackIcon);
                    mergeTransparencyFixPackMeta(targetFolder, originalPackMeta);
                }
                if (Minecraft.getInstance().getResourcePackDirectory().equals(contentDir)) {
                    DownloadedPackMetadata.write(targetFolder, pack);
                }
                if (DownloadedSkinPackStore.managesTargetDirectory(folderName)) {
                    DownloadedSkinPackStore.normalizeInstalledPack(targetFolder);
                } else if (CustomSkinPackStore.managesTargetDirectory(folderName)) {
                    CustomSkinPackStore.normalizeDownloadedPack(targetFolder);
                } else if (syncResourceAlbum && category.useResourceAlbum() && Minecraft.getInstance().getResourcePackDirectory().equals(contentDir)) {
                    DownloadedResourceAlbums.sync(pack);
                }
                LegacyWorldTemplate.downloadDownloadedPack(pack);
                Files.deleteIfExists(downloadedTempFile);

                if (pack.checkSum().isPresent()) {
                    Files.writeString(targetFolder.resolve(".md5"), pack.checkSum().get());
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

    public static void deletePack(Pack pack, Category category) {
        if (pack.hasBundlePacks()) {
            if (hasStandaloneContent(pack)) deleteStandalonePack(pack, category);
            DownloadedResourceAlbums.removeBundle(pack);
            for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
                getCategory(bundlePack.categoryId()).ifPresent(bundleCategory -> deletePack(bundlePack.toPack(), bundleCategory));
            }
            return;
        }
        deleteStandalonePack(pack, category);
    }

    private static void deleteStandalonePack(Pack pack, Category category) {
        Path packDir = getContentDir(category.targetDirectoryName()).resolve(pack.id());
        if (Files.exists(packDir)) {
            deleteDirectoryRecursively(packDir.toFile());
        }
        DownloadedPackMetadata.clear(pack.id());
        DownloadedResourceAlbums.remove(pack.id());
        PackAlbum.Selector.invalidatePackAssets(pack.id());
        LegacyWorldTemplate.removeDownloadedPack(pack.id());
        LegacyWorldTemplate.refreshDownloadedPacks();
    }

    private static boolean areBundleChildrenInstalled(Pack pack) {
        if (!pack.hasBundlePacks()) return false;
        for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
            Optional<Category> bundleCategory = getCategory(bundlePack.categoryId());
            if (bundleCategory.isEmpty() || !isPackInstalled(bundlePack.toPack(), bundleCategory.get())) return false;
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
