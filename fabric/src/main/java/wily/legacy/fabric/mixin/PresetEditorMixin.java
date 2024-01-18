package wily.legacy.fabric.mixin;

import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.screen.LegacyFlatWorldScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Mixin(PresetEditor.class)
public interface PresetEditorMixin {

    @Redirect(method = "<clinit>",at = @At(value = "INVOKE", target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"))
    private static Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> init(Object k1, Object v1, Object k2, Object v2){
        return LegacyMinecraftClient.VANILLA_PRESET_EDITORS;

    }
}
