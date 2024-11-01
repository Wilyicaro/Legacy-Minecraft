package wily.legacy.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyBiomeOverride;

import java.util.Optional;


@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Legacy4JForgeClient {

    public static final IClientFluidTypeExtensions CLIENT_WATER_FLUID_TYPE = new IClientFluidTypeExtensions() {
        private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png"), WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still"), WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow"), WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

        @Override
        public ResourceLocation getStillTexture() {
            return WATER_STILL;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return WATER_FLOW;
        }

        @Nullable
        @Override
        public ResourceLocation getOverlayTexture() {
            return WATER_OVERLAY;
        }

        @Override
        public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
            return UNDERWATER_LOCATION;
        }

        @Override
        public int getTintColor() {
            return 0xFF3F76E4;
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return LegacyBiomeOverride.getOrDefault(Minecraft.getInstance().level.getBiome(pos).unwrapKey()).getWaterARGBOrDefault(BiomeColors.getAverageWaterColor(getter,pos));
        }
    };
    public static void init(){
        Legacy4JClient.init();
        Legacy4JClient.registerReloadListeners(((ReloadableResourceManager)Minecraft.getInstance().getResourceManager())::registerReloadListener);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, TickEvent.ClientTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4JClient.preTick(Minecraft.getInstance());
            else Legacy4JClient.postTick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false,ClientPlayerNetworkEvent.LoggingIn.class, e-> Legacy4JClient.clientPlayerJoin(e.getPlayer()));
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
