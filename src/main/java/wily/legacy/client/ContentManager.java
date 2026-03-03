package wily.legacy.client;

import wily.legacy.api.ContentPack;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentManager {
    private static final Gson GSON = new Gson();

    public static CompletableFuture<List<ContentPack>> fetchIndex(String indexUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader reader = new InputStreamReader(new URL(indexUrl).openStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return GSON.fromJson(json.get("packs"), new TypeToken<List<ContentPack>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    public static Path getContentDir(String folderName) {
        File dir = new File(Minecraft.getInstance().gameDirectory, folderName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath();
    }

    public static boolean isPackInstalled(ContentPack pack, String folderName) {
        Path path = getContentDir(folderName).resolve(pack.id());
        return Files.exists(path) && Files.isDirectory(path);
    }

    public static void downloadPack(ContentPack pack, String folderName, Runnable onFinished) {
        if (isPackInstalled(pack, folderName)) return;

        Path contentDir = getContentDir(folderName);
        CompletableFuture.runAsync(() -> {
            try {
                Path downloadedTempFile = HttpUtil.downloadFile(
                    contentDir,
                    new URL(pack.downloadURI()),
                    new java.util.HashMap<>(),
                    com.google.common.hash.Hashing.sha256(),
                    null,
                    50 * 1024 * 1024,
                    java.net.Proxy.NO_PROXY,
                    new HttpUtil.DownloadProgressListener() {
                        public void requestStart() {}
                        public void downloadStart(java.util.OptionalLong size) {}
                        public void downloadedBytes(long bytes) {}
                        public void requestFinished(boolean success) {}
                    }
                );

                if (downloadedTempFile != null && Files.exists(downloadedTempFile)) {
                    Path targetFolder = contentDir.resolve(pack.id());
                    extractZip(downloadedTempFile, targetFolder);
                    Files.deleteIfExists(downloadedTempFile);
                    Minecraft.getInstance().execute(onFinished);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String normalizedName = entry.getName().replace('\\', '/');
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
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}