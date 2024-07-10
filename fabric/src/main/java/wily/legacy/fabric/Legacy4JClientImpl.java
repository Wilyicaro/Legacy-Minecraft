package wily.legacy.fabric;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import wily.legacy.fabric.compat.ModMenuCompat;
import wily.legacy.util.ModInfo;

public class Legacy4JClientImpl {
    public static void registerRenderType(RenderType renderType, Block... blocks) {
        BlockRenderLayerMap.INSTANCE.putBlocks(renderType,blocks);
    }

    public static Screen getConfigScreen(ModInfo mod, Screen screen) {
        return FabricLoader.getInstance().isModLoaded("modmenu") ? ModMenuCompat.getConfigScreen(mod.getId(),screen) : null;
    }
}
