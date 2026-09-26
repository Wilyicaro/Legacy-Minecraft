package wily.legacy.control.mixin.actionholder;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.screen.LegacyIconHolder;

@Mixin(LegacyIconHolder.class)
public class LegacyIconHolderMixin implements ControlTooltip.ActionHolder {
	@Override
	public @Nullable Component getAction(Object context) {
		return ControlTooltip.getSelectAction((LegacyIconHolder) (Object) this, context);
	}
}
