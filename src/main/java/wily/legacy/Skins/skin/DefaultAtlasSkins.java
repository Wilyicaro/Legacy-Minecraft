package wily.legacy.Skins.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.platform.NativeImage;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class DefaultAtlasSkins {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final ResourceLocation ATLAS_PNG = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/default_atlas.png");
    private static final ResourceLocation ATLAS_JSON = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/default_atlas.json");
    private static boolean loaded;
    private static NativeImage atlas;
    private static final Map<String, Frame> frames = new HashMap<>();
    private static final Map<String, ResourceLocation> dynLocByFrame = new HashMap<>();
    private static final Map<ResourceLocation, DynamicTexture> dynTextures = new HashMap<>();

    private DefaultAtlasSkins() {
    }

    public static void clear() {
        frames.clear();
        if (atlas != null) {
            try {
                atlas.close();
            } catch (Throwable ignored) {
            }
            atlas = null;
        }
        loaded = false;
    }

    public static boolean isDefaultAtlasDyn(ResourceLocation id) {
        return id != null && SkinSync.MODID.equals(id.getNamespace()) && id.getPath().startsWith("dyn/default/");
    }

    public static DynamicTexture recreate(ResourceManager rm, ResourceLocation id) {
        if (!isDefaultAtlasDyn(id)) return null;
        String frame = id.getPath().substring("dyn/default/".length());
        if (frame.isBlank()) return null;
        ensureLoaded(rm);
        Frame fr = frames.get(frame);
        if (fr == null) return null;
        try {
            NativeImage cropped = crop(fr);
            DynamicTexture dt = new DynamicTexture(() -> "ConsoleSkins Default Skin " + frame, cropped);
            dt.upload();
            dynLocByFrame.put(frame, id);
            dynTextures.put(id, dt);
            return dt;
        } catch (Throwable t) {
            return null;
        }
    }

    public static ResourceLocation getTexture(ResourceManager rm, String texPath) {
        String frame = texPath;
        int slash = frame.lastIndexOf('/');
        if (slash >= 0) frame = frame.substring(slash + 1);
        if (frame.endsWith(".png")) frame = frame.substring(0, frame.length() - 4);
        ResourceLocation existing = dynLocByFrame.get(frame);
        if (existing != null) return existing;
        ensureLoaded(rm);
        Frame fr = frames.get(frame);
        if (fr == null) {
            return ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/" + texPath);
        }
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "dyn/default/" + frame);
            Minecraft mc = Minecraft.getInstance();
            DynamicTexture dt = new DynamicTexture("ConsoleSkins Default Skin " + frame, fr.w, fr.h, true);
            NativeImage cropped = crop(fr);
            dt.setPixels(cropped);
            dt.upload();
            mc.getTextureManager().register(loc, dt);
            dynTextures.put(loc, dt);
            dynLocByFrame.put(frame, loc);
            return loc;
        } catch (Throwable t) {
            LOGGER.warn("Failed to create dynamic atlas texture for {}: {}", frame, t.toString());
            return ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/" + texPath);
        }
    }

    private static void ensureLoaded(ResourceManager rm) {
        if (loaded) return;
        loaded = true;
        try {
            Resource resPng = rm.getResource(ATLAS_PNG).orElse(null);
            Resource resJson = rm.getResource(ATLAS_JSON).orElse(null);
            if (resPng == null || resJson == null) {
                LOGGER.warn("Default atlas resources missing (png={}, json={})", resPng != null, resJson != null);
                return;
            }
            try (InputStream in = resPng.open()) {
                atlas = NativeImage.read(in);
            }
            JsonObject root;
            try (InputStreamReader r = new InputStreamReader(resJson.open(), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(r).getAsJsonObject();
            }
            JsonObject fr = root.getAsJsonObject("frames");
            if (fr == null) {
                LOGGER.warn("Atlas JSON missing frames object");
                return;
            }
            for (Map.Entry<String, JsonElement> e : fr.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                int x = o.get("x").getAsInt();
                int y = o.get("y").getAsInt();
                int w = o.get("w").getAsInt();
                int h = o.get("h").getAsInt();
                frames.put(e.getKey(), new Frame(x, y, w, h));
            }
            LOGGER.info("Loaded default atlas: {} frames", frames.size());
        } catch (Throwable t) {
            LOGGER.warn("Failed to load default atlas: {}", t.toString());
        }
    }

    private static NativeImage crop(Frame fr) {
        NativeImage out = new NativeImage(fr.w, fr.h, true);
        for (int yy = 0; yy < fr.h; yy++) {
            for (int xx = 0; xx < fr.w; xx++) {
                int px = atlas.getPixel(fr.x + xx, fr.y + yy);
                out.setPixel(xx, yy, px);
            }
        }
        return out;
    }

    private record Frame(int x, int y, int w, int h) {
    }
}
