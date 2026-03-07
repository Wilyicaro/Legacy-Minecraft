package wily.legacy.Skins.skin;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.Minecraft;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;

public class SkinPackReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    BoxModelManager.reload(manager);
                    SkinPackLoader.loadPacks(manager);
                });
                return;
            }
        } catch (Throwable ignored) {
        }
        BoxModelManager.reload(manager);
        SkinPackLoader.loadPacks(manager);
    }
}
