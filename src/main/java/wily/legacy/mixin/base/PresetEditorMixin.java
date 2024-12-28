package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Legacy4JClient;

import java.util.Map;
import java.util.Optional;


@Mixin(PresetEditor.class)
public interface PresetEditorMixin {
    //? if fabric {
    @Redirect(method = "<clinit>",at = @At(value = "INVOKE", target = "Ljava/util/Map;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"))
    private static Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> init(Object k1, Object v1, Object k2, Object v2){
        return Legacy4JClient.VANILLA_PRESET_EDITORS;
    }
    //?}
}
