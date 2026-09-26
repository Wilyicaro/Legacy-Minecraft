package wily.legacy.control.mixin.controltooltiplistener;

import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.screen.LegacyLoadingScreen;

@Mixin(LegacyLoadingScreen.class)
public class LegacyLoadingScreenMixin implements ControlTooltip.Listener {
}
