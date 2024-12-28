package wily.legacy.client;

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
import wily.legacy.util.JsonUtil;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record LegacyWorldTemplate(Component buttonMessage, ResourceLocation icon, String worldTemplate, String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, Optional<URI> downloadURI, Optional<String> checkSum) {
    public static final Codec<LegacyWorldTemplate> CODEC = RecordCodecBuilder.create(i-> i.group(DynamicUtil.getComponentCodec().fieldOf("buttonMessage").forGetter(LegacyWorldTemplate::buttonMessage), ResourceLocation.CODEC.fieldOf("icon").forGetter(LegacyWorldTemplate::icon), Codec.STRING.fieldOf("templateLocation").forGetter(LegacyWorldTemplate::worldTemplate), Codec.STRING.fieldOf("folderName").forGetter(LegacyWorldTemplate::folderName),Codec.BOOL.fieldOf("directJoin").orElse(false).forGetter(LegacyWorldTemplate::directJoin),Codec.BOOL.fieldOf("isLocked").orElse(true).forGetter(LegacyWorldTemplate::isLocked),Codec.BOOL.fieldOf("isGamePath").orElse(false).forGetter(LegacyWorldTemplate::isGamePath),Codec.STRING.xmap(URI::create,URI::toString).optionalFieldOf("downloadURI").forGetter(LegacyWorldTemplate::downloadURI)).apply(i, LegacyWorldTemplate::create));

    public static LegacyWorldTemplate create(Component buttonMessage, ResourceLocation icon, String worldTemplate, String folderName, boolean directJoin, boolean isLocked, boolean isGamePath, Optional<URI> compoundDownloadURI) {
        Optional<String[]> splitURI = compoundDownloadURI.map(u->u.toString().split("\\?checksum="));
        return new LegacyWorldTemplate(buttonMessage, icon, worldTemplate, folderName, directJoin, isLocked, isGamePath, splitURI.map(s->URI.create(s[0])),splitURI.map(s->s.length < 2 ? null : s[1]));
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
    public void downloadToPathIfPossible(){
        if (!isGamePath || downloadURI.isEmpty()) return;
        Path path = getPath();
        boolean exists = Files.exists(path);
        String checksum;
        if (exists && checkSum().isEmpty() || (checksum = readCheckSum(path)) == null || checkSum().get().equals(checksum)) return;
        try (InputStream stream = downloadURI.get().toURL().openStream()) {
            if (exists) Files.delete(path);
            Files.createDirectories(path.getParent());
            Files.copy(stream,path);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when downloading world template to path {}: {}",path,e.getMessage());
        }
    }

    public String readCheckSum(Path path){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(downloadURI.get()+".md5").toURL().openStream()))) {
            return reader.readLine().trim();
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Error when reading checksum from world template {}: {}",path,e.getMessage());
            return null;
        }
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
                                list.add(create(Component.translatable(s), FactoryAPI.createLocation(GsonHelper.getAsString(tabObj, "icon")), GsonHelper.getAsString(tabObj, "templateLocation"), GsonHelper.getAsString(tabObj, "folderName"), GsonHelper.getAsBoolean(tabObj, "directJoin", false), true, false, Optional.empty()));
                            }
                        });
                    } else if (element instanceof JsonArray a) a.forEach(e->CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(w->{
                        w.downloadToPathIfPossible();
                        list.add(w);
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