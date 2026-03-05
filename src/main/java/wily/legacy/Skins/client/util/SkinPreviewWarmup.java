package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SkinPreviewWarmup {
    private static final ConcurrentLinkedQueue<String> Q = new ConcurrentLinkedQueue<>();
    private static final Set<String> QUEUED = ConcurrentHashMap.newKeySet();

    private SkinPreviewWarmup() {
    }

    public static void clear() {
        Q.clear();
        QUEUED.clear();
    }

    public static void enqueue(String skinId, SkinEntry entry) {
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;
        if (!QUEUED.add(skinId)) return;
        Q.add(skinId);
    }

    public static void pump(Minecraft mc, int budget) {
        if (mc == null || budget <= 0) return;
        for (int i = 0; i < budget; i++) {
            String id = Q.poll();
            if (id == null) return;
            QUEUED.remove(id);

            SkinEntry entry = null;
            try {
                entry = SkinPackLoader.getSkin(id);
            } catch (Throwable ignored) {
            }

            ResourceLocation tex = null;
            try {
                tex = ClientSkinAssets.getTexture(id);
            } catch (Throwable ignored) {
            }
            if (tex == null && entry != null) tex = entry.texture();

            if (tex != null) {
                try {
                    mc.getTextureManager().getTexture(tex);
                } catch (Throwable ignored) {
                }
                try {
                    ResourceLocation modelId = ClientSkinAssets.getModelIdFromTexture(tex);
                    if (modelId != null) BoxModelManager.get(modelId);
                } catch (Throwable ignored) {
                }
            }

            if (entry != null && entry.cape() != null) {
                try {
                    mc.getTextureManager().getTexture(entry.cape());
                } catch (Throwable ignored) {
                }
            }

            try {
                boolean wantCape = entry != null && entry.cape() != null;
                ClientSkinAssets.getCachedPlayerSkin(id, entry, wantCape);
            } catch (Throwable ignored) {
            }
        }
    }
}
