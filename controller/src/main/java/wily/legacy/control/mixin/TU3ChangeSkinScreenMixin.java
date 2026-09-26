package wily.legacy.control.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.Controller;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.control.ControllerListener;
import wily.legacy.client.control.ControllerManager;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.screen.ChooseUserScreen;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.skins.client.screen.AbstractChangeSkinScreen;
import wily.legacy.skins.client.screen.TU3ChangeSkinScreen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Mixin(TU3ChangeSkinScreen.class)
public abstract class TU3ChangeSkinScreenMixin extends AbstractChangeSkinScreen implements ControlTooltip.Listener, ControllerListener {
	@Shadow
	protected abstract boolean once(BindingState par1, ControllerBinding<?> par2);

	@Shadow
	protected abstract boolean handleTu3NavVertical(boolean up, boolean down);

	@Shadow
	@Final
	private HoldRepeat tu3HorizontalHold;

	@Shadow
	protected abstract void stopHoldingTu3Horizontal();

	@Shadow
	protected abstract void startHoldingTu3Horizontal(int dir);

	@Shadow
	protected abstract void pumpHoldingTu3Horizontal();

	@Override
	public void bindingStateTick(BindingState state) {
		if (state != null && (state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.RIGHT_BUMPER))) {
			if (state.pressed && state.canClick()) {
				int dir = state.is(ControllerBinding.RIGHT_BUMPER) ? 1 : -1;
				if (packList.getPackCount() > 1) {
					focusRelativePack(dir, true);
					applyQueuedPackChange();
				}
			}
			state.block();
			return;
		}
		if (once(state, ControllerBinding.DPAD_UP) || once(state, ControllerBinding.LEFT_STICK_UP)) {
			if (handleTu3NavVertical(true, false)) return;
		}
		if (once(state, ControllerBinding.DPAD_DOWN) || once(state, ControllerBinding.LEFT_STICK_DOWN)) {
			if (handleTu3NavVertical(false, true)) return;
		}
		if (state != null && (
				state.is(ControllerBinding.DPAD_LEFT)
				|| state.is(ControllerBinding.LEFT_STICK_LEFT)
				|| state.is(ControllerBinding.DPAD_RIGHT)
				|| state.is(ControllerBinding.LEFT_STICK_RIGHT))) {
			int dir = (state.is(ControllerBinding.DPAD_RIGHT) || state.is(ControllerBinding.LEFT_STICK_RIGHT)) ? 1 : -1;
			if (state.released) {
				if (tu3HorizontalHold.active() && tu3HorizontalHold.dir() == dir) stopHoldingTu3Horizontal();
			} else if (state.pressed) {
				if (!tu3HorizontalHold.active() || tu3HorizontalHold.dir() != dir) startHoldingTu3Horizontal(dir);
				pumpHoldingTu3Horizontal();
				state.block();
				return;
			}
		}
		if (handleSharedBindingState(state)) return;
		if (!ControlType.getActiveType().isKbm()
			&& state != null && state.is(ControllerBinding.LEFT_STICK)
			&& state instanceof BindingState.Axis stick) {
			double sx = stick.x, sy = stick.y;
			if (Math.abs(sx) <= 0.45d) {
				if (sy <= -0.65d) {
					if (!leftStickUpHeld) leftStickUpHeld = true;
					state.block();
					return;
				} else if (sy >= 0.65d) {
					if (!leftStickDownHeld) leftStickDownHeld = true;
					state.block();
					return;
				}
			}
			if (Math.abs(sy) < 0.25d) {
				leftStickUpHeld = false;
				leftStickDownHeld = false;
			}
		}
		superBindingStateTick(state);
	}

	@Override
	public void simulateKeyAction(ControllerManager manager, BindingState state) {
		if (manager.isCursorDisabled)
			manager.simulateKeyAction(s -> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
		manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUTTON), InputConstants.KEY_ESCAPE, state, true);
		manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUTTON), InputConstants.KEY_X, state);
		manager.simulateKeyAction(s -> s.is(ControllerBinding.UP_BUTTON), InputConstants.KEY_O, state);
		if (source.supportsCustomPackOptions())
			manager.simulateKeyAction(s -> s.is(ControllerBinding.BACK), InputConstants.KEY_C, state, true);
		if (manager.isCursorDisabled) {
			manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_TRIGGER), InputConstants.KEY_PAGEUP, state);
			manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_PAGEDOWN, state);
		} else {
			manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
		}
		manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE), InputConstants.KEY_F2, state);
	}

	@Unique
	private static final MethodHandle superBindingStateTick;
	@Unique
	private void superBindingStateTick(BindingState state) {
		try {
			superBindingStateTick.invokeExact((TU3ChangeSkinScreen)this, state);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	static {
		try {
			//noinspection JavaLangInvokeHandleSignature
			superBindingStateTick = MethodHandles.lookup().findSpecial(AbstractChangeSkinScreen.class, "bindingStateTick", MethodType.methodType(void.class, BindingState.class), TU3ChangeSkinScreenMixin.class);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
