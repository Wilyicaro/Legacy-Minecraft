package wily.legacy.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import wily.legacy.util.ModInfo;

public class Legacy4JClientImpl {
    public static void registerRenderType(RenderType renderType, Block... blocks) {
        for (Block block : blocks) {
            ItemBlockRenderTypes.setRenderLayer(block,renderType);
        }
    }

    public static Screen getConfigScreen(ModInfo mod, Screen screen) {
        return ModList.get().getModContainerById(mod.getId()).flatMap(m->  IConfigScreenFactory.getForMod(m.getModInfo())).map(s -> s.createScreen(Minecraft.getInstance(), screen)).orElse(null);
    }
}
