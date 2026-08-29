//? if neoforge && >=1.21 {
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyGuiElements;

import java.util.List;
import java.util.Set;

@Mixin(GuiLayerManager.class)
public abstract class ModdedGuiLayerMixin {
    private static final Set<String> HUD_LAYERS = Set.of("hotbar", "jump_meter", "experience_bar", "player_health", "armor_level", "food_level", "vehicle_health", "air_level", "experience_level");
    @Shadow @Final private List<GuiLayerManager.NamedLayer> layers;

    @Inject(method = "initModdedLayers", at = @At("RETURN"), remap = false)
    private void wrapModdedHudLayers(CallbackInfo ci) {
        for (int i = 0; i < layers.size(); i++) {
            GuiLayerManager.NamedLayer namedLayer = layers.get(i);
            if (namedLayer.name().getNamespace().equals("minecraft") || !isHudLayer(i)) continue;
            LayeredDraw.Layer layer = namedLayer.layer();
            layers.set(i, new GuiLayerManager.NamedLayer(namedLayer.name(), (graphics, deltaTracker) -> LegacyGuiElements.renderModdedHud(graphics, () -> layer.render(graphics, deltaTracker))));
        }
    }

    private boolean isHudLayer(int index) {
        for (int i = index - 1; i >= 0; i--) {
            ResourceLocation id = layers.get(i).name();
            if (id.getNamespace().equals("minecraft")) return HUD_LAYERS.contains(id.getPath());
        }
        for (int i = index + 1; i < layers.size(); i++) {
            ResourceLocation id = layers.get(i).name();
            if (id.getNamespace().equals("minecraft")) return HUD_LAYERS.contains(id.getPath());
        }
        return false;
    }
}
*///?}
