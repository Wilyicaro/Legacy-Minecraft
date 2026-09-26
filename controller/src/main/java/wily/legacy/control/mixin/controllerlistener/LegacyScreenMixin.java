package wily.legacy.control.mixin.controllerlistener;

import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.control.ControllerListener;
import wily.legacy.client.screen.LegacyScreen;

@Mixin(LegacyScreen.class)
public class LegacyScreenMixin implements ControllerListener {
}
