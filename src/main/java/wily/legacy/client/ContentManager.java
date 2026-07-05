package wily.legacy.client;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ContentManager {

    public static final List<Category> CATEGORIES = new ArrayList<>();
    private static final String CATEGORIES_FILE = "store_categories.json";
    static final String STARTERPACKS_CATEGORY_ID = "starterpacks";
    static final String STARTERPACKS_PACK_ID = "starterpacks_bundle";
    static final java.util.Set<String> AUTO_APPLY_RESOURCE_PACKS = ConcurrentHashMap.newKeySet();
    static final String AUTO_APPLY_RESOURCE_PACK_TAG = "auto_apply_resource_pack";

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
        Optional<ResourceAlbum> resourceAlbum,
        List<Variant> downloadVariants,
        List<Variant> worldTemplateVariants
    ) {
        public record Variant(
            Optional<String> id,
            Optional<String> minVersion,
            Optional<URI> downloadURI,
            Optional<String> checkSum
        ) {
            public static final Codec<Variant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("id").forGetter(Variant::id),
                Codec.STRING.optionalFieldOf("minVersion").forGetter(Variant::minVersion),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("downloadURI").forGetter(Variant::downloadURI)
            ).apply(i, Variant::create));

            public static Variant create(Optional<String> id, Optional<String> minVersion, Optional<URI> compoundDownloadURI) {
                ParsedURI download = ParsedURI.of(compoundDownloadURI);
                return new Variant(id, minVersion, download.uri(), download.checkSum());
            }
        }

        public record ResourceAlbum(
            Optional<String> id,
            int version,
            Optional<String> name,
            Optional<String> description,
            Optional<Identifier> icon,
            Optional<Identifier> background,
            List<String> packs,
            Optional<String> displayPack
        ) {
            public static final Codec<ResourceAlbum> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("id").forGetter(ResourceAlbum::id),
                Codec.INT.optionalFieldOf("version", 0).forGetter(ResourceAlbum::version),
                Codec.STRING.optionalFieldOf("name").forGetter(ResourceAlbum::name),
                Codec.STRING.optionalFieldOf("description").forGetter(ResourceAlbum::description),
                Identifier.CODEC.optionalFieldOf("icon").forGetter(ResourceAlbum::icon),
                Identifier.CODEC.optionalFieldOf("background").forGetter(ResourceAlbum::background),
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
            Optional<String> worldTemplateFolderName,
            List<Variant> downloadVariants,
            List<Variant> worldTemplateVariants
        ) {
            public static final Codec<BundlePack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("categoryId").forGetter(BundlePack::categoryId),
                Codec.STRING.fieldOf("id").forGetter(BundlePack::id),
                Codec.STRING.fieldOf("name").forGetter(BundlePack::name),
                Codec.STRING.optionalFieldOf("description", "").forGetter(BundlePack::description),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("downloadURI").forGetter(BundlePack::downloadURI),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("imageUrl").forGetter(BundlePack::imageUrl),
                Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("worldTemplateDownloadURI").forGetter(BundlePack::worldTemplateDownloadURI),
                Codec.STRING.optionalFieldOf("worldTemplateFolderName").forGetter(BundlePack::worldTemplateFolderName),
                Variant.CODEC.listOf().optionalFieldOf("downloadVariants", List.of()).forGetter(BundlePack::downloadVariants),
                Variant.CODEC.listOf().optionalFieldOf("worldTemplateVariants", List.of()).forGetter(BundlePack::worldTemplateVariants)
            ).apply(i, BundlePack::create));

            public static BundlePack create(String categoryId, String id, String name, String description, Optional<URI> compoundDownloadURI, Optional<URI> imageUrl, Optional<URI> compoundWorldTemplateDownloadURI, Optional<String> worldTemplateFolderName, List<Variant> downloadVariants, List<Variant> worldTemplateVariants) {
                ParsedURI download = ParsedURI.of(compoundDownloadURI);
                ParsedURI worldTemplate = ParsedURI.of(compoundWorldTemplateDownloadURI);
                return new BundlePack(categoryId, id, name, description, download.uri(), imageUrl, download.checkSum(), worldTemplate.uri(), worldTemplate.checkSum(), worldTemplateFolderName, downloadVariants, worldTemplateVariants);
            }

            public Pack toPack() {
                return new Pack(id, name, description, downloadURI, imageUrl, checkSum, worldTemplateDownloadURI, worldTemplateCheckSum, worldTemplateFolderName, List.of(), Optional.empty(), downloadVariants, worldTemplateVariants);
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
            ResourceAlbum.CODEC.optionalFieldOf("resourceAlbum").forGetter(Pack::resourceAlbum),
            Variant.CODEC.listOf().optionalFieldOf("downloadVariants", List.of()).forGetter(Pack::downloadVariants),
            Variant.CODEC.listOf().optionalFieldOf("worldTemplateVariants", List.of()).forGetter(Pack::worldTemplateVariants)
        ).apply(i, Pack::create));

        public static final Codec<List<Pack>> LIST_CODEC = CODEC.listOf();

        public static Pack create(String id, String name, String description, Optional<URI> compoundDownloadURI, Optional<URI> imageUrl, Optional<URI> compoundWorldTemplateDownloadURI, Optional<String> worldTemplateFolderName, List<BundlePack> bundlePacks, Optional<ResourceAlbum> resourceAlbum, List<Variant> downloadVariants, List<Variant> worldTemplateVariants) {
            ParsedURI download = ParsedURI.of(compoundDownloadURI);
            ParsedURI worldTemplate = ParsedURI.of(compoundWorldTemplateDownloadURI);
            return new Pack(id, name, description, download.uri(), imageUrl, download.checkSum(), worldTemplate.uri(), worldTemplate.checkSum(), worldTemplateFolderName, bundlePacks, resourceAlbum, downloadVariants, worldTemplateVariants);
        }

        public Component nameComponent() {
            return STARTERPACKS_PACK_ID.equals(id) ? Component.translatable("legacy.menu.store.starterpacks.download_all") : Component.literal(name);
        }

        public Component descriptionComponent() {
            return STARTERPACKS_PACK_ID.equals(id) ? Component.translatable("legacy.menu.store.starterpacks.download_all.description") : Component.literal(description);
        }

        public boolean hasWorldTemplate() {
            return activeWorldTemplateDownloadURI().isPresent();
        }

        public boolean hasBundlePacks() {
            return !bundlePacks.isEmpty();
        }

        public boolean hasResourceAlbum() {
            return resourceAlbum.isPresent();
        }

        public Optional<URI> activeDownloadURI() {
            return selectVariant(downloadVariants).flatMap(Variant::downloadURI).or(() -> downloadURI);
        }

        public Optional<String> activeCheckSum() {
            return ContentManager.activeCheckSum(downloadVariants, checkSum);
        }

        public Optional<URI> activeWorldTemplateDownloadURI() {
            return selectVariant(worldTemplateVariants).flatMap(Variant::downloadURI).or(() -> worldTemplateDownloadURI);
        }

        public Optional<String> activeWorldTemplateCheckSum() {
            return ContentManager.activeCheckSum(worldTemplateVariants, worldTemplateCheckSum);
        }

        public String activeDownloadKey() {
            return selectVariant(downloadVariants).map(ContentManager::variantKey).orElse("default");
        }
    }

    private record ParsedURI(Optional<URI> uri, Optional<String> checkSum) {
        private static ParsedURI of(Optional<URI> compoundURI) {
            if (compoundURI.isEmpty()) return new ParsedURI(Optional.empty(), Optional.empty());
            String[] splitURI = compoundURI.get().toString().split("\\?checksum=");
            return new ParsedURI(Optional.of(URI.create(splitURI[0])), splitURI.length < 2 ? Optional.empty() : Optional.of(splitURI[1]));
        }
    }

    private static Optional<Pack.Variant> selectVariant(List<Pack.Variant> variants) {
        Pack.Variant selected = null;
        for (Pack.Variant variant : variants) {
            if (matchesVersion(variant)) selected = variant;
        }
        return Optional.ofNullable(selected);
    }

    private static Optional<String> activeCheckSum(List<Pack.Variant> variants, Optional<String> fallback) {
        Optional<Pack.Variant> variant = selectVariant(variants);
        return variant.flatMap(Pack.Variant::downloadURI).isPresent() ? variant.flatMap(Pack.Variant::checkSum) : fallback;
    }

    private static boolean matchesVersion(Pack.Variant range) {
        String current = SharedConstants.getCurrentVersion().name();
        return range.minVersion().isEmpty() || compareVersions(current, range.minVersion().get()) >= 0;
    }

    private static int compareVersions(String left, String right) {
        List<Integer> a = Legacy4J.getParsedVersion(left);
        List<Integer> b = Legacy4J.getParsedVersion(right);
        int size = Math.max(a.size(), b.size());
        for (int i = 0; i < size; i++) {
            int av = i < a.size() ? a.get(i) : 0;
            int bv = i < b.size() ? b.get(i) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static String variantKey(Pack.Variant variant) {
        return variant.id().or(() -> variant.minVersion()).or(() -> variant.checkSum()).or(() -> variant.downloadURI().map(URI::toString)).orElse("default");
    }

    public static InputStream openRemoteStream(URL url, int connectTimeout, int readTimeout) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection.getInputStream();
    }

    public static CompletableFuture<List<Pack>> fetchIndex(Category category) {
        return ContentIndexLoader.fetchIndex(category);
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

    private static String downloadKey(Category category, Pack pack) {
        return category.id() + ":" + pack.id();
    }

    static Optional<Category> getCategory(String categoryId) {
        return CATEGORIES.stream().filter(c -> c.id().equals(categoryId)).findFirst();
    }

    public static boolean isPackDownloading(Pack pack, Category category) {
        return ContentPackDownloader.isDownloading(pack, category);
    }

    public static boolean isPackInstalled(Pack pack, Category category) {
        return ContentPackDownloader.isInstalled(pack, category);
    }

    public static void prepareDownloadTarget(Pack pack, Category category) throws IOException {
        prepareManagedTarget(category);
        if (!pack.hasBundlePacks()) return;
        for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
            Optional<Category> bundleCategory = getCategory(bundlePack.categoryId());
            if (bundleCategory.isPresent() && !bundleCategory.get().id().equals(category.id())) {
                prepareManagedTarget(bundleCategory.get());
            }
        }
    }

    private static void prepareManagedTarget(Category category) throws IOException {
        String folderName = category.targetDirectoryName();
        Minecraft minecraft = Minecraft.getInstance();
        if (DownloadedSkinPackStore.managesTargetDirectory(folderName)) {
            DownloadedSkinPackStore.enableResourcePack(minecraft);
        } else if (CustomSkinPackStore.managesTargetDirectory(folderName)) {
            CustomSkinPackStore.enableResourcePack(minecraft);
        }
    }

    public static void downloadPack(Pack pack, Category category, Consumer<Boolean> onComplete) {
        ContentPackDownloader.download(pack, category).whenComplete((installedAnything, throwable) ->
            Minecraft.getInstance().execute(() -> onComplete.accept(installedAnything != null && installedAnything))
        );
    }

    public static boolean applyAutoResourcePacks(Pack pack, Category category) {
        boolean changed = applyAutoResourcePack(pack, category);
        for (Pack.BundlePack bundlePack : pack.bundlePacks()) {
            Optional<Category> bundleCategory = getCategory(bundlePack.categoryId());
            if (bundleCategory.isPresent()) {
                changed |= applyAutoResourcePacks(bundlePack.toPack(), bundleCategory.get());
            }
        }
        return changed;
    }

    private static boolean applyAutoResourcePack(Pack pack, Category category) {
        if (!isRootResourcePackCategory(category) || !AUTO_APPLY_RESOURCE_PACKS.contains(downloadKey(category, pack))) return false;
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository repository = minecraft.getResourcePackRepository();
        if (repository == null) return false;
        repository.reload();
        String resolvedId = resolveResourcePackId(repository, pack.id());
        if (resolvedId == null) return false;
        List<String> globalPacks = new ArrayList<>(GlobalPacks.globalResources.get().list());
        boolean configChanged = removePackId(globalPacks, pack.id());
        if (!globalPacks.contains(resolvedId)) {
            globalPacks.add(resolvedId);
            configChanged = true;
        }
        if (configChanged) {
            GlobalPacks.globalResources.set(GlobalPacks.globalResources.get().withPacks(globalPacks));
            GlobalPacks.globalResources.save();
        }
        List<String> oldSelection = PackAlbum.getSelectedIds(repository);
        GlobalPacks.globalResources.get().applyPacks(repository, PackAlbum.getDefaultResourceAlbum().packs());
        boolean selectionChanged = !oldSelection.equals(PackAlbum.getSelectedIds(repository));
        if (selectionChanged) PackAlbum.updateSavedResourcePacks();
        return selectionChanged;
    }

    private static boolean isRootResourcePackCategory(Category category) {
        return isRootResourcePackDirectory(getContentDir(category.targetDirectoryName()));
    }

    static boolean isRootResourcePackDirectory(Path path) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && samePath(minecraft.getResourcePackDirectory(), path);
    }

    private static boolean samePath(Path first, Path second) {
        return first != null && second != null && first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    private static String resolveResourcePackId(PackRepository repository, String packId) {
        String normalized = normalizeFilePackId(packId);
        String fileId = "file/" + normalized;
        if (repository.getPack(fileId) != null) return fileId;
        if (repository.getPack(normalized) != null) return normalized;
        if (repository.getPack(packId) != null) return packId;
        return null;
    }

    private static boolean removePackId(List<String> packs, String packId) {
        String normalized = normalizeFilePackId(packId);
        return packs.remove(normalized) | packs.remove("file/" + normalized);
    }

    private static String normalizeFilePackId(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }

    public static void deletePack(Pack pack, Category category) {
        ContentPackDownloader.delete(pack, category);
    }
}
