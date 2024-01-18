package wily.legacy.forge;


import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

import java.util.List;
@Mod(LegacyMinecraft.MOD_ID)
@Mod.EventBusSubscriber(modid = LegacyMinecraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    public static final List<VanillaGuiOverlay> HUD_VANILLA_OVERLAYS = List.of(VanillaGuiOverlay.PLAYER_HEALTH,VanillaGuiOverlay.AIR_LEVEL,VanillaGuiOverlay.ARMOR_LEVEL,VanillaGuiOverlay.MOUNT_HEALTH,VanillaGuiOverlay.FOOD_LEVEL);
    @SubscribeEvent
    public static void overlayModify(RenderGuiOverlayEvent.Pre event){
        Minecraft minecraft = Minecraft.getInstance();
        if (HUD_VANILLA_OVERLAYS.stream().map(VanillaGuiOverlay::type).anyMatch(e-> e.equals(event.getOverlay()))){
            if (minecraft.screen != null){
                event.setCanceled(true);
                return;
            }
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, ScreenUtil.getHUDDistance(),0.0F);
            ScreenUtil.applyHUDScale(event.getGuiGraphics(),i-> minecraft.gui.screenWidth = i,i-> minecraft.gui.screenHeight = i);
        }
    }
    @SubscribeEvent
    public static void overlayModify(RenderGuiOverlayEvent.Post event){
        Minecraft minecraft = Minecraft.getInstance();
        if (HUD_VANILLA_OVERLAYS.stream().map(VanillaGuiOverlay::type).anyMatch(e-> e.equals(event.getOverlay()))){
            if (minecraft.screen != null){
                return;
            }
            ScreenUtil.resetHUDScale(event.getGuiGraphics(),i-> minecraft.gui.screenWidth = i,i-> minecraft.gui.screenHeight = i);
            event.getGuiGraphics().pose().popPose();
            event.getGuiGraphics().setColor(1.0f,1.0f,1.0f, 1.0f);
        }
    }
}
