package wily.legacy.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import wily.legacy.util.ModInfo;

public class Legacy4JClientImpl {
    public static void registerRenderType(RenderType renderType, Block... blocks) {
        for (Block block : blocks) {
            ItemBlockRenderTypes.setRenderLayer(block,renderType);
        }
    }

    public static Screen getConfigScreen(ModInfo mod, Screen screen) {
        return ModList.get().getModContainerById(mod.getId()).flatMap(m-> m.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class)).map(s -> s.screenFunction().apply(Minecraft.getInstance(), screen)).orElse(null);
    }
}
