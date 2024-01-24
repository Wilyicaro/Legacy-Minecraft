package wily.legacy.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;

@Mod(LegacyMinecraft.MOD_ID)
@Mod.EventBusSubscriber(modid = LegacyMinecraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LegacyMinecraftForge {
    public LegacyMinecraftForge() {
        EventBuses.registerModEventBus(LegacyMinecraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        LegacyMinecraft.init();
        if (FMLEnvironment.dist == Dist.CLIENT) LegacyMinecraftClient.init();
    }
}
