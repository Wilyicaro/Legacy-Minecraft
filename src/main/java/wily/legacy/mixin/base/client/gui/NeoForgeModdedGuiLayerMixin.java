//? if neoforge && <1.21 {
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.NamedGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.client.LegacyGuiElements;

import java.util.Set;

@Mixin(ExtendedGui.class)
public abstract class NeoForgeModdedGuiLayerMixin {
    private static final Set<String> HUD_LAYERS = Set.of("hotbar", "player_health", "armor_level", "food_level", "air_level", "mount_health", "jump_bar", "experience_bar");
    @Unique private boolean moddedHudPre;
    @Unique private boolean moddedHudPost;

    @Inject(method = "pre", at = @At("HEAD"), remap = false)
    private void preparePre(NamedGuiOverlay overlay, GuiGraphics graphics, CallbackInfoReturnable<Boolean> ci) {
        moddedHudPre = isHudLayer(overlay.id()) && LegacyGuiElements.prepareModdedHud(graphics);
    }

    @Inject(method = "pre", at = @At("RETURN"), remap = false)
    private void finalizePre(NamedGuiOverlay overlay, GuiGraphics graphics, CallbackInfoReturnable<Boolean> ci) {
        if (moddedHudPre) {
            LegacyGuiElements.finalizeModdedHud(graphics);
            moddedHudPre = false;
        }
    }

    @Inject(method = "post", at = @At("HEAD"), remap = false)
    private void preparePost(NamedGuiOverlay overlay, GuiGraphics graphics, CallbackInfo ci) {
        moddedHudPost = isHudLayer(overlay.id()) && LegacyGuiElements.prepareModdedHud(graphics);
    }

    @Inject(method = "post", at = @At("RETURN"), remap = false)
    private void finalizePost(NamedGuiOverlay overlay, GuiGraphics graphics, CallbackInfo ci) {
        if (moddedHudPost) {
            LegacyGuiElements.finalizeModdedHud(graphics);
            moddedHudPost = false;
        }
    }

    private static boolean isHudLayer(ResourceLocation id) {
        return id.getNamespace().equals("minecraft") && HUD_LAYERS.contains(id.getPath());
    }
}
*///?}
