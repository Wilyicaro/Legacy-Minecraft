//? if forge && >=26.1 {
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.resources.Identifier;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyGuiElements;

import java.util.List;
import java.util.Map;

@Mixin(ForgeLayeredDraw.class)
public abstract class ModdedGuiLayerMixin {
    @Shadow @Final private Map<Identifier, ForgeLayer> namedLayers;
    @Shadow @Final private List<Identifier> order;
    @Shadow @Final private Identifier name;

    @Inject(method = "resolveNested", at = @At("HEAD"), remap = false)
    private void wrapModdedHudLayers(CallbackInfo ci) {
        if (!ForgeLayeredDraw.HOTBAR_AND_DECOS.equals(name)) return;
        for (Identifier id : order) {
            if (id.getNamespace().equals("minecraft")) continue;
            namedLayers.computeIfPresent(id, (key, layer) -> (graphics, deltaTracker) -> LegacyGuiElements.renderModdedHud(graphics, () -> layer.extract(graphics, deltaTracker)));
        }
    }
}
*///?} else if neoforge && >=26.1 {
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyGuiElements;

import java.util.List;

@Mixin(GuiLayerManager.class)
public abstract class ModdedGuiLayerMixin {
    @Shadow @Final private List<GuiLayerManager.NamedLayer> layers;

    @Inject(method = "initModdedLayers", at = @At("RETURN"), remap = false)
    private void wrapModdedHudLayers(CallbackInfo ci) {
        for (int i = 0; i < layers.size(); i++) {
            GuiLayerManager.NamedLayer namedLayer = layers.get(i);
            if (namedLayer.name().getNamespace().equals("minecraft") || !isHudLayer(i)) continue;
            GuiLayer layer = namedLayer.layer();
            layers.set(i, new GuiLayerManager.NamedLayer(namedLayer.name(), (graphics, deltaTracker) -> LegacyGuiElements.renderModdedHud(graphics, () -> layer.render(graphics, deltaTracker))));
        }
    }

    @Unique
    private boolean isHudLayer(int index) {
        for (int i = index - 1; i >= 0; i--) {
            Identifier id = layers.get(i).name();
            if (id.getNamespace().equals("minecraft")) {
                if (isHudAnchor(id)) return true;
                break;
            }
        }
        for (int i = index + 1; i < layers.size(); i++) {
            Identifier id = layers.get(i).name();
            if (id.getNamespace().equals("minecraft")) return isHudAnchor(id);
        }
        return false;
    }

    @Unique
    private static boolean isHudAnchor(Identifier id) {
        return id.equals(VanillaGuiLayers.HOTBAR)
                || id.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || id.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || id.equals(VanillaGuiLayers.FOOD_LEVEL)
                || id.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || id.equals(VanillaGuiLayers.AIR_LEVEL)
                || id.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND)
                || id.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || id.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR);
    }
}
*///?}
