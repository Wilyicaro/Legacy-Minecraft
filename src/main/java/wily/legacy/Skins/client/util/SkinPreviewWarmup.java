package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;

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

    public static void enqueue(String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (!QUEUED.add(skinId)) return;
        Q.add(skinId);
    }

    public static void pump(Minecraft mc, int budget) {
        if (mc == null || budget <= 0) return;
        for (int i = 0; i < budget; i++) {
            String id = Q.poll();
            if (id == null) return;
            QUEUED.remove(id);
            warm(mc, id);
        }
    }

    private static void warm(Minecraft mc, String id) {
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(id);
        if (resolved == null) return;
        warmTexture(mc, resolved.texture());
        warmTexture(mc, resolved.boxTexture());
        var entry = resolved.entry();
        warmTexture(mc, entry != null ? entry.cape() : null);
        warmCachedSkin(id, resolved);
    }

    private static void warmTexture(Minecraft mc, ResourceLocation texture) {
        if (mc == null || texture == null) return;
        mc.getTextureManager().getTexture(texture);
    }

    private static void warmCachedSkin(String id, ClientSkinAssets.ResolvedSkin resolved) {
        var entry = resolved.entry();
        ClientSkinAssets.getCachedPlayerSkin(id, entry, entry != null && entry.cape() != null);
    }
}
