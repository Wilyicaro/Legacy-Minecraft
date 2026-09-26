package wily.legacy.control.mixin.controltooltiplistener;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.client.screen.PanelVListScreen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Mixin(ChooseUserScreen.class)
public class ChooseUserScreenMixin extends PanelVListScreen implements ControlTooltip.Listener {
	@Shadow
	@Final
	public static Component DIRECT_LOGIN;

	@Shadow
	@Final
	public static Component ACCOUNT_OPTIONS;

	public ChooseUserScreenMixin() {
		super(null, null);
	}
	@Unique
	private static final MethodHandle superAddControlTooltips;
	@Unique
	private void superAddControlTooltips(ControlTooltipList list) {
		try {
			superAddControlTooltips.invokeExact((ChooseUserScreen)this, list);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	static {
		try {
			//noinspection JavaLangInvokeHandleSignature
			superAddControlTooltips = MethodHandles.lookup().findSpecial(PanelVListScreen.class, "addControlTooltips", MethodType.methodType(void.class, ControlTooltipList.class), ChooseUserScreen.class);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void addControlTooltips(ControlTooltipList list) {
		superAddControlTooltips(list);
		list.add(() -> getFocused() == null || renderableVList.renderables.indexOf(getFocused()) <= 0 ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> ACCOUNT_OPTIONS);
		list.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), () -> DIRECT_LOGIN);
	}
}
