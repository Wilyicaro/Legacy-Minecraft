package wily.legacy.mixin.base.client.create_world;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.CreateWorldLoadingTracker;

@Mixin(LevelLoadingScreen.class)
public class LoadingMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;onClose()V"))
    private boolean tick(LevelLoadingScreen instance) {
        return !LegacyOptions.legacyLoadingAndConnecting.get() || !CreateWorldLoadingTracker.holdClose();
    }
}
