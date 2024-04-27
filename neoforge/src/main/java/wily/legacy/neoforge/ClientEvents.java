package wily.legacy.neoforge;


import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
