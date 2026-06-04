package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.FileUtils;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Predicate;

public class LegacySaveCache {

    public static LevelStorageSource currentWorldSource;
    public static boolean manualSave = false;
    public static boolean saveExit = false;
    public static boolean retakeWorldIcon = false;
    private static boolean saveCopyRequested = false;
    private static String worldOrderLevelId;
    private static long worldOrderLastPlayed;

    public static void setup(Minecraft m) {
        currentWorldSource = LevelStorageSource.createDefault(m.gameDirectory.toPath().resolve("current-world"));
    }

    public static LevelStorageSource getLevelStorageSource() {
        return LegacyOptions.saveCache.get() ? currentWorldSource : Minecraft.getInstance().getLevelSource();
    }

    public static boolean hasSaveSystem(Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() && !minecraft.isDemo() && !minecraft.getSingleplayerServer().isHardcore() && isCurrentWorldSource(minecraft.getSingleplayerServer().storageSource);
    }

    public static Path getLevelDirectory(LevelStorageSource.LevelStorageAccess storageSource) {
        return storageSource.getLevelPath(LevelResource.ROOT).normalize();
    }

    public static boolean isCurrentWorldSource(LevelStorageSource.LevelStorageAccess storageSource) {
        return storageSource.parent().getBaseDir().equals(currentWorldSource.getBaseDir());
    }

    public static void saveLevel(LevelStorageSource.LevelStorageAccess storageSource) {
        if (isCurrentWorldSource(storageSource)) {
            copySaveBtwSources(storageSource, Minecraft.getInstance().getLevelSource(), false);
        }
    }

    public static void requestSaveCopy() {
        saveCopyRequested = true;
    }

    public static boolean consumeSaveCopyRequest() {
        boolean requested = saveCopyRequested;
        saveCopyRequested = false;
        return requested;
    }

    public static void clearWorldOrderSnapshot() {
        worldOrderLevelId = null;
    }

    public static void restoreWorldOrderSnapshot() {
        if (worldOrderLevelId == null) return;
        try {
            Path dataPath = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(worldOrderLevelId).resolve(LevelResource.LEVEL_DATA_FILE.id());
            CompoundTag root = NbtIo.readCompressed(dataPath, NbtAccounter.unlimitedHeap());
            root.getCompoundOrEmpty("Data").putLong("LastPlayed", worldOrderLastPlayed);
            NbtIo.writeCompressed(root, dataPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            clearWorldOrderSnapshot();
        }
    }

    public static String importSaveFile(InputStream saveInputStream, Predicate<String> exists, LevelStorageSource source, String saveDirName) {
        return Legacy4JClient.manageAvailableSaveDirName(f -> Legacy4J.copySaveToDirectory(saveInputStream, f), exists, source, saveDirName);
    }

    public static String importSaveFile(InputStream saveInputStream, LevelStorageSource source, String saveDirName) {
        return importSaveFile(saveInputStream, source::levelExists, source, saveDirName);
    }

    public static String copySaveFile(Path savePath, LevelStorageSource source, String saveDirName) {
        return Legacy4JClient.manageAvailableSaveDirName(f -> {
            try {
                FileUtils.copyDirectory(savePath.toFile(), f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, source::levelExists, source, saveDirName);
    }

    public static void copySaveBtwSources(LevelStorageSource.LevelStorageAccess sendSource, LevelStorageSource destSource) {
        copySaveBtwSources(sendSource, destSource, true);
    }

    public static void copySaveBtwSources(LevelStorageSource.LevelStorageAccess sendSource, LevelStorageSource destSource, boolean deleteOldDest) {
        try {
            if (deleteOldDest && currentWorldSource != null && destSource.getBaseDir().equals(currentWorldSource.getBaseDir()) && sendSource.parent().getBaseDir().equals(Minecraft.getInstance().getLevelSource().getBaseDir())) {
                clearWorldOrderSnapshot();
                CompoundTag data = NbtIo.readCompressed(sendSource.getLevelPath(LevelResource.LEVEL_DATA_FILE), NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("Data");
                worldOrderLevelId = sendSource.getLevelId();
                worldOrderLastPlayed = data.getLongOr("LastPlayed", 0L);
            }
            Path sourceLevelDirectory = getLevelDirectory(sendSource);
            Path destLevelDirectoryPath = destSource.getBaseDir().resolve(sendSource.getLevelId());
            File destLevelDirectory = destLevelDirectoryPath.toFile();
            if (deleteOldDest && destLevelDirectory.exists()) FileUtils.deleteQuietly(destLevelDirectory);
            FileUtils.copyDirectory(sourceLevelDirectory.toFile(), destLevelDirectory, p -> {
                if (p.getName().equals("session.lock")) return false;
                if (deleteOldDest) return true;
                File destFile = p.toPath().relativize(destLevelDirectory.toPath()).toFile();
                return !destFile.exists() || FileUtils.isFileNewer(p, destFile);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
