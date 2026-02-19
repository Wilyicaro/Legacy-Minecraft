package wily.legacy.Skins.skin;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;

public class SkinPackReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        SkinPackLoader.loadPacks(manager);
        CpmModelManager.invalidateAll();
        GuiCpmPreviewCache.clearCaches();
    }
}
