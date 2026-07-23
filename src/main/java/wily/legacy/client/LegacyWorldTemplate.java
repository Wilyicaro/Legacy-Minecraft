package wily.legacy.client;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.FilenameUtils;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.CreationList;
import wily.legacy.util.JsonUtil;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record LegacyWorldTemplate(Component buttonMessage, ResourceLocation icon, String worldTemplate, String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, boolean preDownload, Optional<String> albumId, Optional<URI> downloadURI, Optional<URI> checksumURI, Optional<String> checkSum) {
    public static final Codec<LegacyWorldTemplate> CODEC = RecordCodecBuilder.create(i-> i.group(DynamicUtil.getComponentCodec().fieldOf("buttonMessage").forGetter(LegacyWorldTemplate::buttonMessage), ResourceLocation.CODEC.fieldOf("icon").forGetter(LegacyWorldTemplate::icon), Codec.STRING.fieldOf("templateLocation").forGetter(LegacyWorldTemplate::worldTemplate), Codec.STRING.fieldOf("folderName").forGetter(LegacyWorldTemplate::folderName),Codec.BOOL.fieldOf("directJoin").orElse(false).forGetter(LegacyWorldTemplate::directJoin),Codec.BOOL.fieldOf("isLocked").orElse(true).forGetter(LegacyWorldTemplate::isLocked),Codec.BOOL.fieldOf("isGamePath").orElse(false).forGetter(LegacyWorldTemplate::isGamePath),Codec.BOOL.fieldOf("preDownload").orElse(false).forGetter(LegacyWorldTemplate::preDownload), Codec.STRING.optionalFieldOf("resourceAlbum").forGetter(LegacyWorldTemplate::albumId),Codec.STRING.xmap(URI::create,URI::toString).optionalFieldOf("downloadURI").forGetter(LegacyWorldTemplate::downloadURI),Codec.STRING.xmap(URI::create,URI::toString).optionalFieldOf("checksumURI").forGetter(LegacyWorldTemplate::checksumURI),Codec.STRING.optionalFieldOf("checksum").forGetter(LegacyWorldTemplate::checkSum)).apply(i, LegacyWorldTemplate::create));

    public static LegacyWorldTemplate create(Component buttonMessage, ResourceLocation icon, String worldTemplate, String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, boolean preDownload, Optional<String> albumId, Optional<URI> compoundDownloadURI, Optional<URI> checksumURI, Optional<String> checkSum) {
        Optional<String[]> splitURI = compoundDownloadURI.map(u->u.toString().split("\\?checksum="));
        Optional<URI> downloadURI = splitURI.map(s->URI.create(s[0]));
        return new LegacyWorldTemplate(buttonMessage, icon, worldTemplate, folderName, directJoin, isLocked, isGamePath, preDownload, albumId, downloadURI, checksumURI.or(()->downloadURI.map(u->URI.create(u + ".md5"))), checkSum.or(()->splitURI.map(s->s.length < 2 ? null : s[1])));
    }

    public Path getPath(){
        return Minecraft.getInstance().gameDirectory.toPath().resolve(worldTemplate);
    }

    public InputStream open(){
        try {
            return isGamePath ? new FileInputStream(getPath().toString()) : Minecraft.getInstance().getResourceManager().open(FactoryAPI.createLocation(worldTemplate));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public Path getDownloadPath(){
        if (!isGamePath || downloadURI.isEmpty()) return null;
        Path path = getPath();
        if (checkSum().isEmpty()) return path;
        if (Files.exists(path) && checkSum().get().equals(readFileCheckSum(path))) return null;
        String checksum = readCheckSum(path);
        if (checksum == null) return path;
        return checkSum().get().equals(checksum) ? path : null;
    }
    public void downloadToPathIfPossible(){
        Path path = getDownloadPath();
        if (path == null) return;
        try (InputStream stream = downloadURI.get().toURL().openStream()) {
            Files.deleteIfExists(path);
            Files.createDirectories(path.getParent());
            Files.copy(stream,path);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when downloading world template to path {}: {}",path,e.getMessage());
        }
    }

    public String readCheckSum(Path path){
        if (checksumURI.isEmpty()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(checksumURI.get().toURL().openStream()))) {
            return reader.readLine().trim();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading checksum from world template {}: {}",path,e.getMessage());
            return null;
        }
    }
    public String readFileCheckSum(Path path){
        ByteSource byteSource = com.google.common.io.Files.asByteSource(path.toFile());
        try {
            return byteSource.hash(Hashing.md5()).toString();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading existing checksum from world template {}: {}",path,e.getMessage());
            return null;
        }
    }

    public static boolean isDownloadedPackInstalled(ContentManager.Pack pack) {
        if (!pack.hasWorldTemplate()) return true;
        Path path = downloadedPackPath(pack.id());
        if (!Files.isRegularFile(path)) return false;
        return pack.activeWorldTemplateCheckSum().map(s -> s.equals(ContentManager.readFileCheckSum(path))).orElse(true);
    }

    public static void downloadDownloadedPack(ContentManager.Pack pack) throws IOException {
        if (!pack.hasWorldTemplate()) return;
        Path path = downloadedPackPath(pack.id());
        Path temp = Files.createTempFile("legacy_world_", ".mcsave");
        try {
            try (InputStream stream = ContentManager.openRemoteStream(pack.activeWorldTemplateDownloadURI().orElseThrow().toURL(), 5000, 10000)) {
                Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            if (pack.activeWorldTemplateCheckSum().isPresent()) {
                String fileHash = ContentManager.readFileCheckSum(temp);
                if (!pack.activeWorldTemplateCheckSum().get().equals(fileHash)) {
                    throw new IOException("Checksum mismatch for world template " + pack.id());
                }
            }
            Files.createDirectories(path.getParent());
            Files.copy(temp, path, StandardCopyOption.REPLACE_EXISTING);
            invalidateWorldIcon(path);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static void removeDownloadedPack(String packId) {
        try {
            Path path = downloadedPackPath(packId);
            if (Files.deleteIfExists(path)) invalidateWorldIcon(path);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to remove downloaded world template {}", packId, e);
        }
    }

    public static void refreshDownloadedPacks() {
        list.removeIf(LegacyWorldTemplate::isManagedDownloadedPack);
        loadDownloadedPacks(list);
    }

    private static void loadDownloadedPacks(List<LegacyWorldTemplate> list) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        Path dir = minecraft.getResourcePackDirectory();
        if (dir == null || !Files.isDirectory(dir)) return;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString)
                .map(LegacyWorldTemplate::downloadedPackTemplate)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(template -> template.buttonMessage().getString(), String.CASE_INSENSITIVE_ORDER))
                .forEach(list::add);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to load downloaded world templates", e);
        }
    }

    private static Optional<LegacyWorldTemplate> downloadedPackTemplate(String packId) {
        DownloadedPackMetadata.Entry entry = DownloadedPackMetadata.entry(packId);
        if (!entry.hasWorldTemplate() || !Files.isRegularFile(downloadedPackPath(packId))) return Optional.empty();
        String name = entry.name() == null || entry.name().isBlank() ? packId : entry.name();
        String folderName = entry.worldTemplateFolderName();
        if (folderName == null || folderName.isBlank()) folderName = name;
        return Optional.of(new LegacyWorldTemplate(Component.literal(name), Legacy4J.createModLocation("creation_list/create_world"), downloadedPackLocation(packId), folderName, false, true, true, false, Optional.of(DownloadedResourceAlbums.albumId(packId)), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static Path downloadedPackPath(String packId) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(downloadedPackLocation(packId));
    }

    private static String downloadedPackLocation(String packId) {
        return "legacy_world_templates/files/" + normalizeDownloadedPackId(packId) + ".mcsave";
    }

    private static String normalizeDownloadedPackId(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }

    private static void invalidateWorldIcon(Path path) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) minecraft.execute(() -> CreationList.invalidateWorldIcon(path));
    }

    private static boolean isManagedDownloadedPack(LegacyWorldTemplate template) {
        return template.albumId().filter(DownloadedResourceAlbums::isManagedAlbum).isPresent() || template.isGamePath() && template.worldTemplate().startsWith("legacy_world_templates/files/");
    }

    public static final List<LegacyWorldTemplate> list = new ArrayList<>();
    private static final String TEMPLATES = "world_templates.json";
    public static class Manager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            list.clear();
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name-> resourceManager.getResource(FactoryAPI.createLocation(name, TEMPLATES)).ifPresent(r->{
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonElement element = JsonParser.parseReader(bufferedReader);
                    if (element instanceof JsonObject obj) {
                        Legacy4J.LOGGER.warn("The World Template {} is using a deprecated syntax, please contact this resource creator or try updating it.", name+":"+TEMPLATES);
                        obj.asMap().forEach((s, e) -> {
                            if (e instanceof JsonObject tabObj) {
                                list.add(create(Component.translatable(s), FactoryAPI.createLocation(GsonHelper.getAsString(tabObj, "icon")), GsonHelper.getAsString(tabObj, "templateLocation"), GsonHelper.getAsString(tabObj, "folderName"), GsonHelper.getAsBoolean(tabObj, "directJoin", false), true, false, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                            }
                        });
                    } else if (element instanceof JsonArray a) a.forEach(e->CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(w->{
                        list.add(w);
                        if (w.preDownload()) w.downloadToPathIfPossible();
                    }));
                } catch (IOException var8) {
                    Legacy4J.LOGGER.warn(var8.getMessage());
                }
            }));
            loadDownloadedPacks(list);
        }
        @Override
        public String getName() {
            return "legacy:world_template";
        }
    }
}
