package wily.legacy.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyBiomeOverride;


@EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, e-> Legacy4JClient.preTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e-> Legacy4JClient.postTick(Minecraft.getInstance()));
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
        event.enqueueWork(Legacy4JClient::setup);}
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event){
        Legacy4JClient.registerScreen(event::register);
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void clientInit(ModelEvent.RegisterAdditional event){
        Legacy4JClient.registerExtraModels(event::register);
    }
    @SubscribeEvent
    public static void overlayModify(RegisterPresetEditorsEvent event){
        Legacy4JClient.VANILLA_PRESET_EDITORS.forEach(((o, presetEditor) -> o.ifPresent(worldPresetResourceKey -> event.register(worldPresetResourceKey, presetEditor))));
    }
}
