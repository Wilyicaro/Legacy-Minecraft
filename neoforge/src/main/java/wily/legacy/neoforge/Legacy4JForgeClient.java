package wily.legacy.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;


@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Legacy4JForgeClient {
    public static void init(){
        Legacy4JClient.init();
        Legacy4JClient.registerReloadListeners(((ReloadableResourceManager)Minecraft.getInstance().getResourceManager())::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(TickEvent.ClientTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4JClient.preTick(Minecraft.getInstance());
            else Legacy4JClient.postTick(Minecraft.getInstance());
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, e-> Legacy4JClient.clientPlayerJoin(e.getPlayer()));
    }
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event){
        Legacy4JClient.registerItemColors(event::register);
    }
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event){
        Legacy4JClient.registerBlockColors(event::register);
    }
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        Legacy4JClient.registerKeyMappings(event::register);
    }
    @SubscribeEvent
    public static void initClient(FMLClientSetupEvent event){
        event.enqueueWork(Legacy4JClient::setup);
        Legacy4JClient.registerScreen(MenuScreens::register);
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void clientInit(ModelEvent.RegisterAdditional event){
        Legacy4JClient.registerExtraModels(event::register);
    }
    @SubscribeEvent
    public static void overlayModify(RegisterPresetEditorsEvent event){
        Legacy4JClient.VANILLA_PRESET_EDITORS.forEach(((o, presetEditor) -> o.ifPresent(worldPresetResourceKey -> event.register(worldPresetResourceKey, presetEditor))));
    }
    public static Player getClientPlayer(){
        return Minecraft.getInstance().player;
    }
}
