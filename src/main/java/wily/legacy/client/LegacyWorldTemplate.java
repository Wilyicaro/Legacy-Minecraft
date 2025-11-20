package wily.legacy.client;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
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
import wily.legacy.util.IOUtil;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record LegacyWorldTemplate(Component buttonMessage, ResourceLocation icon, String worldTemplate,
                                  String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, boolean preDownload,
                                  Optional<URI> downloadURI, Optional<String> checkSum) {
    public static final Codec<LegacyWorldTemplate> CODEC = RecordCodecBuilder.create(i -> i.group(DynamicUtil.getComponentCodec().fieldOf("buttonMessage").forGetter(LegacyWorldTemplate::buttonMessage), ResourceLocation.CODEC.fieldOf("icon").forGetter(LegacyWorldTemplate::icon), Codec.STRING.fieldOf("templateLocation").forGetter(LegacyWorldTemplate::worldTemplate), Codec.STRING.fieldOf("folderName").forGetter(LegacyWorldTemplate::folderName), Codec.BOOL.optionalFieldOf("directJoin", false).forGetter(LegacyWorldTemplate::directJoin), Codec.BOOL.optionalFieldOf("isLocked", false).forGetter(LegacyWorldTemplate::isLocked), Codec.BOOL.optionalFieldOf("isGamePath", false).forGetter(LegacyWorldTemplate::isGamePath), Codec.BOOL.optionalFieldOf("preDownload", false).forGetter(LegacyWorldTemplate::preDownload), Codec.STRING.xmap(URI::create, URI::toString).optionalFieldOf("downloadURI").forGetter(LegacyWorldTemplate::downloadURI)).apply(i, LegacyWorldTemplate::create));
    public static final Codec<List<LegacyWorldTemplate>> LIST_CODEC = CODEC.listOf();
    public static final List<LegacyWorldTemplate> list = new ArrayList<>();
    private static final String TEMPLATES = "world_templates.json";

    public static LegacyWorldTemplate create(Component buttonMessage, ResourceLocation icon, String worldTemplate, String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, boolean delayDownload, Optional<URI> compoundDownloadURI) {
        Optional<String[]> splitURI = compoundDownloadURI.map(u -> u.toString().split("\\?checksum="));
        return new LegacyWorldTemplate(buttonMessage, icon, worldTemplate, folderName, directJoin, isLocked, isGamePath, delayDownload, splitURI.map(s -> URI.create(s[0])), splitURI.map(s -> s.length < 2 ? null : s[1]));
    }

    public Path getPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(worldTemplate);
    }

    public InputStream open() {
        try {
            return isGamePath ? new FileInputStream(getPath().toString()) : Minecraft.getInstance().getResourceManager().open(FactoryAPI.createLocation(worldTemplate));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getValidPath() {
        if (!isGamePath || downloadURI.isEmpty()) return null;
        Path path = getPath();
        boolean exists = Files.exists(path);
        String checksum;
        if (exists && checkSum().isEmpty() || (exists && checkSum().get().equals(readFileCheckSum(path))) || (checksum = readCheckSum(path)) == null || !checkSum().get().equals(checksum))
            return null;

        return path;
    }

    public void downloadToPathIfPossible() {
        Path path = getValidPath();
        if (path == null) return;
        try (InputStream stream = downloadURI.get().toURL().openStream()) {
            Files.deleteIfExists(path);
            Files.createDirectories(path.getParent());
            Files.copy(stream, path);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when downloading world template to path {}: {}", path, e.getMessage());
        }
    }

    public String readCheckSum(Path path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(downloadURI.get() + ".md5").toURL().openStream()))) {
            return reader.readLine().trim();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading checksum from world template {}: {}", path, e.getMessage());
            return null;
        }
    }

    public String readFileCheckSum(Path path) {
        ByteSource byteSource = com.google.common.io.Files.asByteSource(path.toFile());
        try {
            return byteSource.hash(Hashing.md5()).toString();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading existing checksum from world template {}: {}", path, e.getMessage());
            return null;
        }
    }

    public static class Manager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            list.clear();
            IOUtil.getOrderedNamespaces(resourceManager).forEach(name -> resourceManager.getResource(FactoryAPI.createLocation(name, TEMPLATES)).ifPresent(r -> {
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader)).resultOrPartial(Legacy4J.LOGGER::warn).ifPresent(templates -> templates.forEach(template -> {
                        list.add(template);
                        if (template.preDownload())
                            template.downloadToPathIfPossible();
                    }));

                } catch (IOException var8) {
                    Legacy4J.LOGGER.warn(var8.getMessage());
                }
            }));
        }

        @Override
        public String getName() {
            return "legacy:world_template";
        }
    }
}